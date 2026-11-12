import { ensure, choice, integer, json } from './errors.mjs';
import { assertTeamOpen } from './team-matches.mjs';

export function tournamentRules(input) {
  const formatCode = choice(input.formatCode ?? (input.isLeague ? 'ROUND_ROBIN' : 'KNOCKOUT'), ['KNOCKOUT', 'ROUND_ROBIN'], 'Turnuva biçimi');
  const capacity = integer(input.capacity, 'Takım sayısı', 2, formatCode === 'ROUND_ROBIN' ? 16 : 32);
  if (formatCode === 'KNOCKOUT') ensure((capacity & (capacity - 1)) === 0, 400, 'INVALID_CAPACITY', 'Eleme kapasitesi 2, 4, 8, 16 veya 32 olmalı.');
  ensure(input.verifiedOnly == null || typeof input.verifiedOnly === 'boolean');
  return { formatCode, capacity, isLeague: formatCode === 'ROUND_ROBIN',
    minRating: integer(input.minRating ?? 0, 'En düşük MMR', 0, 5000), verifiedOnly: input.verifiedOnly === true,
    rosterSize: input.rosterSize == null ? null : integer(input.rosterSize, 'Maç kadrosu', 1, 25) };
}

export async function assertTournamentRoster(ctx, tournament, team, roster) {
  assertTeamOpen(team);
  const rules = json(tournament.payload);
  ensure(roster.length > 0 && roster.length <= 50 && new Set(roster).size===roster.length, 409, 'INVALID_ROSTER', 'Takım kadrosu 1–50 aktif sporcudan oluşmalı.');
  if(rules.rosterSize!=null)ensure(roster.length===rules.rosterSize,409,'ROSTER_SIZE_REQUIRED',`Bu turnuvada takım başına ${rules.rosterSize} sporcu seçmelisin.`);
  for (const userId of roster) {
    ensure(await ctx.db.get("SELECT user_id FROM team_members WHERE team_id=? AND user_id=? AND status='joined'",[team.id,userId]),409,'INVALID_ROSTER','Turnuva kadrosundaki sporcu takımın aktif üyesi olmalı.');
    const saved = await ctx.db.get("SELECT p.payload FROM profiles p JOIN users u ON u.id=p.user_id WHERE p.user_id=? AND u.status='active'", [userId]);
    ensure(saved, 409, 'INCOMPLETE_PROFILE', 'Kadroda aktif pasaportu olmayan sporcu var.');
    const player = json(saved.payload), skill = player.skills.find(s => s.sport === tournament.sport);
    ensure(skill && skill.rating >= (rules.minRating ?? 0), 403, 'TOURNAMENT_RATING_REQUIRED', 'Kadroda turnuvanın branş/MMR koşulunu karşılamayan sporcu var.');
    ensure(!rules.verifiedOnly || player.verifications.includes('BASIC'), 403, 'TOURNAMENT_VERIFICATION_REQUIRED', 'Turnuva kadrodaki her sporcu için telefon doğrulaması istiyor.');
  }
}
