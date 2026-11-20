import { randomUUID } from 'node:crypto';
import { audit,adminRequired } from './model.mjs';
import { ensure,keys,text } from './errors.mjs';

const UUID = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
const LEASE_MS = 60000;

export const identityDeletionHandlers = {
  'admin.identity.cleanup.retry': async (ctx, payload) => {
    adminRequired(ctx);
    keys(payload,['userId']);
    const userId = text(payload.userId,'Hesap',1,200);
    const job = await ctx.db.get("SELECT j.status FROM identity_deletions j JOIN users u ON u.id=j.user_id WHERE j.user_id=? AND u.status='deleted'",[userId]);
    ensure(job?.status==='pending',409,'INVALID_STATE','Bu silme işlemi yeniden denemeye uygun değil.');
    await ctx.db.run('UPDATE identity_deletions SET next_at=? WHERE user_id=?',[ctx.now,userId]);
    await audit(ctx,'admin.identity.cleanup.retry',userId);
    return {queued:true};
  }
};

/** Capture the provider identity before local anonymization removes its mapping. */
export async function queueIdentityDeletion(ctx, user) {
  if (!user.subject.startsWith('provider:')) return;
  let origin = '';
  try { origin = new URL(ctx.config.userInfoUrl).origin; } catch { /* Keep unresolved work visible to operations. */ }
  await ctx.db.run(`INSERT INTO identity_deletions(user_id,provider_user_id,auth_origin,next_at,created_at)
    VALUES(?,?,?,?,?) ON CONFLICT(user_id) DO NOTHING`,
  [user.id,user.subject.slice('provider:'.length),origin,ctx.now,ctx.now]);
}

function endpoint(config, job) {
  if (!config.authAdminKey || !config.supabaseUrl || !config.userInfoUrl) throw new Error('AUTH_ADMIN_NOT_CONFIGURED');
  const project = new URL(config.supabaseUrl), userInfo = new URL(config.userInfoUrl);
  if (project.protocol !== 'https:' || project.username || project.password || project.pathname.replace(/\/$/,'') !== '' ||
      project.search || project.hash || project.origin !== userInfo.origin || project.origin !== job.auth_origin ||
      userInfo.pathname.replace(/\/$/,'') !== '/auth/v1/user') throw new Error('AUTH_PROJECT_MISMATCH');
  if (!UUID.test(job.provider_user_id ?? '')) throw new Error('AUTH_ID_UNSUPPORTED');
  return `${project.origin}/auth/v1/admin/users/${job.provider_user_id}`;
}

async function claim(service) {
  return service.store.transaction(async db => {
    const now = service.clock();
    await db.run("UPDATE identity_deletions SET status='pending',claim_token=NULL WHERE status='processing' AND next_at<=?",[now]);
    const job = await db.get(`SELECT j.* FROM identity_deletions j JOIN users u ON u.id=j.user_id
      WHERE j.status='pending' AND j.next_at<=? AND u.status='deleted' ORDER BY j.created_at,j.user_id LIMIT 1`,[now]);
    if (!job) return null;
    const token = randomUUID();
    await db.run("UPDATE identity_deletions SET status='processing',claim_token=?,next_at=? WHERE user_id=?",[token,now+LEASE_MS,job.user_id]);
    return {...job,claimToken:token};
  });
}

/** Runs outside domain transactions. A lost response is safely retried; missing users count as deleted. */
export async function processIdentityDeletions(service, request = fetch) {
  for (let index = 0; index < 10; index++) {
    const job = await claim(service);
    if (!job) break;
    let failure = null;
    try {
      const url = endpoint(service.config,job);
      const response = await request(url,{
        method:'DELETE', redirect:'error', signal:AbortSignal.timeout(8000),
        headers:{apikey:service.config.authAdminKey,Authorization:`Bearer ${service.config.authAdminKey}`,'Content-Type':'application/json'},
        body:JSON.stringify({should_soft_delete:false})
      });
      if (!response.ok) {
        const body = response.status === 404 ? await response.json().catch(() => ({})) : {};
        if (response.status !== 404 || ![body.code,body.error_code].includes('user_not_found')) {
          throw new Error(`AUTH_DELETE_REJECTED_${response.status}`);
        }
      }
    } catch (error) {
      failure = /^AUTH_[A-Z0-9_]+$/.test(error.message) ? error.message : 'AUTH_DELETE_UNAVAILABLE';
    }
    await service.store.transaction(async db => {
      const now = service.clock();
      if (failure) {
        await db.run(`UPDATE identity_deletions SET status='pending',claim_token=NULL,attempts=attempts+1,
          last_error=?,next_at=? WHERE user_id=? AND status='processing' AND claim_token=?`,
        [failure,now+Math.min(3600000,60000*2**Math.min(job.attempts,6)),job.user_id,job.claimToken]);
      } else {
        const result = await db.run(`UPDATE identity_deletions SET status='completed',provider_user_id=NULL,auth_origin='',
          claim_token=NULL,last_error='',completed_at=? WHERE user_id=? AND status='processing' AND claim_token=?`,
        [now,job.user_id,job.claimToken]);
        if (result.changes) await audit({db,actor:null,now},'account.identity.deletion.complete',job.user_id,{provider:'supabase'});
      }
    });
  }
}
