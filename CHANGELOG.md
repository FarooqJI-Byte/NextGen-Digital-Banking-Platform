# Changelog

All notable changes to the NextGen Digital Banking Platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
- 2026-08-11: Created `develop` branch and integrated PR #1 (`nikhitamdesai:main`) containing Account and Card backend modules, DTOs, repositories, services, tests, and frontend `account-card` UI components. Resolved compilation and state machine validation issues; verified cleanly with `mvn test` (31 passing tests) and `npm run build`.
- 2026-08-06: Redesigned `WORKFLOW-GUIDE.html` — clean light-mode HTML web tool guiding developers through the ChatGPT <-> Antigravity iteration loop. Features strategy prompt generator (instructing ChatGPT not to guess codebase technicalities), developer attention notice rules, and 1-click prompt copiers.
- 2026-08-06: Created `AI-SESSION-STARTER.md` — master session starter prompt for teammates to paste into their AI IDE at the start of any coding session to load full context and get guided module implementation.
- 2026-08-05: Created `TEAM-MESSAGE.md` — final personalized team kickoff brief with module assignments (Farooq/Nikitha/Divya+Ankit/Mithun), build order, 4 integration checkpoints, per-person reading list, 5 integration rules, Definition of Done checklist, and 15 Aug deadline.
- 2026-08-05: Created `VERIFY-PROMPT.md` — AI master verification prompt teammates paste into their IDE to check their module against all 8 design doc requirements before marking work done.
- 2026-08-02: Executed Design System Refresh — dark-nav / warm-canvas tokens, `HorizonCard`, `AskAIBar`, updated `StatusBadge`, `Button`, `DataTable`, `FormField`, `ComponentShowcase`, and verified frontend build (`npm run build`).
- 2026-08-02: Created Docker onboarding and troubleshooting guide (`DOCKER-GUIDE.md`).
- 2026-08-02: Created project status tracking system (`PROJECT-STATUS.md`, `AGENTS.md`, `CHANGELOG.md`).
- 2026-08-02: Fixed Aadhaar VO with Verhoeff algorithm validation and added unit tests (`AadhaarTest`).
- 2026-08-02: Configured Flyway as single source of truth (`V1__initial_schema.sql`), added Flyway dependencies to `pom.xml`, and updated `docker-compose.yml` to start PostgreSQL with an empty schema.
- 2026-08-02: Scaffolded `com.nextgen.bank.common` shared package, backend folder boundaries, Python `ai-loan-service`, and frontend domain modules.
- 2026-08-02: Generated 12 technical design documents in `/docs`.
