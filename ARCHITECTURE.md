# Matchora architecture foundation

## Client

Kotlin Multiplatform and Compose Multiplatform share the product UI, domain
models, state contracts and repository interfaces. Platform code remains
available for push notifications, location permissions, maps, billing, deep
links, camera and background work.

## MVP backend boundary

Start with one modular monolith. Keep these modules separate in code and data:

- Identity and authentication
- Profile and Sports Passport
- Geo and discovery
- Matchmaking and explainable Match Score
- Events, participants, waitlist and state machine
- Teams and challenges
- Trust, verification, reliability and moderation
- Wallet and billing
- Contextual chat and notifications
- Analytics and feature flags

## Data infrastructure target

- PostgreSQL + PostGIS for relational and approximate-area queries
- Redis for cache, rate limits and ephemeral queues
- Object storage for profile, verification and result evidence
- Queue/event bus for notifications, score recalculation and moderation work
- Managed Supabase can accelerate the MVP, while repository interfaces preserve
  the option to extract services later

## Non-negotiable contracts

- Wallet transactions are append-only and idempotent.
- Digital token billing and physical venue billing use separate ledgers.
- Match state changes, disputes and admin actions are auditable.
- Rating history and Reliability events are retained, not overwritten snapshots.
- Exact live location is never exposed to other players.
- Event chat is participant-scoped; direct messaging requires a trust gate.
- Reports use risk scoring and human review rather than automatic bans only.
- Feature flags, notification deduplication, rate limits and deletion/retention
  policy are release requirements.

## Product phases

MVP proves city liquidity and safe repeatable play. V2 adds ranking, tournament,
waitlist automation, badges, missions, rematch and communities. V3 adds venue,
booking, organizer tools, campus/company packages and Auto-Squad. V4 adds AI
Organizer, Travel Mode, Sports Wrapped and advanced analytics only after the
core network demonstrates repeat usage.
