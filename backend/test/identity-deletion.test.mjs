import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { processDeletions } from '../src/jobs.mjs';
import { processIdentityDeletions } from '../src/identity-deletion.mjs';

const providerId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
async function scheduledDeletion(t) {
  const f = await fixture(t);
  Object.assign(f.config,{supabaseUrl:'https://auth.example.invalid',userInfoUrl:'https://auth.example.invalid/auth/v1/user',authAdminKey:'server-only-test-key'});
  await f.store.run('UPDATE users SET subject=? WHERE id=?',[`provider:${providerId}`,f.host.id]);
  await f.cmd(f.host,'account.delete',{});
  f.advance(f.config.deletionDelay+1);
  await f.store.transaction(db => processDeletions(f.service.ctx(db)));
  assert.equal((await f.store.get('SELECT status FROM users WHERE id=?',[f.host.id])).status,'deleted');
  return f;
}

test('deletion preserves provider mapping durably, deletes only that user, and removes the mapping after acknowledgement', async t => {
  const f = await scheduledDeletion(t);
  const pending = await f.store.get('SELECT * FROM identity_deletions WHERE user_id=?',[f.host.id]);
  assert.equal(pending.provider_user_id,providerId);
  assert.equal(pending.auth_origin,'https://auth.example.invalid');
  let calls=0;
  await processIdentityDeletions(f.service,async (url,options) => {
    calls++;
    assert.equal(url,`https://auth.example.invalid/auth/v1/admin/users/${providerId}`);
    assert.equal(options.method,'DELETE');
    assert.equal(options.headers.Authorization,'Bearer server-only-test-key');
    assert.deepEqual(JSON.parse(options.body),{should_soft_delete:false});
    return new Response('{}',{status:200});
  });
  await processIdentityDeletions(f.service,async () => {throw new Error('completed deletion must not repeat');});
  const done = await f.store.get('SELECT * FROM identity_deletions WHERE user_id=?',[f.host.id]);
  assert.equal(calls,1);
  assert.equal(done.status,'completed');
  assert.equal(done.provider_user_id,null);
  assert.equal(done.auth_origin,'');
  assert.equal((await f.store.get('SELECT status FROM users WHERE id=?',[f.guest.id])).status,'active');
});

test('lost replies retry with backoff and only a real user-not-found response confirms prior deletion', async t => {
  const f = await scheduledDeletion(t);
  await processIdentityDeletions(f.service,async () => {throw new Error('Do not persist credentials: secret-value');});
  let job=await f.store.get('SELECT * FROM identity_deletions WHERE user_id=?',[f.host.id]);
  assert.equal(job.status,'pending');
  assert.equal(job.last_error,'AUTH_DELETE_UNAVAILABLE');
  assert.equal(job.attempts,1);
  await processIdentityDeletions(f.service,async () => {throw new Error('retry is not due');});
  assert.equal((await f.store.get('SELECT attempts FROM identity_deletions WHERE user_id=?',[f.host.id])).attempts,1);
  await assert.rejects(f.store.transaction(db=>f.auth.upsertIdentity(db,`provider:${providerId}`)),error=>error.code==='ACCOUNT_DELETION_PENDING');
  f.advance(60001);
  await processIdentityDeletions(f.service,async () => new Response(JSON.stringify({message:'Unknown route'}),{status:404}));
  job=await f.store.get('SELECT * FROM identity_deletions WHERE user_id=?',[f.host.id]);
  assert.equal(job.status,'pending');
  assert.equal(job.last_error,'AUTH_DELETE_REJECTED_404');
  f.advance(120001);
  await processIdentityDeletions(f.service,async () => new Response(JSON.stringify({code:404,error_code:'user_not_found',msg:'User not found'}),{status:404}));
  assert.equal((await f.store.get('SELECT status FROM identity_deletions WHERE user_id=?',[f.host.id])).status,'completed');
});

test('wrong-project or absent admin configuration never sends a deletion; operations can retry only scheduled work', async t => {
  const f = await scheduledDeletion(t);
  let calls=0;
  const request=async () => {calls++;return new Response('{}');};
  f.config.supabaseUrl='https://another.example.invalid';
  await processIdentityDeletions(f.service,request);
  assert.equal(calls,0);
  assert.equal((await f.store.get('SELECT last_error FROM identity_deletions WHERE user_id=?',[f.host.id])).last_error,'AUTH_PROJECT_MISMATCH');
  await assert.rejects(f.cmd(f.guest,'admin.identity.cleanup.retry',{userId:f.host.id}),error=>error.code==='FORBIDDEN');
  await assert.rejects(f.cmd(f.admin,'admin.identity.cleanup.retry',{userId:f.guest.id}),error=>error.code==='INVALID_STATE');
  Object.assign(f.config,{supabaseUrl:'https://auth.example.invalid',authAdminKey:''});
  await f.cmd(f.admin,'admin.identity.cleanup.retry',{userId:f.host.id});
  await processIdentityDeletions(f.service,request);
  assert.equal(calls,0);
  const operations=await f.service.adminInbox(f.admin);
  assert.equal(operations.identityDeletions[0].last_error,'AUTH_ADMIN_NOT_CONFIGURED');
  assert.equal(operations.identityDeletions[0].provider_user_id,undefined);
});

test('two workers do not delete concurrently, and a rolled back local deletion never leaves external work', async t => {
  const f = await scheduledDeletion(t);
  let release;
  const gate=new Promise(resolve=>{release=resolve;});
  let started;
  const entered=new Promise(resolve=>{started=resolve;});
  let calls=0;
  const request=async()=>{calls++;started();await gate;return new Response('{}');};
  const first=processIdentityDeletions(f.service,request);
  await entered;
  await processIdentityDeletions(f.service,request);
  assert.equal(calls,1);
  release();await first;
  await f.store.run('UPDATE users SET subject=? WHERE id=?',['provider:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',f.guest.id]);
  await f.cmd(f.guest,'account.delete',{});f.advance(f.config.deletionDelay+1);
  await assert.rejects(f.store.transaction(async db=>{await processDeletions(f.service.ctx(db));throw new Error('rollback');}));
  assert.equal(await f.store.get('SELECT user_id FROM identity_deletions WHERE user_id=?',[f.guest.id]),undefined);
  assert.equal((await f.store.get('SELECT status FROM users WHERE id=?',[f.guest.id])).status,'active');
});
