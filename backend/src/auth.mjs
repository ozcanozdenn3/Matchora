import { smsConfigured,sendOtpSms } from './sms.mjs';
import { randomBytes, randomInt, randomUUID, createHash, createHmac, createVerify, timingSafeEqual } from 'node:crypto';
import { ensure, fail, text, keys, choice, ApiError } from './errors.mjs';
const FIREBASE_CERTS_URL = 'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com';
export const digest = value => createHash('sha256').update(value).digest('hex');
export const secureEqual = (a, b) => typeof a === 'string' && typeof b === 'string' && Buffer.byteLength(a) === Buffer.byteLength(b) && timingSafeEqual(Buffer.from(a), Buffer.from(b));
export const sign = (secret, value) => createHmac('sha256', secret).update(value).digest('hex');
export const id = () => randomUUID();
export function normalizePhone(value) {
  const raw = text(value, 'Telefon', 7, 24).trim();
  if (raw.startsWith('+')) {
    const digits = raw.slice(1).replace(/\D/g, '');
    ensure(digits.length >= 7 && digits.length <= 15, 400, 'INVALID_PHONE', 'Geçerli uluslararası telefon numarası gir.');
    return `+${digits}`;
  }
  let digits = raw.replace(/\D/g, '');
  if (digits.startsWith('90') && digits.length === 12) digits = digits.slice(2);
  if (digits.startsWith('0') && digits.length === 11) digits = digits.slice(1);
  if (/^5\d{9}$/.test(digits)) return `+90${digits}`;
  ensure(digits.length >= 7 && digits.length <= 15, 400, 'INVALID_PHONE', 'Geçerli cep telefonu gir.');
  return `+${digits}`;
}
export async function rateLimit(db, bucket, now, max = 60, window = 60000) {
  const result = await db.run(`INSERT INTO rate_limits(bucket,count,resets_at) VALUES(?,1,?)
    ON CONFLICT(bucket) DO UPDATE SET
      count=CASE WHEN rate_limits.resets_at<=? THEN 1 ELSE rate_limits.count+1 END,
      resets_at=CASE WHEN rate_limits.resets_at<=? THEN excluded.resets_at ELSE rate_limits.resets_at END
    WHERE rate_limits.resets_at<=? OR rate_limits.count<?`, [bucket,now+window,now,now,now,max]);
  ensure(result.changes === 1, 429, 'RATE_LIMITED', 'Çok fazla deneme. Biraz sonra tekrar dene.');
}
export class Auth {
  constructor(store, config, clock = Date.now, request = fetch) { this.store = store; this.config = config; this.clock = clock; this.request = request; }
  async actor(token) {
    ensure(typeof token === 'string' && /^[a-f0-9]{64}$/.test(token), 401, 'UNAUTHENTICATED', 'Giriş yapmalısın.');
    const row = await this.store.get('SELECT s.*,u.role,u.status FROM sessions s JOIN users u ON u.id=s.user_id WHERE token_hash=?', [digest(token)]);
    ensure(row && !row.revoked && row.status === 'active', 401, 'UNAUTHENTICATED', 'Oturum geçersiz.');
    ensure(Number(row.expires_at) > this.clock(), 401, 'SESSION_EXPIRED', 'Oturum yenilenmeli.');
    return { id: row.user_id, role: row.role, sessionId: row.id };
  }
  async issue(db, userId, family = id()) {
    const token = randomBytes(32).toString('hex'), refresh = randomBytes(32).toString('hex'), now = this.clock();
    await db.run('INSERT INTO sessions(id,user_id,token_hash,refresh_hash,family_id,expires_at,refresh_expires_at,created_at) VALUES(?,?,?,?,?,?,?,?)', [id(), userId, digest(token), digest(refresh), family, now + 15 * 60000, now + 30 * 86400000, now]);
    return { accessToken: token, refreshToken: refresh, expiresAt: now + 15 * 60000, userId };
  }
  async upsertIdentity(db, subject, phone = null, email = null) {
    if(subject.startsWith('provider:'))ensure(!await db.get("SELECT user_id FROM identity_deletions WHERE provider_user_id=? AND status<>'completed'",[subject.slice('provider:'.length)]),403,'ACCOUNT_DELETION_PENDING','Önceki hesabının silme işlemi tamamlanıyor. Daha sonra tekrar dene.');
    let user = await db.get('SELECT * FROM users WHERE subject=?', [subject]);
    if (!user) {
      if(phone)ensure(!await db.get('SELECT id FROM users WHERE phone=?',[phone]),409,'PHONE_ALREADY_LINKED','Bu telefon başka bir Matchora hesabında. Telefonla giriş yapabilir veya destekle iletişime geçebilirsin.');
      const userId = id();
      // Never merge accounts based solely on a client-supplied email/phone.
      await db.run('INSERT INTO users(id,subject,phone,email,role,created_at) VALUES(?,?,?,?,?,?)', [userId, subject, phone, email, this.config.admins.has(subject) ? 'admin' : 'user', this.clock()]);
      await db.run('INSERT INTO preferences(user_id) VALUES(?)', [userId]); user = { id: userId, status: 'active' };
    }
    ensure(user.status === 'active', 403, 'ACCOUNT_DISABLED', 'Bu hesap kullanılamıyor.'); return user.id;
  }
  async devLogin(payload) {
    ensure(this.config.devAuth && !this.config.production, 404, 'NOT_FOUND', 'Bulunamadı.');
    keys(payload, ['identity']); const subject = `dev:${text(payload.identity, 'Test kimliği', 3, 100)}`;
    return this.store.transaction(async db => this.issue(db, await this.upsertIdentity(db, subject)));
  }
  async exchange(payload, expectedIdentity = null, overrideName = null) {
    keys(payload, ['providerToken']); text(payload.providerToken, 'Provider token', 20, 10000);
    ensure(this.config.userInfoUrl, 503, 'AUTH_NOT_CONFIGURED', 'Giriş servisi henüz bağlanmadı.');
    const response = await this.request(this.config.userInfoUrl, { headers: { Authorization: `Bearer ${payload.providerToken}`, ...(this.config.authPublicKey ? { apikey: this.config.authPublicKey } : {}) }, signal: AbortSignal.timeout(8000), redirect: 'error' });
    ensure(response.ok, 401, 'PROVIDER_REJECTED', 'Kimlik doğrulanamadı.');
    const identity = await response.json();
    ensure(typeof identity.id === 'string' && identity.id.length <= 200, 401, 'PROVIDER_REJECTED', 'Kimlik doğrulanamadı.');
    if(expectedIdentity?.email)ensure(identity.email_confirmed_at&&typeof identity.email==='string'&&identity.email.toLowerCase()===expectedIdentity.email,401,'PROVIDER_REJECTED','Doğrulanan e-posta bu giriş isteğiyle eşleşmiyor.');
    const subject = `provider:${identity.id}`;
    const verifiedPhone=identity.phone_confirmed_at&&typeof identity.phone==='string'&&/^(\+?90)?5\d{9}$/.test(identity.phone)?normalizePhone(identity.phone):null;
    const session=await this.store.transaction(async db => this.issue(db, await this.upsertIdentity(db, subject, verifiedPhone, identity.email_confirmed_at ? identity.email : null)));
    const name = overrideName || (identity.user_metadata?.full_name ?? identity.user_metadata?.name);
    return{...session,profileHints:{name:typeof name==='string'?name.trim().slice(0,40):'',email:identity.email_confirmed_at?identity.email??'':'',phone:verifiedPhone??''}};
  }
  async verifyFirebaseIdToken(idToken, expectedProject) {
    const parts = idToken.split('.');
    ensure(parts.length === 3, 401, 'INVALID_TOKEN', 'Geçersiz doğrulama belirteci.');
    const [headerB64, payloadB64, sigB64] = parts;
    let header, claims;
    try {
      header = JSON.parse(Buffer.from(headerB64, 'base64url').toString('utf8'));
      claims = JSON.parse(Buffer.from(payloadB64, 'base64url').toString('utf8'));
    } catch {
      fail(401, 'INVALID_TOKEN', 'Geçersiz doğrulama belirteci.');
    }
    ensure(header?.alg === 'RS256' && typeof header.kid === 'string' && header.kid.length > 0, 401, 'INVALID_TOKEN', 'Geçersiz doğrulama belirteci.');
    const nowSec = Math.floor(this.clock() / 1000);
    ensure(
      claims &&
      claims.aud === expectedProject &&
      claims.iss === `https://securetoken.google.com/${expectedProject}` &&
      typeof claims.sub === 'string' && claims.sub.length > 0 && claims.sub.length <= 128 &&
      Number.isFinite(claims.exp) && claims.exp > nowSec &&
      Number.isFinite(claims.iat) && claims.iat <= nowSec + 300 &&
      (claims.auth_time === undefined || (Number.isFinite(claims.auth_time) && claims.auth_time <= nowSec + 300)),
      401, 'INVALID_TOKEN', 'Geçersiz veya süresi dolmuş doğrulama belirteci.'
    );
    let certPem = this.firebaseCerts?.certs?.[header.kid];
    if (!certPem || !this.firebaseCerts || this.firebaseCerts.expiresAt <= this.clock()) {
      let response;
      try {
        response = await this.request(FIREBASE_CERTS_URL, { signal: AbortSignal.timeout(10000) });
      } catch {
        ensure(false, 503, 'AUTH_UNAVAILABLE', 'Giriş servisine ulaşılamadı. Yeniden dene.');
      }
      ensure(response.ok, 401, 'PROVIDER_REJECTED', 'Telefon doğrulaması başarısız oldu.');
      const certs = await response.json();
      const cacheHeader = response.headers?.get?.('cache-control') || '';
      const maxAgeMatch = /max-age=(\d+)/i.exec(cacheHeader);
      const ttlMs = maxAgeMatch ? Number(maxAgeMatch[1]) * 1000 : 3600 * 1000;
      this.firebaseCerts = { certs, expiresAt: this.clock() + ttlMs };
      certPem = certs?.[header.kid];
    }
    ensure(typeof certPem === 'string' && certPem.includes('BEGIN '), 401, 'INVALID_TOKEN', 'Doğrulama anahtarı bulunamadı.');
    let validSig = false;
    try {
      const verifier = createVerify('RSA-SHA256');
      verifier.update(`${headerB64}.${payloadB64}`);
      verifier.end();
      validSig = verifier.verify(certPem, Buffer.from(sigB64, 'base64url'));
    } catch {
      validSig = false;
    }
    ensure(validSig, 401, 'INVALID_TOKEN', 'Geçersiz doğrulama imzası.');
    return claims;
  }
  async exchangeIdToken(payload) {
    keys(payload, ['provider', 'idToken', 'nonce', 'fullName']);
    const provider = choice(payload.provider, ['apple', 'google', 'firebase'], 'Giriş sağlayıcısı');
    text(payload.idToken, 'Kimlik belirteci', 20, 10000);
    if (provider === 'firebase') {
      const expectedProject = this.config.fcmProjectId || 'matchora-c5ddc';
      const data = await this.verifyFirebaseIdToken(payload.idToken, expectedProject);
      const phone = data.phone_number ? normalizePhone(data.phone_number) : null;
      const identityKey = phone ? `phone:${phone}` : `firebase:${data.sub}`;
      const fullName = typeof payload.fullName === 'string' && payload.fullName.trim().length ? payload.fullName.trim() : null;
      const session = await this.store.transaction(async db => this.issue(db, await this.upsertIdentity(db, identityKey, phone, null)));
      return { ...session, profileHints: { name: fullName || '', email: '', phone: phone || '' } };
    }
    ensure(this.config.userInfoUrl, 503, 'AUTH_NOT_CONFIGURED', 'Giriş servisi henüz bağlanmadı.');
    const tokenUrl = this.config.userInfoUrl.replace(/user$/, 'token?grant_type=id_token');
    let response;
    try {
      response = await this.request(tokenUrl, {
        method: 'POST',
        headers: {
          apikey: this.config.authPublicKey,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          provider,
          id_token: payload.idToken,
          ...(payload.nonce ? { nonce: payload.nonce } : {})
        }),
        signal: AbortSignal.timeout(10000),
        redirect: 'error'
      });
    } catch {
      ensure(false, 503, 'AUTH_UNAVAILABLE', 'Giriş servisine ulaşılamadı. Yeniden dene.');
    }
    if (!response.ok) {
      const errBody = await response.text().catch(() => '');
      console.error('[ID_TOKEN_ERROR]', response.status, errBody);
    }
    ensure(response.ok, 401, 'PROVIDER_REJECTED', 'Giriş doğrulanamadı. Yeniden dene.');
    const data = await response.json();
    const fullName = typeof payload.fullName === 'string' && payload.fullName.trim().length ? payload.fullName.trim() : null;
    return this.exchange({ providerToken: data.access_token }, null, fullName);
  }
  async startOtp(payload, bucket, actor = null) {
    keys(payload, ['phone']); const phone = normalizePhone(payload.phone), challengeId = id(), now = this.clock();
    const code = randomInt(0, 1000000).toString().padStart(6, '0');
    ensure(smsConfigured(this.config) || this.config.devAuth, 503, 'SMS_NOT_CONFIGURED', 'SMS servisi henüz bağlanmadı.');
    await this.store.transaction(async db => {
      await rateLimit(db, `otp-phone:${phone}`, now, 3, 15 * 60000);
      await rateLimit(db, `otp-ip:${bucket}`, now, 20, 15 * 60000);
      await db.run('UPDATE auth_challenges SET consumed=1 WHERE identity=? AND consumed=0', [phone]);
      await db.run('INSERT INTO auth_challenges(id,identity,user_id,code_hash,expires_at,created_at) VALUES(?,?,?,?,?,?)', [challengeId, phone, actor?.id ?? null, sign(this.config.secret, `${challengeId}:${code}`), now + 5 * 60000, now]);
    });
    if (smsConfigured(this.config)) {
      try { await sendOtpSms(this.config,phone,code,challengeId,this.request); }
      catch(error) { await this.store.run('UPDATE auth_challenges SET consumed=1 WHERE id=?',[challengeId]);throw error; }
    }
    return { challengeId, expiresAt: now + 5 * 60000, ...(this.config.devAuth && !smsConfigured(this.config) ? { developmentCode: code } : {}) };
  }
  async verifyOtp(payload) {
    keys(payload, ['challengeId', 'code']); text(payload.challengeId, 'Challenge', 10, 100); ensure(/^\d{6}$/.test(payload.code));
    const result = await this.store.transaction(async db => {
      const row = await db.get('SELECT * FROM auth_challenges WHERE id=?', [payload.challengeId]);
      if (!row || row.user_id || row.consumed || Number(row.expires_at) <= this.clock() || row.attempts >= 5) return { error: 'OTP_EXPIRED' };
      // Failed attempts must commit; throwing inside this transaction would roll them back.
      await db.run('UPDATE auth_challenges SET attempts=attempts+1 WHERE id=?', [row.id]);
      if (!secureEqual(row.code_hash, sign(this.config.secret, `${row.id}:${payload.code}`))) return { error: 'OTP_INVALID' };
      await db.run('UPDATE auth_challenges SET consumed=1 WHERE id=?', [row.id]);
      return this.issue(db, await this.upsertIdentity(db, `phone:${row.identity}`, row.identity));
    });
    if (result.error) fail(401, result.error, 'Kod geçersiz veya süresi dolmuş.'); return result;
  }
  async verifyContact(actor,payload) {
    ensure(actor?.id,401,'UNAUTHENTICATED','Giriş yapmalısın.');keys(payload,['challengeId','code']);ensure(/^\d{6}$/.test(payload.code));
    const result=await this.store.transaction(async db=>{
      const row=await db.get('SELECT * FROM auth_challenges WHERE id=?',[payload.challengeId]);
      if(!row||row.user_id!==actor.id||row.consumed||Number(row.expires_at)<=this.clock()||row.attempts>=5)return{error:'OTP_EXPIRED'};
      await db.run('UPDATE auth_challenges SET attempts=attempts+1 WHERE id=?',[row.id]);
      if(!secureEqual(row.code_hash,sign(this.config.secret,`${row.id}:${payload.code}`)))return{error:'OTP_INVALID'};
      const existing=await db.get('SELECT id FROM users WHERE phone=? AND id<>?',[row.identity,actor.id]);
      if(existing)return{error:'PHONE_ALREADY_LINKED'};
      await db.run('UPDATE users SET phone=? WHERE id=?',[row.identity,actor.id]);
      const profile=await db.get('SELECT payload FROM profiles WHERE user_id=?',[actor.id]);
      if(profile){const p=JSON.parse(profile.payload);p.phone=row.identity;p.verifications=[...new Set([...(p.verifications??[]),'BASIC'])];await db.run('UPDATE profiles SET payload=?,version=version+1,updated_at=? WHERE user_id=?',[JSON.stringify(p),this.clock(),actor.id]);}
      await db.run('UPDATE auth_challenges SET consumed=1 WHERE id=?',[row.id]);return{verified:true};
    });
    if(result.error)fail(409,result.error,'Doğrulama tamamlanamadı. Yeni kod iste veya destekle iletişime geç.');return result;
  }
  async refresh(payload) {
    keys(payload, ['refreshToken']); text(payload.refreshToken, 'Yenileme tokenı', 64, 64);
    const result = await this.store.transaction(async db => {
      const row = await db.get('SELECT s.*,u.status FROM sessions s JOIN users u ON u.id=s.user_id WHERE refresh_hash=?', [digest(payload.refreshToken)]);
      if (!row || Number(row.refresh_expires_at) <= this.clock() || row.status !== 'active') return { error: true };
      if (row.revoked) { await db.run('UPDATE sessions SET revoked=1 WHERE family_id=?', [row.family_id]); return { error: true }; }
      await db.run('UPDATE sessions SET revoked=1 WHERE id=?', [row.id]); return this.issue(db, row.user_id, row.family_id);
    });
    ensure(!result.error, 401, 'UNAUTHENTICATED', 'Yeniden giriş yapmalısın.'); return result;
  }
  async logout(actor, all = false) { await this.store.transaction(db => db.run(all ? 'UPDATE sessions SET revoked=1 WHERE user_id=?' : 'UPDATE sessions SET revoked=1 WHERE id=?', [all ? actor.id : actor.sessionId])); return { signedOut: true }; }
}
