# TRIAD Mission Control — Android App · Handover

**Repo:** `github.com/djordi10/triad-mc-android-build` (**PUBLIC** — see Security)
**Latest:** release `v18` · all 19 views live-wired · in-app auto-update working
**Backend:** `https://triad-mc.bgzr.io` (triad-mcp, read/replay/propose window)

---

## 1. What this is

A **fully native** Android app (Kotlin 2.0 · Jetpack Compose · Material 3) that is a client of
`triad-mcp` — the same read-only window the web Mission Control dashboard uses. It boots straight
into **LIVE** against `triad-mc.bgzr.io` and renders all **19 views** of the v5.16 dashboard
roster, each built to its v1.0 wiring doc, reading real MCP data.

**The wall (unchanged, enforced in the type system):** the app reads, replays, and **proposes**.
The MCP client exposes exactly `call()` (read, verb-denylisted), `propose()`, `recordCheckup()`,
and `listTools()` (read). There is no code path from a tap to an order. Config changes can only be
**proposed**; a human applies them via `triad-config compile → triadctl config verify → apply`.

### Lineage
- Base: `TriadAgentic/TriadLearning` PR #62 ("v5.11 facelift — segmented nav, all views") at
  commit `d6b16c3`, extracted as a standalone Gradle project (repo root = the android module).
- Extended here to the **v5.16 roster** (+Topology, Prompt Studio, Lanes, Connections, MCP) and
  live-wired throughout. The TriadAgentic org repo could not build APKs (Actions billing blocked);
  this personal repo exists so CI can run on free public-repo minutes.

## 2. The 19 views (4 segments — `ui/nav/View.kt`)

| Segment | Views |
|---|---|
| OPERATE | 00 Topology · 01 Overview · 02 Executor · 03 Checkup · 04 Ops·Loops |
| ANALYSE | 05 Analytics · 06 Trade Logs · 07 Databank · 08 Query Console |
| MODEL·LEARN | 09 Intelligence·CAG · 10 Prompt Studio · 11 Shadow·Personas · 12 Books·Calibration · 13 Learning Pipeline |
| CONTROL | 14 Config Store · 15 Lanes · 16 Governance · 17 Connections · 18 MCP |

Phone: bottom bar = ☰ **Views** launcher (opens all 19 by segment) + the primary views.
Wide window: full segmented rail. Top bar: mode badge + Connect + Propose.

## 3. Source map (`app/src/main/java/agentic/triad/missioncontrol/`)

- `MainActivity.kt` — Compose host; fires the auto-update check on launch.
- `TriadApp.kt` — manual DI; **boots LIVE** via `goLive(LIVE_ENDPOINT)` (token in `?token=` query);
  `goDemo()` fallback; `propose()` is the only write path. `LIVE_ENDPOINT` constant lives here.
- `mcp/LiveMcpClient.kt` — the protocol that actually works against this server:
  `initialize` → capture **`Mcp-Session-Id`** header → `notifications/initialized` → every
  `tools/call` carries the session header; responses are **SSE** (`data:` line); tool payload =
  `result.content[0].text` parsed as `{ok, data}`. Re-handshakes once on HTTP 400.
  `listTools()` = the real Test-connection. **A bare tools/call without a session gets 400** — this
  is why the original client never worked.
- `mcp/MutatingDenylist.kt` — read guard. `get_*/list_*/run_*/search_*` always pass (a
  `get_kill_state` READS the switch); mutating verbs throw before the network.
- `data/` — `MissionRepository` (the seam), `LiveRepository` (per-tool last-good cache → honest
  `Stale`), `DemoRepository` (fixtures).
- `ui/ToolsViewModel.kt` — the one live poller every view uses: polls its tool list every 30s into
  a `data: Map<String, JsonElement?>` (the web `D` store analog).
- `ui/components/Components.kt` — McCard, StanceStrip, Ribbon, LawBlock, MiniTable, KvRow, Tag,
  StatRow, Tone (incl. SEV/UNK). **`PendBox` is a deliberate no-op** (operator chose to hide
  PEND boxes); flip it back to visible by restoring its body.
- `ui/components/Json.kt` — null-tolerant readers (`num/int/text/bool/obj/arr/field/rows/list/
  numEntries/sumValues`, `fmt`). Missing key → em-dash, never a fabricated zero.
- `ui/overview/Overview.kt` + `ui/views/*.kt` — the 19 screens (OperateViews, AnalyseViews,
  ModelLearnViews, ControlViews, ControlPlaneViews, Lanes, PromptStudio).
- `update/Updater.kt` — auto-update (see §5).
- `work/BroadcastWorker.kt` — checkup digest notifications (from the base app).

## 4. Design documents implemented

Wiring docs (in Djordi's Downloads, also the source of the laws/panels): Overview, Executor,
Checkup, Ops, Analytics, Lanes, Prompt-Studio, ConfigStore, Control-Plane — all v1.0. Views
without a doc (Trade Logs, Databank, Query, Intelligence, Shadow, Books, Learning Pipeline) were
ported from the deployed dashboard's own render functions (v5.16 shell).

**Audit status (full acceptance-table pass, 2026-07-15):** Lanes 13/13 · Prompt Studio 16/16 ·
Governance clean · Checkup/Ops/Config/Connections gaps **fixed in v17–v18** (census grid, work
list, DSN contradiction, run history, the 14-row failure matrix, FLOW&LANES, latency law, all
Config domain viewers, real CLIENT-tier connection controls). A live field-mapping audit probed
**55 tools**; all reads now match the real payload shapes.

## 5. Auto-update (how new builds reach the phone)

1. `versionCode = $VERSION_CODE` (CI sets it to `github.run_number`) — strictly increasing.
2. CI (`.github/workflows/android-apk.yml`) builds on every push and **publishes a GitHub
   Release** `vN` with `app-debug.apk` attached (`--latest`).
3. On every app launch `Updater.checkAndInstall()` GETs
   `api.github.com/repos/djordi10/triad-mc-android-build/releases/latest` (unauthenticated),
   parses `v(\d+)`, and if newer **downloads the APK and fires the installer intent**
   (FileProvider + `REQUEST_INSTALL_PACKAGES`). Android requires one "Install" tap — that's a
   platform guarantee, not a bug. First time: allow "install unknown apps".
4. Failures are silent (offline/rate-limit) — the app just runs what it has.

**So: push to `main` → green CI → release vN → phones self-update on next open.**

## 6. Operations

```bash
# build + release (anything pushed to main)
git push origin main                     # CI: tests → assembleDebug → artifact → Release vN

# manual trigger / fetch
gh workflow run android-apk.yml --repo djordi10/triad-mc-android-build --ref main
curl -sSL -o triad.apk "$(gh api repos/djordi10/triad-mc-android-build/releases/latest --jq '.assets[0].browser_download_url')"

# change the MCP endpoint/token (baked)
#   TriadApp.kt  -> LIVE_ENDPOINT
#   MainActivity has no endpoint; Connections view can repoint at runtime (not persisted)
```

Local build needs JDK 17 + Android SDK 34 (`local.properties: sdk.dir=…`), then
`./gradlew :app:assembleDebug`. The authoring environment had no SDK — CI is the compiler.

## 7. Security — read this

- **The MCP token is baked into the APK** (`TriadApp.LIVE_ENDPOINT`) **and the repo is PUBLIC**
  (required for free CI + unauthenticated auto-update). Anyone with the URL can read the TRIAD
  window and file proposals (never trade — the wall holds server-side). This was an explicit
  operator decision. **Rotate the token** when convenient: change the Cloudflare-edge token,
  update `LIVE_ENDPOINT`, push (auto-update ships it).
- Alternatives if posture changes: no-token build (enter endpoint+token once in Connections;
  Keystore-stored), or host releases behind your own infra.
- Debug-signed APK (sideload). For wider distribution, add a release keystore + Play track.

## 8. Known gaps / honest state

- **~20 server tools the docs call for don't exist yet** (`get_scan_board`, `get_vr_scoreboard`,
  `get_governor_chain`, `get_money_path`, the 16-tool control plane, `prompt_get/set`, …).
  Affected panels render honest UNKNOWN (PEND boxes exist in code but are hidden). When the
  backend ships a tool, the panel goes live with no app change.
- Prometheus/NATS/venue are absent server-side → exec-quality/feeds/bus panels read UNKNOWN.
  Correct behavior, not a bug.
- The native views render tables/stats/ribbons — **not** the web's decorative SVG charts
  (histogram bars are tabular). Function-for-function web parity = the WebView variant
  (one commit in history has it: a WebView shell over the deployed dashboard).
- MODEL·LEARN + Trade Logs/Databank/Query views follow the *web modules*; no standalone wiring
  docs exist for them yet — re-verify when their v1.0 docs land.
- Checkup/Ops "history" panels depend on `get_checkup.history` being served; they degrade
  honestly when absent.

## 9. Version history (releases)

- **v15** — first auto-update baseline (updater + CI releases).
- **v16** — nav/menu fixes lineage (Views launcher bottom-bar, PEND hidden, denylist read-prefix
  fix, 7 lighter views deepened, v5.16 roster with 5 new views).
- **v17** — audit wave-1: real `get_limits` nested paths (Prompt Studio was showing hardcoded
  numbers), Query Console canned queries → allowlisted views, proposal titles from `args.title`,
  dropped always-erroring `get_decision(_chain)` polls, updater tag-parse hardened.
- **v18 (current)** — audit wave-2: full Checkup panels (verbatim green reasons, per-plane
  coverage, work list, DSN contradiction, run history, GREEN guard), Ops 14-row failure matrix +
  FLOW&LANES + latency law + plane supervision, Config per-domain viewers, real Connections/MCP
  CLIENT controls with live `tools/list` Test-connection.

## 10. Related planes (context)

- **Web dashboard** (deployed): `https://triad-mc.bgzr.io/` (+ `config-store.html`,
  `analytics.html`) — the design source of truth; the app tracks it.
- **Config apply pipeline** (out of app scope): compile exists (`TriadLearning
  src/triad_core/config_store`), destinations vendored per `configs/apply_map.v1.json`, but
  **`triadctl config apply` is not built yet** — config application is a human CLI step and today
  even that step is missing its implementation. The app's Propose lane is the only mobile input.
- **API doc**: `TRIAD-API-Documentation.md` (v1.0, 2026-07-13) — 3 surfaces (`/mcp`,
  `/api/triad/*.csv`, `/control/*`), exactly 2 write tools; no config-write tool exists.

---
*Handover written 2026-07-15 by Claude (session with Djordi/Liko). The app is complete against
the docs available at that date; the auto-updater makes every future push self-deploying.*
