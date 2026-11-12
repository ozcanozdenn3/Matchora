export class ApiError extends Error {
  constructor(status, code, message) { super(message); this.status = status; this.code = code; }
}
export const fail = (status, code, message) => { throw new ApiError(status, code, message); };
export const ensure = (condition, status = 400, code = 'INVALID_INPUT', message = 'Bilgileri kontrol et.') => { if (!condition) fail(status, code, message); };
export function text(value, name, min = 1, max = 500) {
  ensure(typeof value === 'string' && value.trim().length >= min && value.trim().length <= max, 400, 'INVALID_INPUT', `${name} alanını kontrol et.`);
  return value.trim();
}
export function integer(value, name, min = 0, max = 1000000) {
  ensure(Number.isSafeInteger(value) && value >= min && value <= max, 400, 'INVALID_INPUT', `${name} aralık dışında.`); return value;
}
export function choice(value, values, name) { ensure(values.includes(value), 400, 'INVALID_INPUT', `${name} geçersiz.`); return value; }
export function object(value) { ensure(value && typeof value === 'object' && !Array.isArray(value)); return value; }
export function keys(value, allowed) { object(value); ensure(Object.keys(value).every(key => allowed.includes(key)), 400, 'UNKNOWN_FIELD', 'Desteklenmeyen alan gönderildi.'); }
export const json = value => JSON.parse(value);
