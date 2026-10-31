# Matchora product foundation

This project is being shaped from the Sports Network OS product blueprint.

## MVP surfaces

- Home: personalized action center and Play Now entry point
- Discover: approximate-location discovery with list/map/filter states
- Play: Play Now, create event, find opponent, need player, team challenge
- Event: join, participants, contextual event chat, check-in, result and rating
- Trust: age/phone verification, Reliability, report and block flows
- Profile: Sports Passport, verified history, badges and team relationships

## Shared domain boundaries

The shared domain models cover identity, sports passport, events, participants,
teams, challenges, check-ins, results, rating history, reports, wallet ledger,
contextual chat, notifications and audit logs. Backend implementations should
preserve append-only history for wallet, reliability and rating events.

## Deferred phases

- V2: ranking, tournament, waitlist, badges, missions, rematch and communities
- V3: venue booking, organizer OS, campus/company packages and Auto-Squad
- V4: AI Organizer, Travel Mode, Sports Wrapped and advanced analytics

The MVP should validate city liquidity and safe repeatable play before these
features are promoted into the primary navigation.

## Cross-cutting release contracts

Every feature must carry loading, empty, error, offline and permission-denied
states; analytics impression/action/success events; privacy and abuse handling;
accessibility labels; localization-ready strings; feature-flag rollback; and an
operator/support path. Trust scores must remain separate from skill ratings.

Payment work must keep digital token purchases and physical venue payments in
separate ledgers. Wallet transactions are append-only and idempotent. Match
results remain disputable until the configured hold expires.

Location surfaces show approximate areas or venue points, never a user's live
precise location. Event chat is participant-scoped, direct messages are gated by
shared activity or consent, and report/block actions are first-class flows.

## Design direction

The visual system uses the clarity of finance dashboards, the scanability of
commerce lists, and the progress language of activity products, then translates
them into Matchora's coral, ink, cream, mint, lilac and blue palette. No source
app screen is copied; the shared patterns are information hierarchy, concise
cards, progressive disclosure, clear status and one dominant action.