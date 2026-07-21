# TRIAD Mission Control — MCP wiring checklist

Tracks which UI panels read **live MCP data** vs render **dummy / demo / doc-snapshot** data. Update the
Status column every wiring pass (flip ⬜ → ✅). Source of truth for the wiring effort.

**Status legend**
- ✅ `LIVE` — reads a working tool, renders its payload
- ⬜ `TODO-F1` — Fase-1 target: tool returns real data, wire it now
- ⏳ `DATABANK-F2` — tool is UNAVAILABLE until the server sets `TRIAD_DATABANK_DSN` to a real sqlite bank
  (root cause: `get_shadow_bank` / `get_databank` / `get_vr_scoreboard` return `transport: unavailable`).
  Server/infra change on liko's Mac — Fase 2. Keep the current snapshot/demo until then.
- 🔒 `PEND` — the MCP server has no such tool (designed, not built). Cannot wire app-side.
- 📄 `SPEC` — narrative / doc text, not backed by any tool. Stays static.
- 🐞 `BUG` — a wiring defect to fix.

**Tool availability (probed live 2026-07-21)**
- WORKING: `get_analytics` (incl. `equity_curve` + `pnl_by_hour`), `get_bank_priced`, `get_table_census`,
  `get_row_integrity`, `get_reader_writer_map`, `get_seam_audit`, `get_scan_board`, `get_books_scoreboard`,
  `get_venue_session`, `get_take_rate`, `get_conviction_histogram`, `get_detector_registry`,
  `get_decision_census`, `get_calibration`, `get_config_active`, `get_lanes`, `get_mcp_audit_summary`,
  `get_system_overview`, `get_service_status`, and most ledger-plane reads.
- UNAVAILABLE (Fase 2, one root cause): `get_shadow_bank`, `get_databank`, `get_vr_scoreboard`.

**Reference (already 100% live — the "done right" example):** `TradeLogsScreen`, `DatabankScreen`.

---

## OPERATE

### 01 Overview (`ui/overview/Overview.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Strip / estate / stance / edge / flow / next | ✅ LIVE | system_overview, service_status, books_scoreboard, calibration, continuity, go_no_go, proposals | — |
| Money path stages intents/orders/fills/outcomes | 🔒 PEND | get_money_path (404) → hardcoded 0 | :355 |
| Risk Sev-1 counter | 🔒 PEND | get_risk_envelope | :682 |
| Truth source-plane booleans | 🔒 PEND | get_truth_coverage | :722 |

### 02 Executor (`ui/views/OperateViews.kt`) — LIVE; one static input
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Rails / governor / sizing / venue / replay / server reads | ✅ LIVE | 23-tool set | — |
| `equity = 25_000.0` sizing input | 📄 SPEC | venue_wallet_snapshot absent | :390 |

### 03 Checkup (`ui/views/OperateViews.kt`) — ✅ LIVE throughout
### 04 Ops · Loops (`ui/views/OperateViews.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Invariant / failure-matrix / paging / loops / services / flow | ✅ LIVE | 28-tool set (spec ladders + live overlay) | — |
| Acceptance catalog §21 verdicts | 🔒 PEND | no acceptance/drill tool | :4127 |

### 05 Reader / Writer (`ui/views/ReaderWriter.kt`) — **Slice 1**
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Seam board — WRITERS rows/state | ✅ LIVE | get_table_census (rows overlay) + get_service_status (state) | :111,:391 |
| Reader lanes | ✅ LIVE | get_bridge_lag | :394 |
| The hole table | ✅ LIVE | get_seam_audit (+ reconstructed fallback) | :419 |
| Orphan & mismap | ✅ LIVE | get_reader_writer_map (counts) | :443 |
| Per-layer integrity grid `IntegGrid` | ✅ LIVE | get_row_integrity.layers + inflation_factor | :493 |
| Dedup / NATS narrative counts | 📄 SPEC | prose (inflation headline is live) | :467 |
| provision_nats | 🔒 PEND | — | :463 |

### 00 Topology (`ui/views/ControlPlaneViews.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Estate node dot-status | ✅ LIVE | service_status/bridge_lag | — |
| Node evidence strings + Distribution universe | 📄 SPEC | static topology roster | :959,:1139 |
| edge_flow / transport_actual / process_status / keyholder_status | 🔒 PEND | four §6 tools | — |

---

## ANALYSE

### 06 Analytics (`ui/views/AnalyseViews.kt`) — **Slice 2**
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Failure hist / conviction / CAG / attribution / exec quality (top block) | ✅ LIVE | get_analytics, get_conviction_histogram, get_cag_stats, get_attribution_ledger, get_exec_quality | :375+ |
| **Equity curve** | ⬜ TODO-F1 | get_analytics.equity_curve → LineChart | :317 |
| **WR / PnL by hour** | ⬜ TODO-F1 | get_analytics.pnl_by_hour | :354 |
| **Emission board** | ✅ LIVE | get_scan_board (cands_24h per symbol; fixture fallback) | :450 |
| **Regime + screen map** | ✅ LIVE | get_scan_board.board → screen_reason distribution | :465 |
| Equity curve / WR-by-hour | ⏳ EMPTY | get_analytics.equity_curve/pnl_by_hour all-zero (no closed trades); kept demo | :317,:354 |
| Outcome mix / WR-by-symbol / WR-by-stop / WR-by-side / Payoff dist | ⏳ DATABANK-F2 | get_vr_scoreboard (demo now) | :325,:337,:344,:349,:359 |
| Curriculum mix / Drift / Skip anatomy / Latency / Attribution preview | 📄 SPEC | bodies hardcoded; captions now honest WIRE-PENDING | — |
| ~14 other §01–05 cards | ⏳/📄 | get_vr_scoreboard / SQL-desc | :514–663 |

### 07 Trade Logs / 09 Databank — ✅ LIVE throughout (reference)
### 08 Strategy (`ui/views/Strategy.kt`) — **Slice 4**
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Track A/B + books scoreboard (n/ev overlay) | ✅ LIVE (partial) | get_books_scoreboard | :339 |
| Books table **WR + CI columns** | ⬜ TODO-F1 | get_books_scoreboard.wr/ciLo/ciHi (seed now) | :422 |
| Detector table | ⬜ TODO-F1 | get_detector_registry (`FLEET_DETECTORS`) | :390 |
| SMC tracks / combos tables | ⏳ DATABANK-F2 | get_shadow_bank / no combo tool | :406,:454 |
| Where the bleed is (stop bucket) | ⏳ DATABANK-F2 | get_shadow_bank group_by | :542 |
| detector_split / track_watch / resolve_stuck | 🔒 PEND | — | :479,:538,:610 |

### 10 Query Console — ✅ LIVE (schema seed + get_view_catalog overlay)

---

## MODEL · LEARN — mostly ✅ LIVE
### 11 Intelligence / 13 Shadow / 14 Books / 15 Learning (`ui/views/ModelLearnViews.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Most cards | ✅ LIVE | per-screen tool sets | — |
| Shadow "The reversal" (10/45 bps cost) | ⏳ DATABANK-F2 | get_bank_priced/get_shadow_bank geometry | :908 |
| Learning ladder / reward-terms / mitigations / principles | 📄 SPEC | constants (LADDER_RUNGS/REWARD_*/AUDITS/PRINCIPLES) | :1564+ |
### 12 Prompt Studio (`ui/views/PromptStudio.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| "96 tokens" kills / applied prompt / export | ✅ LIVE | get_validator_rejects, get_limits, prompt_get | — |
| The composer (14 blocks) | 📄 SPEC | defaultBlocks() (client-side by design) | :147 |
| The ledger bench (11,528) | 📄 SPEC | no decisions-ledger tool wired | :611 |
| prompt_set | 🔒 PEND | — | :850 |

---

## CONTROL — mostly ✅ LIVE
### 16 Config / 18 Governance (`ui/views/ControlViews.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Config domains / draft / gates board / silence / proposals | ✅ LIVE | get_config_preset/active, computed gates | — |
| Config "Operator actions" | 🔒 PEND | control-write tools | :377 |
| Governance "Sixteen rules" | 📄 SPEC | GOV_RULES const | :899 |
| Governance "tool reliability" | ⬜ TODO-F1 | get_mcp_audit_summary (`GOV_TOOL_RELIABILITY`) | :915 |
### 17 Lanes (`ui/views/Lanes.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| §5.x boards / lineage / bundle | ✅ LIVE | get_lanes, get_promotion_ledger, get_preset_lineage | — |
| §3 lane board (scaffold) | 📄 SPEC | hardcoded `lanes` list (live board is §5.1) | :411 |
### 19 Connections (`ui/views/ControlPlaneViews.kt`) — **Slice 5**
| Panel | Status | Tool | Ref |
|---|---|---|---|
| posCount / ordCount | 🐞 BUG | get_positions/get_open_orders cast as JsonArray (envelope is object) | :281 |
| Four-profile board / SYSTEM controls | 🔒 PEND | conn_profiles/conn_activate | :169,:453 |
### 20 MCP — ✅ LIVE (roster static; audit + proposals live)

---

## Global chrome (app-bar, every view — `ui/nav/MissionNav.kt`)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Stance strip PHASE/ENTRIES/MODE/PNL/POSITIONS/ALERTS/SERVICES | ✅ LIVE | get_system_overview | StanceStrip |

## SUITE — **Slice 3** (built from doc snapshot; per-symbol stays Fase-2)
| Panel | Status | Tool | Ref |
|---|---|---|---|
| Overview aggregate stance (bank rows / net R) + Priced-bank card | ✅ LIVE | get_bank_priced (overlay; snapshot fallback) | :122 |
| Overview per-symbol TRIAD-A / M-null tables | ⏳ DATABANK-F2 | get_shadow_bank (`AGG_ROWS` snapshot) | :78,:153 |
| Symbols directory + per-symbol detail | ⏳ DATABANK-F2 | get_shadow_bank/get_decision_census (`SYM_DIR`/`AGG_ROWS`) | :251 |
| Lab composer + per-symbol matrix | ⏳ DATABANK-F2 | get_shadow_bank (`SuiteMx`/`MX_JSON` snapshot) | SuiteData.kt |
| Lab / Tables **books scoreboard** | ⬜ TODO-F1 | get_books_scoreboard | :705,:824 |
| Lab SAVE → propose | ✅ LIVE (write) | propose_action | :566 |
| Tables saved experiments | ✅ LIVE (device) | LabStore (SharedPreferences) | — |
| Venue stance + verdict + summary | ✅ LIVE | get_venue_session (session/keys/order_id_map/reconciler) | :887 |
| Venue Open orders + Positions rows | ✅ LIVE | get_open_orders / get_positions (live MiniTable rows) | :989 |
| Venue Fills / SL-TP / Rejected-Canceled rows | 🔒 PEND | no per-row read (column shape + honest PEND) | :1010 |
