import test from 'node:test';
import assert from 'node:assert/strict';
import { fixture } from './helpers.mjs';
import { createApi } from '../src/server.mjs';

const fails = code => error => error.code === code;
const community = async (f, type, extra = {}) => (await f.cmd(f.host, 'community.create', {
  name: `${type} spor ağı`, type, city: 'İstanbul', area: 'Kadıköy', access: 'OPEN', ...extra
})).result.id;

test('invitation ID is a permanent unique UUID, replay keeps it and lookup ignores browse city', async t => {
  const f = await fixture(t); await f.grant(f.host); await f.grant(f.guest);
  const payload = f.event({ city: 'Ankara', area: 'Çankaya' });
  const first = await f.cmd(f.host, 'event.create', payload, 'permanent-invitation');
  const replay = await f.cmd(f.host, 'event.create', payload, 'permanent-invitation');
  assert.equal(first.result.id, first.result.invitationId); assert.deepEqual(replay.result, first.result);
  assert.match(first.result.id, /^[a-f0-9-]{36}$/);
  const second = await f.cmd(f.host, 'event.create', payload); assert.notEqual(first.result.id, second.result.id);
  assert.equal((await f.service.bootstrap(f.guest, { city: 'İstanbul' })).events.length, 0);
  const found = await f.service.event(null, first.result.id.toUpperCase()); assert.equal(found.city, 'Ankara');
  await f.cmd(f.guest, 'event.join', { eventId: found.id });
  assert.ok((await f.service.bootstrap(f.guest, { city: 'İstanbul' })).joinedEventIds.includes(found.id));
  await assert.rejects(f.service.event(null, '../../account'), fails('INVALID_EVENT_ID'));
});

test('four listing networks persist canonical names and private rosters reject mismatched, pending and archived communities', async t => {
  const f = await fixture(t); await f.grant(f.host); await f.grant(f.guest);
  const campus = await community(f, 'CAMPUS'), company = await community(f, 'COMPANY'), club = await community(f, 'CLUB');
  for (const [networkScope, communityId] of [['CAMPUS', campus], ['COMPANY', company]]) {
    const id = (await f.cmd(f.host, 'event.create', f.event({ networkScope, communityId, networkName: 'Sahte ağ' }))).result.id;
    assert.equal((await f.service.event(f.host, id)).networkName, `${networkScope} spor ağı`);
    await assert.rejects(f.service.event(f.guest, id), fails('NOT_FOUND'));
    await f.cmd(f.guest, 'community.join', { communityId });
    await f.cmd(f.guest, 'event.join', { eventId: id });
  }
  for (const [networkScope, communityId] of [['CAMPUS', company], ['COMPANY', campus], ['CAMPUS', club]])
    await assert.rejects(f.cmd(f.host, 'event.create', f.event({ networkScope, communityId })), fails('COMMUNITY_REQUIRED'));
  const approval = await community(f, 'CAMPUS', { access: 'APPROVAL' });
  await f.cmd(f.guest, 'community.join', { communityId: approval });
  await assert.rejects(f.cmd(f.guest, 'event.create', f.event({ networkScope: 'CAMPUS', communityId: approval })), fails('COMMUNITY_REQUIRED'));
  await f.cmd(f.host, 'community.archive', { communityId: approval });
  await assert.rejects(f.cmd(f.host, 'event.create', f.event({ networkScope: 'CAMPUS', communityId: approval })), fails('COMMUNITY_REQUIRED'));
  for (const networkScope of ['CITY', 'TRAVEL']) {
    const id = (await f.cmd(f.host, 'event.create', f.event({ networkScope, city: 'İzmir', networkName: 'Yanlış şehir', communityId: campus }))).result.id;
    const view = await f.service.event(null, id);
    assert.equal(view.communityId, null); assert.equal(view.networkName, networkScope === 'TRAVEL' ? 'İzmir' : null);
  }
});

test('ID lookup over HTTP preserves private membership and approval, full and cancelled roster rules', async t => {
  const f = await fixture(t); for (const actor of [f.host, f.guest, f.other]) await f.grant(actor);
  const server = createApi(f.service, f.auth); await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`;
  const campus = await community(f, 'CAMPUS');
  const hidden = (await f.cmd(f.host, 'event.create', f.event({ networkScope: 'CAMPUS', communityId: campus }))).result.id;
  assert.equal((await fetch(`${base}/v1/events/${hidden}`)).status, 404);
  const id = (await f.cmd(f.host, 'event.create', f.event({ capacity: 3, approvalRequired: true }))).result.id;
  assert.equal((await fetch(`${base}/v1/events/${id}?city=Ankara`)).status, 200);
  await assert.rejects(f.cmd(f.guest, 'event.join', { eventId: id }), fails('APPROVAL_REQUIRED'));
  // Doğrudan davet bypass: davet koduna veya invitationId'ye sahip olan katılımcı onaysız doğrudan katılır
  await f.cmd(f.guest, 'event.join', { eventId: id, bypassApproval: true, invitationId: id });
  assert.ok((await f.service.bootstrap(f.guest)).joinedEventIds.includes(id));
  const request = (await f.cmd(f.other, 'event.request', { eventId: id })).result.id;
  await f.cmd(f.host, 'event.request.review', { requestId: request, approve: true });
  const extra = (await f.cmd(f.host, 'event.create', f.event({ capacity: 2, approvalRequired: true }))).result.id;
  await f.cmd(f.guest, 'event.join', { eventId: extra, bypassApproval: true });
  await assert.rejects(f.cmd(f.other, 'event.join', { eventId: extra, bypassApproval: true }), fails('EVENT_FULL'));
  const version = (await f.service.event(f.host, id)).version;
  await f.cmd(f.host, 'event.transition', { eventId: id, expectedVersion: version, status: 'CANCELLED' });
  await assert.rejects(f.cmd(f.other, 'event.request', { eventId: id }), fails('EVENT_CLOSED'));
});
