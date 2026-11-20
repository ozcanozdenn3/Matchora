import { createHash } from 'node:crypto';
import { ensure, keys, text, choice } from './errors.mjs';
import { PostgresStore } from './store.mjs';
import { publicPlayer, preloadPublicPlayers } from './queries.mjs';
import { sealCursor, openCursor } from './opaque-cursor.mjs';
import { canonicalTurkish } from './turkish-locale.mjs';
import { SPORTS } from './sports.mjs';

// Apply the same Turkish folding as canonicalTurkish before SQL's lowercase.
// The expressions and replacement literals are application constants.
function foldedSql(expression) {
  const replacements = [
    ['İ','i'],['I','i'],['ı','i'],['Ş','s'],['ş','s'],['Ğ','g'],['ğ','g'],
    ['Ü','u'],['ü','u'],['Ö','o'],['ö','o'],['Ç','c'],['ç','c'],
    ['Â','a'],['â','a'],['Î','i'],['î','i'],['Û','u'],['û','u'],['\u0307','']
  ];
  let sql = `trim(COALESCE(${expression},''))`;
  for (const [from,to] of replacements) sql = `replace(${sql},'${from}','${to}')`;
  return `lower(${sql})`;
}

const escapeLike = value => value.replace(/[\\%_]/g, character => `\\${character}`);

/** Search the complete active athlete directory, with current blocks and public projection. */
export async function searchPlayers(ctx, filters = {}) {
  ensure(ctx.actor?.id,401,'UNAUTHENTICATED','Sporcu aramak için giriş yap.');
  keys(filters,['q','sport','city','area','district','after','limit']);
  const query = canonicalTurkish(text(filters.q ?? '', 'Arama',0,80));
  const city = canonicalTurkish(text(filters.city ?? '', 'İl',0,60));
  const district = canonicalTurkish(text(filters.district ?? filters.area ?? '', 'İlçe',0,60));
  const sport = filters.sport ? choice(filters.sport,SPORTS,'Branş') : '';
  const size = filters.limit == null ? 24 : Number(filters.limit);
  ensure(Number.isInteger(size) && size >= 1 && size <= 100,400,'INVALID_INPUT','Sayfa boyutu 1 ile 100 arasında olmalı.');
  const scope = createHash('sha256').update(JSON.stringify([query,city,district,sport])).digest('hex');
  const cursor = filters.after ? openCursor(ctx,'player-search',filters.after) : null;
  const watermark = cursor?.watermark ?? ctx.now;
  if (cursor) {
    ensure(cursor.scope === scope && Number.isSafeInteger(watermark) && watermark >= 0 &&
      Array.isArray(cursor.last) && cursor.last.length === 2 && Number.isSafeInteger(cursor.last[0]) &&
      cursor.last[0] <= watermark && typeof cursor.last[1] === 'string' && cursor.last[1].length <= 200,
      400,'INVALID_CURSOR','Sporcu arama filtreleri değişti. Aramayı yenile.');
  }
  const postgres = ctx.db instanceof PostgresStore;
  const field = name => postgres ? `(p.payload::jsonb->>'${name}')` : `json_extract(p.payload,'$.${name}')`;
  const conditions = ["u.status='active'",'p.updated_at<=?','p.user_id<>?',
    'NOT EXISTS(SELECT 1 FROM blocks b WHERE (b.user_id=? AND b.target_id=p.user_id) OR (b.target_id=? AND b.user_id=p.user_id))'];
  const parameters = [watermark,ctx.actor.id,ctx.actor.id,ctx.actor.id];
  if (query) {
    conditions.push(`${foldedSql(field('name'))} LIKE ? ESCAPE '\\'`);
    parameters.push(`%${escapeLike(query)}%`);
  }
  if (city) { conditions.push(`${foldedSql(field('city'))}=?`); parameters.push(city); }
  if (district) { conditions.push(`${foldedSql(field('area'))}=?`); parameters.push(district); }
  if (sport) {
    conditions.push(postgres
      ? `EXISTS(SELECT 1 FROM jsonb_array_elements(CASE WHEN jsonb_typeof(p.payload::jsonb->'skills')='array' THEN p.payload::jsonb->'skills' ELSE '[]'::jsonb END) skill WHERE skill->>'sport'=?)`
      : `EXISTS(SELECT 1 FROM json_each(CASE WHEN json_type(p.payload,'$.skills')='array' THEN json_extract(p.payload,'$.skills') ELSE '[]' END) skill WHERE json_extract(CASE WHEN skill.type='object' THEN skill.value ELSE '{}' END,'$.sport')=?)`);
    parameters.push(sport);
  }
  if (cursor) {
    conditions.push('(p.updated_at<? OR (p.updated_at=? AND p.user_id<?))');
    parameters.push(cursor.last[0],cursor.last[0],cursor.last[1]);
  }
  parameters.push(size+1);
  const rows = await ctx.db.all(`SELECT p.user_id,p.updated_at FROM profiles p JOIN users u ON u.id=p.user_id
    WHERE ${conditions.join(' AND ')} ORDER BY p.updated_at DESC,p.user_id DESC LIMIT ?`,parameters);
  const selected = rows.slice(0,size);
  await preloadPublicPlayers(ctx,selected.map(row => row.user_id));
  const items = await Promise.all(selected.map(row => publicPlayer(ctx,row.user_id)));
  const last = selected.at(-1);
  return {items,serverTime:ctx.now,nextCursor:rows.length>size
    ? sealCursor(ctx,'player-search',{scope,watermark,last:[Number(last.updated_at),last.user_id]}) : null};
}
