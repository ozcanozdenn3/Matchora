import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { PGlite } from '@electric-sql/pglite';

const sql = await readFile(new URL('../supabase/04_reconcile_passport_stats.sql', import.meta.url), 'utf8');

test('passport SQL rebuilds verified attendance and consecutive Istanbul weeks without awarding pending matches or changing MMR', async t => {
  const db = new PGlite();
  t.after(() => db.close());
  await db.exec(await readFile(new URL('../migrations/001_core.sql', import.meta.url), 'utf8'));
  const week = Number((await db.query(`SELECT FLOOR((EXTRACT(EPOCH FROM transaction_timestamp()) * 1000 + 3 * 3600000 + 3 * 86400000) / (7 * 86400000))::bigint AS week`)).rows[0].week);
  const original = {
    name: 'Sporcu', verifiedMatches: 0, streakWeeks: 0, reliability: 94, fairPlay: 4.6,
    skills: [{ sport: 'FOOTBALL', rating: 1234, position: 'Orta saha', level: 'INTERMEDIATE' }],
    skillArchive: { TENNIS: { sport: 'TENNIS', rating: 1456, verifiedMatches: null, custom: 'preserve' } }
  };
  for (const [id, payload] of [['athlete', original], ['pending-user', { name:'Bekleyen', skills:[{sport:'FOOTBALL',rating:1000}] }], ['previous-week', {skills:[]}], ['expired-week', {skills:[]}]]) {
    await db.query('INSERT INTO users(id,subject,created_at) VALUES($1,$1,1)', [id]);
    await db.query('INSERT INTO profiles(user_id,payload,version,updated_at) VALUES($1,$2,5,1)', [id,JSON.stringify(payload)]);
  }
  async function match(id, user, offset, sport = 'FOOTBALL', { checkedIn = true, result = 'verified', side = 'home', homeScore = 2, awayScore = 1 } = {}) {
    const endsAt = (week - offset) * 7 * 86400000 - 3 * 86400000 - 3 * 3600000 + 3600000;
    await db.query(`INSERT INTO events(id,host_id,sport,status,capacity,starts_at,ends_at,commitment,city,district,payload,created_at) VALUES($1,$2,$3,'COMPLETED',2,$4,$5,10,'İstanbul','Kadıköy','{}',1)`, [id,user,sport,endsAt-3600000,endsAt]);
    await db.query(`INSERT INTO event_members(event_id,user_id,status,side,checked_in_at,joined_at) VALUES($1,$2,'joined',$3,$4,1)`, [id,user,side,checkedIn?endsAt-1000:null]);
    if (result) await db.query(`INSERT INTO results(event_id,submitted_by,payload,status,dispute_until,created_at) VALUES($1,$2,$3,$4,1,1)`, [id,user,JSON.stringify({homeScore,awayScore}),result]);
  }
  await match('current', 'athlete', 0);
  await match('previous', 'athlete', 1, 'FOOTBALL', { side:'away' });
  await match('two-weeks', 'athlete', 2, 'TENNIS');
  await match('gap', 'athlete', 4, 'TENNIS', {homeScore:1,awayScore:1});
  await match('not-present', 'athlete', 0, 'FOOTBALL', {checkedIn:false});
  await match('pending-result', 'pending-user', 0, 'FOOTBALL', {result:'pending'});
  await match('no-result', 'pending-user', 0, 'FOOTBALL', {result:null});
  await match('grace', 'previous-week', 1);
  await match('expired', 'expired-week', 2);
  // Reproduce editors that submit each top-level statement independently.
  const blockStart = sql.indexOf('DO $matchora_passport_repair$');
  const blockEnd = sql.indexOf('$matchora_passport_repair$;', blockStart) + '$matchora_passport_repair$;'.length;
  assert.ok(blockStart >= 0 && blockEnd > blockStart);
  await db.exec(sql.slice(0, blockStart));
  await db.exec(sql.slice(blockStart, blockEnd));
  await db.exec(sql.slice(blockEnd));

  const record = (await db.query(`SELECT payload,version,updated_at FROM profiles WHERE user_id='athlete'`)).rows[0];
  const repaired = JSON.parse(record.payload);
  assert.equal(repaired.verifiedMatches,4);
  assert.equal(repaired.streakWeeks,3);
  assert.deepEqual(repaired.skills[0], {...original.skills[0],verifiedMatches:2,wonMatches:1,winRate:50});
  assert.deepEqual(repaired.skillArchive.TENNIS, {...original.skillArchive.TENNIS,verifiedMatches:2,wonMatches:1,winRate:50});
  assert.equal(repaired.reliability,94);assert.equal(repaired.fairPlay,4.6);
  assert.equal(record.version,6);
  const pending = JSON.parse((await db.query(`SELECT payload FROM profiles WHERE user_id='pending-user'`)).rows[0].payload);
  assert.equal(pending.verifiedMatches,0);assert.equal(pending.streakWeeks,0);
  assert.equal(pending.skillArchive,undefined);
  const grace = JSON.parse((await db.query("SELECT payload FROM profiles WHERE user_id='previous-week'")).rows[0].payload);
  const expired = JSON.parse((await db.query("SELECT payload FROM profiles WHERE user_id='expired-week'")).rows[0].payload);
  assert.equal(grace.streakWeeks,1);assert.equal(expired.streakWeeks,0);
  const untouchedResults = (await db.query(`SELECT event_id,status FROM results ORDER BY event_id`)).rows;
  await db.exec(sql);
  assert.deepEqual((await db.query(`SELECT payload,version,updated_at FROM profiles WHERE user_id='athlete'`)).rows[0],record);
  assert.deepEqual((await db.query(`SELECT event_id,status FROM results ORDER BY event_id`)).rows,untouchedResults);
  await db.exec('BEGIN');
  await db.exec(sql.slice(blockStart, blockEnd));
  await db.exec(sql.slice(blockStart, blockEnd));
  await db.exec('COMMIT');
  assert.deepEqual((await db.query(`SELECT payload,version,updated_at FROM profiles WHERE user_id='athlete'`)).rows[0],record);
});
