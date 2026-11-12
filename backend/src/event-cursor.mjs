import { createHash, randomBytes, createCipheriv, createDecipheriv } from 'node:crypto';
import { ensure } from './errors.mjs';

export function eventCursor(ctx, event) {
  const iv = randomBytes(12), key = createHash('sha256').update(ctx.config.secret).digest();
  const cipher = createCipheriv('aes-256-gcm', key, iv);
  const body = Buffer.concat([cipher.update(JSON.stringify([Number(event.starts_at), event.id, ctx.now + 3600000])), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), body]).toString('base64url');
}
export function readEventCursor(ctx, token) {
  try {
    ensure(typeof token === 'string' && token.length <= 1024);
    const raw = Buffer.from(token, 'base64url'), key = createHash('sha256').update(ctx.config.secret).digest();
    ensure(raw.length > 28);
    const decipher = createDecipheriv('aes-256-gcm', key, raw.subarray(0, 12));
    decipher.setAuthTag(raw.subarray(12, 28));
    const value = JSON.parse(Buffer.concat([decipher.update(raw.subarray(28)), decipher.final()]).toString());
    ensure(Array.isArray(value) && value.length === 3 && Number.isSafeInteger(value[0]) && typeof value[1] === 'string' && value[2] > ctx.now);
    return value;
  } catch { ensure(false, 400, 'INVALID_CURSOR', 'Liste sayfasının süresi doldu veya bilgisi geçersiz. Listeyi yenile.'); }
}
