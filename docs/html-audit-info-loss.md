# HTML → app info-loss audit

Comparison of the current Android views (after the meaning-first redesign sweep)
against the original reference **TRIAD-Mission-Control-v5_18-native.html**. One
subagent per view group read both sides and classified every info-bearing HTML
element as PRESENT, INTENTIONAL-OK (deliberately dropped: dedup / de-jargon /
fabricated-or-placeholder removed / folded into a hero-summary), or **POSSIBLE
INFO LOSS** (a real datum, warning, threshold, or claim now absent everywhere).

SUITE (views 21-25) is excluded: it has no HTML counterpart.

Scope: all 21 non-SUITE views (00-20).

**STATUS: restore complete.** Every Priority A/B/C item and every doable
Priority-D item has been restored across 6 commits (125abc8, bf614a1, 34825b3,
3087ba6, 1d6e8b7, 583ef92). The only open items are the **3 data-dependent**
ones at the very bottom, which cannot be restored without a real data series the
app does not yet carry (restoring them would mean fabricating numbers). Checked
boxes below carry their commit hash.

---

## Verdict per view

| View | Result |
|------|--------|
| 00 Topology | rebuilt onto newer v5.18 data — 3 items superseded but worth re-adding |
| 01 Overview | **clean** (1:1 port) |
| 02 Executor | **clean** (1:1 port + additive server panels) |
| 03 Checkup | 2 items (work-list probe IDs, per-cell census detail) |
| 04 Ops·Loops | 1 item (§10 F-matrix behaviour column) |
| 05 Reader/Writer | **clean** (1:1 port) |
| 06 Analytics | 4 items (CAG economics the real one) |
| 07 Trade Logs | 5 items (incl. the "broken gateway" thesis) |
| 08 Strategy | **clean** (1 minor: combo_registry spec) |
| 09 Databank | 2 items (refusal staleness, null-key stakes) |
| 10 Query Console | 2 items (lint rule L-12, provenance library) |
| 11 Intelligence·CAG | 5 items (incl. a **mislabel** worth fixing) |
| 12 Prompt Studio | **clean** (richer than HTML) |
| 13 Shadow·Personas | 3 items (triple-resolution, P-NOFLOOR, book CI) |
| 14 Books·Calibration | 2 items (3.8bps width, artifact hash) — minor |
| 15 Learning Pipeline | 4 items (w_cal §6.7 the real one) — mostly minor |
| 16 Config Store | 6 items (Operator-actions card the big one) |
| 17 Lanes | 1 item (per-lane description) — minor; dedup merges lost nothing |
| 18 Governance | 2 items (3 propose presets, kill armed:false) |
| 19 Connections | **clean** (superset of HTML) |
| 20 MCP | 1 item (a **mislabel** worth fixing) |

**Clean, no loss (6):** Overview, Executor, Reader/Writer, Strategy, Prompt Studio, Connections.

---

## PRIORITY A — correctness (app now shows something that reads as WRONG)

These are not just missing info; the current copy states the opposite of the HTML.

- [x] **20 MCP — `McpServerTile` note inverts the tier.** *(done — 125abc8; fixed both the tile note and the card-level "what it means" note)* The tile's enable/disable · set-token · Test · Add · Remove are **CLIENT-tier and real** in the HTML ("everything in this card is real"; the toggle stops *this dashboard* calling the server, distinct from `mcp_toggle` which stops the process). The Android note labels them "all SYSTEM control-writes that do not exist on the estate" — the opposite claim. Correct meaning survives in the C-2 WhyBox, but the tile text a user reads first is wrong. **Fix:** reword the tile note (ControlPlaneViews.kt ~L569-574) to "CLIENT-tier controls (real in the web app); this read-only mirror renders them disabled."
- [x] **11 Intelligence — `get_validator_rejects` mislabel.** *(done — 3087ba6; ribbon flags it as the governor's refusals view, points to Model rejects for the real validator kills)* HTML's keystone I-1 claim: `get_validator_rejects` returns the *governor's* 18-row refusals view (97-row hole, 100%-null PK), NOT the validator's kills — the real validator rejects live in `decisions.body.validator.checks_failed` and no tool reads them. The Kotlin view instead treats `get_validator_rejects.by_check` as the authoritative kill sheet, i.e. renders the exact table the HTML warns is the wrong source. **Fix:** add a ribbon flagging get_validator_rejects as the governor's table and that the real validator rejects are unsurfaced.

## PRIORITY B — a view's central thesis / a real guardrail is gone

- [x] **07 Trade Logs — "broken gateway, not a picky model."** *(done — 34825b3; thesis ribbon added under Decision census)*
- [x] **04 Ops — §10 F-matrix mandatory-behaviour column (F1-F14).** *(done — bf614a1; added `beh` to all 14, rendered as a "required response" line, verified live)* The 4th column, the required fail-closed/fail-open response per failure mode (e.g. F7 "degrade to defensive: venue stops only, halt entries, page"; F14 "§11.6 immediately, all scopes"), is dropped from `OpsFSpec`; it renders only if the live `get_failure_matrix` returns `behavior`. In demo/unserved state the 14 required behaviours — the whole point of the matrix — are invisible. **Restore:** add a `beh` field to `OPS_FMATRIX` and render per row (collapsible).
- [x] **16 Config Store — Operator actions card entirely gone.** *(done — 1d6e8b7; added an "Operator actions" card with all four controls + safety semantics + the "exits keep managing, never orphan" guarantee, verified live)*
- [x] **10 Query — lint rule L-12 TS_UNIT (absent from the entire app).** *(done — 34825b3; added L-12 TS_UNIT and L-2 BANK_INFLATION to `qcLint`)*

## PRIORITY C — specific numbers / warnings a user could want (per view)

**00 Topology** (superseded by newer data, but the magnitudes carry the severity) — *(all done — 125abc8)*
- [x] Quantified inflation chain: `164 dup candidates → 8 dup adjudications → 5,277 dup bank rows → 2.93× → counterfeit net_pnl_r` + `§7.2 idempotency` (re-added to `NatsRootCauseRibbon`).
- [x] Ledger provenance findings: `chain_verified:false` / `input_hash=0×64 on 1,825 rows` / `refusal_id 100% null` (added to the learning node).
- [x] Signal replay gap: `45,692 context packets have no view — P4 replay dead at first hop` (added to the signal node).

**03 Checkup** — *(both done — bf614a1)*
- [x] Work-list probe IDs + secondary evidence (`w.ids`, remaining `w.ev`) — now shows all evidence lines + an "unblocks <probe ids>" line per source.
- [x] Per-census-cell detail — plane groups now name the components behind the cells and tag UNKNOWN ones (lightweight; no tap-drawer).

**06 Analytics** — *(main item done — 34825b3)*
- [x] CAG economics unique fields: `latency saved ~103s`, `audit agreement 0.95 min · window 50`, `memo-key incl. checkpoint LRN-8` — added to the Latency+CAG card as a "guardrails" LeverTable.
- [ ] Validity-trend series (day0→now 39→28→30) reduced to one KvRow; the stale-storm dip/recovery shape gone. *(minor, deferred)*
- [ ] `26 symbols` / `8 cohorts` scoping sub-labels; failure-histogram caption. *(minor, deferred)*

**07 Trade Logs** (besides the thesis in Priority B) — *(main items done — 34825b3)*
- [x] Dupe-by-symbol writer race — added to Row integrity (renders `by_symbol` for the worst layer when served).
- [x] Root-cause chain: NATS → no dedupe → §7.2 → 164→8→5,277 — added as a note on Row integrity.
- [x] Absence "sting": all 8 SMC fields null → cannot post-mortem — added to Fabrication audit.
- [x] Replay-chain verification in Row detail — added chain_verified + per-hop hash + P4/§21.2 ribbon.
- [ ] Conviction cache overlay ("caching 22, 34 of 43 hits"); `ts` HH:MM no-date/no-tz caveat. *(minor, deferred)*

**09 Databank** — *(both done — 34825b3)*
- [x] Newest-refusal staleness (~8.3h) + stale-service claim — added to the hole card.
- [x] D-3 null-key stakes framing (governor is the only thing between model and money) — added to the hole card.

**10 Query** (besides L-12 in Priority B) — *(done — 34825b3)*
- [x] 13-query provenance library (Q-01..Q-13) embedded as `QC_LIB` static fallback; renders when `get_query_catalog` is unbuilt. Verified live (Q-11 $276k/11× finding present).

**11 Intelligence** (besides the mislabel in Priority A) — *(done — 3087ba6)*
- [x] CAG control law (1,598 died at 136ms = batch failure; llm_swap the one real, dangerous control) — ribbon on CAG economics.
- [x] Governor 9.1bps cross-ref — note on the kill sheet.
- [x] Killed-trade envelope (record_rejected_order discards entry/stop/targets/size) — note on Model rejects.
- [ ] Combinations-that-kill: 8-row co-failure table. *(deferred — data-dependent on get_model_rejects.combinations; top combo already shown)*

**13 Shadow** — *(all done — 3087ba6)*
- [x] Triple-resolution walkthrough (loss/win/loss, summed not resolved) — note on Bank dedup.
- [x] P-NOFLOOR 2.0-vs-2.5 mismatch + per-persona payoffs — note on the personas card.
- [x] Per-book stats: B0 expectancy/CI — note on The books.

**16 Config Store** (besides Operator-actions in Priority B) — *(all done — 1d6e8b7)*
- [x] `ttl_min_s` / `ttl_max_s` + validator-fallback note (1800s) — added to Risk & execution.
- [x] `symbol_exposure_cap_pct` + `correlation_bucket_cap_pct` — added to Risk & execution.
- [x] `guided_decoding` "format law" — added to The brain (LLM).
- [x] `take_band_lo` / `take_band_hi` — added to The brain (LLM).
- [x] Minor levers: `min_stop_atr_mult`, `funding_guard_min`, `requote_max`, `be_after_tp1`, `mark_drift_bps`, `cag3_max` — all added.

**18 Governance** — *(main done — 1d6e8b7)*
- [x] Three propose-action presets (`fix_get_alerts`, `run_key_safety_probe`, `run_kill_drill`) — restored to the propose kinds. Verified live.
- [ ] Kill-switch `configured.armed:false` nuance. *(minor, deferred)*

## PRIORITY D — minor / secondary (verbatim sub-quotes, decorative counts)

- [x] **14 Books:** *(done — 3087ba6)* 3.8bps structure width in the deadlock node; `calibration_artifact_hash: null` + `get_golden→not_found` note.
- [x] **15 Learning:** *(done — 3087ba6)* `w_cal` §6.7 quote; §3 per-hack predictions; §6.6 audits-are-the-design quote; T-5 time-to-corpus pace.
- [x] **17 Lanes:** *(done — 583ef92)* per-lane description text in each lane's drawer.
- [x] **08 Strategy:** *(done — 583ef92)* `get_combo_registry` PEND build-spec block.
- [x] **06 Analytics / 07 Trade Logs / 18 Governance minors:** *(done — 583ef92)* Analytics 26-symbols/8-cohorts sub-labels + failure-histogram caption; Trade Logs ts/acct data-quality caveat; Governance kill `configured.armed:false` nuance.

---

## Remaining — data-dependent, NOT fabricatable (3)

These need a real data series the app does not currently carry; adding them would mean inventing numbers, which violates the app's honest-data principle. Left for when the underlying tool/series ships:

- [ ] **06 Analytics — validity-trend line chart** (day0→now 39→28→30). Needs a per-day validity series; only a single live value is available today. Restore when a `validity_pct` time series is served.
- [ ] **07 Trade Logs — conviction cache overlay** ("caching 22, 34 of 43 hits"). Needs a cache-vs-fresh histogram series; only the fresh distribution is available. Restore when the cache series is served.
- [ ] **11 Intelligence — combinations-that-kill 8-row table.** Needs `get_model_rejects.combinations`; the top single combination is already shown. Restore when the tool returns the co-failure sets.
- [ ] **17 Lanes:** per-lane description `w` text (e.g. shadow-of-live "collects REAL / GATED / MISSED"; live_playground "real testnet fills, real latency") — add a `desc` field to the `Lane` data class.
- [ ] **08 Strategy:** full `get_combo_registry` build-spec block (substance survives in the combos Note).

---

## Notes on method / caveats

- Several "losses" on **Topology** are because the view was deliberately rebuilt onto newer v5.18 data (venue now "crossed", 13 intents, W-71 keeper crash-loop) — the old numbers are superseded, not merely deleted. Re-adding them means re-adding the *current* equivalents, not the stale figures.
- Many Config/Query "losses" are items re-pointed at **unbuilt PEND server tools** instead of carrying the HTML's always-available client-side content. Restoring = embedding a static fallback (as `QC_SCHEMA` already does for the schema card).
- The redesign's structural removals were verified sound: every dropped **hero StatRow / host.strip** restated the stance pills (and, for Governance, the board breakdown now in the HBarChart); the **Lanes** two-board and two-ledger merges dropped no unique data; the **shared tile grids** (ConnProfileTile / McpServerTile) kept every field when converted to LeverTable.
