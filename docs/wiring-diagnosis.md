# TRIAD Mission Control — live-wiring diagnosis + connect plan (LIVE-VERIFIED 2026-07-22)

> **STATUS UPDATE 2026-07-23 — READ SECTION 9 FIRST.** Much of the diagnosis below has since
> been FIXED and deployed: the DSN is set (bank tools live), the guard bug is patched, the bank
> tools are SQL-ified, the client timeout is raised, Lab saves are durable. Section 9 is the
> current state + the remaining work, with a clear owner split (app / MCP server / Mac box /
> engine). Sections 0-8 are kept as the diagnosis record.

This doc covers **live-data wiring across the whole app**. Sections 0-6 are the SUITE deep-dive
(where the work started); **section 7 is the app-wide inventory** of everything else that is not
yet wired, grouped by whose job it is to fix.

SUITE scope: the five SUITE views (21 Overview · 22 Symbols · 23 Lab · 24 Tables · 25 Venue),
`ui/views/SuiteViews.kt` + `ui/views/SuiteData.kt`.

All numbers below are from a **live MCP session on 2026-07-22** (the origin was 502 for a few
minutes, then the watchdog respawned it and I pulled real tool dumps). This replaces the
earlier snapshot guesswork.

The split is the same everywhere: what the app can connect now (app-side), what needs a server
tool built (backend), and what needs a transport switched on (infra), so each fix lands with the
smallest possible diff.

---

## 0. TL;DR — the one root cause

**`TRIAD_DATABANK_DSN` is unset (or not a sqlite DSN) on liko's Mac.** That single missing
config is what makes the whole "bank" half of SUITE read 0000 / dead:

```
get_bank_priced   → ok:false "transport: unavailable · local shadow bank (TRIAD_DATABANK_DSN unset or file missing)"
get_shadow_bank   → ok:false "transport: unavailable · local shadow bank (TRIAD_DATABANK_DSN is not a sqlite DSN)"
get_bank_dedup    → ok:false "transport: unavailable · local shadow bank (TRIAD_DATABANK_DSN unset or file missing)"
get_books_scoreboard → ok:true but data = {books:[B0,B1,M1,K1], status:"no local shadow bank",
                       note:"set a sqlite TRIAD_DATABANK_DSN and run triad-databank-resolve"}
```

This is the **Fase 2 databank blocker** already on record. It is a server-side fix, not app
wiring. Once the DSN is set and `triad-databank-resolve` runs, the priced-bank card, the
per-symbol tables, dedup, and the four-book scoreboard all light up with no app change.

**What is already LIVE and correct (needs no DSN):** the census / take-rate / P&L tools. So
the accepted-vs-rejected answer you want can be shown right now — it just isn't wired onto the
Overview yet.

---

## 1. Your questions, answered with live data

**"Yang di-accept berapa banyak?"** → **438 takes.** Confirmed two ways:
- `get_take_rate` → `by_verdict: { take: 438, wait: 6, skip: 831956 }`, `total: 832400`,
  `take_rate: 0.000526`, `in_band: false`.
- `get_decision_census` → the `take` reason row: `n: 438, pct: 0.0005, avg_conviction: 74.59`.

The Overview hardcodes `takes 109`. That is stale by 4x. Live is **438**.

**"Shadow yang di-reject juga ke-record?"** → No, they are cleanly separated at this layer.
The census `by_reason` splits all 832,400 decisions:

| reason | n | what it is |
|---|---|---|
| shed | 696,312 | dropped before model (backpressure) |
| timeout | 97,781 | gateway timeout (not a decision) |
| error | 24,784 | gateway error |
| model | 6,919 | model answered, did not take |
| matrix_off | 3,369 | matrix disabled |
| invalid_output | 2,676 | model answered, unparseable |
| **take** | **438** | **ACCEPTED** |
| judge_coerced | 115 | |
| wait | 6 | |

`model_actually_consulted` = 7,363 (take + a real model answer only). So "accepted" (take) is
438, distinct from the 831,956 skip/shed/reject. Nothing is double-counting takes here. The
*confusion* is that the Overview front page shows the **bank** tables (TRIAD-A rejected pool +
M-null control) which are currently dead (DSN), and hardcodes a stale `takes 109`, so you never
see the clean 438-vs-the-rest split that the census already has.

**"Kenapa 0000, harusnya ada isinya?"** → The bank tools are `transport: unavailable` because
`TRIAD_DATABANK_DSN` is unset (section 0). Not an app bug, not a gate bug — a missing DSN.

**"Make sure resolusinya jalan."** → Resolution IS running. `get_pnl_summary` returns real
closed outcomes right now: 6 groups, e.g. `BCH win_tp +2.77R`, `ETH loss_stop -11.4R (14)`,
`BTC loss_stop`; totalling ~22 resolved, net ≈ -11R, 2 wins. So the 438 takes are producing
resolved P&L. Only 22 closed so far, but the resolver is alive.

---

## 2. Real tool shapes (confirmed live — for wiring)

```
get_take_rate       { take_rate:Double, in_band:Bool, total:Int,
                      by_verdict:{ take:Int, wait:Int, skip:Int },
                      window:{ from, to, unit } }
get_decision_census { total:Int,
                      by_reason:[ { reason:String, n:Int, pct:Double,
                                    avg_conviction:Double, avg_latency_ms:Double,
                                    zero_input_hash:Int, validator_passed:Int, fabrications:Int } ],
                      model_actually_consulted:{ n:Int, pct:Double }, note:String }
get_pnl_summary     { groups:[ { symbol:String, label:String, count:Int,
                                 pnl_r_sum:Double, wins:Int } ] }
get_bank_priced     ok:false until TRIAD_DATABANK_DSN set  (shape when live: n, net_total_r,
                    net_expectancy, gross_expectancy, cost_r_per_trade, breakeven_roundtrip_bps,
                    median_stop_bps, cost_model.roundtrip_bps — already wired in SuiteViews)
get_shadow_bank     ok:false until DSN  (live: total, distinct_decisions, net_pnl_r, rows[])
get_bank_dedup      ok:false until DSN  (live: rows, distinct_decisions, inflation, by_book[], ...)
get_books_scoreboard books:[...] only until DSN  (live: books{ B0,B1,M1,K1 } per-book stats)
```

NOTE (out of SUITE scope, but real): `AnalyseViews.kt` parses `get_pnl_summary` with the wrong
keys — it reads `n / net_r / key|group`, but the live shape is `count / pnl_r_sum / symbol|label`.
That view's P&L subsection will mis-read. Flagging for a separate fix; not touching it here.

---

## 3. Per-view state (all five)

- **21 Overview** — WIRED (4A, commit `304fc94`): live accepted-vs-rest card + live-first stance
  + explicit "needs DSN" card. The two per-symbol bank tables stay snapshot (need the DSN, 4C).
- **22 Symbols** — WIRED (4A, commit `1057b35`): live 24h scan overlay (directory note, per-row
  tag, per-symbol "Live 24h scan" card). The profitability split stays snapshot (need the DSN).
- **23 Lab** — calc is snapshot-math from `MX` in `SuiteData.kt` by design. SAVE →
  `propose_action(kind=other, type:lab_save)` is correctly wired. Leave it.
- **24 Tables** — reads local `LabStore`. Correct.
- **25 Venue** — properly live-wired (`get_venue_session`, `get_open_orders`, `get_positions`).
  The reference for how the others should look.

---

## 4. The connect plan (split)

### 4A. App-side — DONE + verified live (no DSN needed)

**Overview 21** (commit `304fc94`) — new **"Accepted vs the rest"** card, live from tools that
work without the DSN. Verified live on emulator: 438 accepted, skip 831,956, resolution line.

- stance `takes` ← `get_take_rate.by_verdict.take` (live 438), snapshot fallback.
- stance `candidates` ← `get_decision_census.total` (live 832,400), snapshot fallback.
- card: accepted (take) vs the rest, from `get_decision_census.by_reason` (real `n` per reason);
  falls back to `get_take_rate.by_verdict` (take/wait/skip) so the split renders even when the
  census flaps to absent (see section 6).
- resolution line from `get_pnl_summary.groups` — n resolved, net R, wins — "resolver is running"
  when non-empty, honest "no closed outcomes this poll" when empty.
- `get_bank_priced == null` now renders an explicit "needs the databank DSN" card instead of
  vanishing, so the dead bank cards read as a known config gap, not a mystery 0000.

**Symbols 22** (commit `1057b35`) — added `get_scan_board`, the one genuinely per-symbol read
that works without the DSN. Verified live on emulator (note rendered "Live 24h feed: 0 of 31
emitting, feed absent").

- directory stance live-first: symbols / emitting 24h / screened from the board.
- directory note "Live 24h feed: N of M emitting" ("feed absent" when the market feed is down).
- per-symbol row: a "24h N" tag only when that symbol has fresh 24h candidates.
- per-symbol detail: a "Live 24h scan" card (candidates 24h / regime / spread / screen_reason).
- profitability split stays snapshot (needs DSN), labelled as such.

All additive + guarded (render only when the tool is present), so nothing blanks when a tool is
absent — it degrades to the snapshot. NOTE: because these are heavy live reads on a flapping
origin, the live overlays appear only on polls where the tool actually lands (section 6).

### 4B. Your side — one server env var (lights up the rest, zero app change)

On liko's Mac (the triad-mcp host):

```
set TRIAD_DATABANK_DSN=sqlite:////<path>/shadow_bank.sqlite   # a real sqlite DSN
run triad-databank-resolve                                     # populate/resolve the bank
```

That turns `get_bank_priced` / `get_shadow_bank` / `get_bank_dedup` / `get_books_scoreboard`
from `transport: unavailable` into live data, which the already-written Overview + (once wired)
Symbols cards will render. This is the Fase 2 databank item.

### 4C. Optional later (needs a tool that may not exist)

The per-symbol TRIAD-A / M-null tables (`AGG_ROWS`) are aggregate-snapshot. A live per-symbol
split needs either `get_bank_priced` accepting a `symbol` arg or a dedicated per-symbol tool.
If neither exists, the tables stay snapshot (honest) until the DSN is set and we check whether
`get_bank_priced` can key by symbol. Do NOT fabricate per-symbol live rows.

---

## 5. Infra note (the 502 you may hit again)

`triad-mc.bgzr.io/mcp` runs on liko's Mac Studio behind a cloudflared tunnel (`:8801`). A
Cloudflare 502 `origin_bad_gateway` = the mcp process hung (the pgrep watchdog catches a crash,
not a hang). Fix: SSH to the Mac (credentials off-repo), `pgrep -fl triad-mcp`,
`kill <PID>` — the keeper respawns it. Do NOT bounce the whole stack (live trading runs there).
During this work it flapped 502 twice and self-recovered within minutes each time; SSH to the
Mac also timed out during a 502 window (so a hard 502 may need liko, not just a kill).

---

## 6. Known problems / why a live panel sometimes "flips" to blank

This is expected behaviour, not a bug — worth knowing so a blank panel is not mistaken for lost
wiring. Every live card is guarded (`if (tool != null)`) and degrades to the snapshot / a "—"
when the tool does not answer that poll. So the same card can show live data one refresh and the
snapshot the next. Causes, in order of how often you will see them:

1. **Origin 502 flap.** When the mcp proc hangs, ALL live tools read absent for that window and
   every panel shows its snapshot / "—". Recovers on watchdog respawn (section 5).
2. **Heavy-tool flap (the big one for SUITE).** `get_scan_board` (a 45-symbol scan) and the bank
   reads are heavy. When they are one of ~5 sequential calls against a flapping origin, that one
   call can 502 while the light tools (census, take-rate, pnl) succeed. Result: on Symbols the
   directory shows snapshot rows with no "Live 24h feed" note until a poll where scan_board lands.
   `ToolsViewModel` gives a null result one quick retry (`RETRY_BUDGET = 3` per poll) to soften
   this, but a badly-flapping origin still misses some polls. Tap refresh a few times; it lands.
   Observed this session: curl (a fresh single-tool session) got scan_board 6/6, but the app (one
   persistent session, 5 sequential calls) missed several polls before it rendered. Not a code
   bug — the read pattern is the same one Venue and the Overview accepted card use successfully.
3. **DSN-gated tools never land** (not a flap): `get_bank_priced` / `get_shadow_bank` /
   `get_bank_dedup` return `transport: unavailable` every poll until `TRIAD_DATABANK_DSN` is set
   (section 4B). These read as their explicit "needs DSN" state, permanently, by design.
4. **feed_absent (upstream, not a flap).** When the market feed is down, `get_scan_board` still
   answers but every `cands_24h` is 0 and `screen_reason` is `feed_absent`. The live overlay then
   correctly says "0 of 31 emitting (feed absent)" — that IS the live truth, not a wiring miss.

Honest-data rule held throughout: nothing renders a fabricated number. A tool that is absent,
DSN-gated, or feed-absent shows its real absent/zero state, never a made-up value.

---

## 7. App-wide wiring inventory (beyond SUITE)

Everything else in the app that is not yet on live data, grouped by whose job it is. Same three
buckets as SUITE. **Only bucket A is an app-side gap** — B and C are honest states where the app
already shows the absence correctly (a PEND box or a "transport unavailable" line), so they are
not bugs, they are waiting on a server tool or a transport.

### A. Declared-but-unread — app-side, wire now (the SUITE-Overview pattern)

The tool IS polled by the view's `*_TOOLS` list, then never read in the composable, so its data
is fetched over the wire and thrown away. This is the exact gap that was just closed on SUITE
Overview. Closing it is a small, additive, guarded diff (render only when the tool answers).

| View | Tool declared-but-unread |
|------|--------------------------|
| Connections 19 (`ControlPlaneViews.kt`) | `get_config_preset`, `get_continuity` |
| Reader/Writer 05 (`ReaderWriter.kt`) | `get_feed_health`, `get_view_catalog` |

(SUITE's own leftovers in this bucket — `get_bank_dedup`, `get_shadow_bank`,
`get_books_scoreboard`, `get_detector_registry`, `get_validator_rejects`, `get_book_definitions` —
are declared for the Lab / bank context and light up with the DSN, so they are tracked under
section 4B, not here.)

### B. PEND — the server tool is not built (backend)

The app already renders an honest "PEND · NOT BUILT" box for these; nothing can wire them until
the server ships the tool.

| View | Tool (what it would show) |
|------|---------------------------|
| Overview 01 (`Overview.kt`) | `get_money_path`, `get_risk_envelope`, `get_truth_coverage` (the "PEND trio", 404 until built) |
| Strategy 08 (`Strategy.kt`) | `get_detector_split` (per-detector outcome split), `get_combo_registry` (per-combo win rate) |

Until these ship, Strategy's per-detector rows honestly borrow their win rate from TRIAD-A and
the combo rows read "NOT EMITTING / 0 rows / PEND", never a fabricated number.

### C. Transport-gated — a transport must be switched on (infra)

The tool exists but its underlying transport is off, so every read is `transport: unavailable`.
This is the same shape as SUITE's DSN gap: an infra switch on liko's side, zero app change.

| View | Reads | Transport needed |
|------|-------|------------------|
| Executor 02 + Ops 04 (`OperateViews.kt`) | latency budgets, lane headroom, watchdog stats, clock skew, exit-lane p99 | **Prometheus** |
| Overview 01 (`Overview.kt`) | NATS bus status | the NATS bus (does not exist on the diagram by design) |
| SUITE + Shadow/Books (`SuiteViews.kt`, `ModelLearnViews.kt`) | priced bank, shadow bank, dedup, book scoreboard | **`TRIAD_DATABANK_DSN`** (section 4B) |

### Priority summary

| Bucket | Nature | Owner | Action |
|--------|--------|-------|--------|
| **A** (4 tools) | fetched but not rendered | app | wire now, SUITE-Overview pattern |
| **B** (5 tools) | server tool not built | backend | build the tool; app already shows PEND |
| **C** (Prometheus · NATS · DSN) | transport off | infra / liko | switch the transport on; zero app change |

The one line that unblocks the most at once is still `TRIAD_DATABANK_DSN` (bucket C) — it lights
up the whole bank half of SUITE plus the Shadow/Books reads with no app change.

> **⚠ SEE SECTION 8 for the resolved picture.** Sections 0 / 1 / 3 / 4B say "set a sqlite
> `TRIAD_DATABANK_DSN`". After a read-only SSH recon that first looked like it disproved this
> (Postgres is healthy), reading the DEPLOYED server source resolved it: the bank tools ARE
> sqlite-only and `TRIAD_DATABANK_DSN` IS unset on the server — so those sections are
> **directionally correct**. But they are incomplete: the local sqlite is already populated
> (2.84M rows, bridge-fed from Postgres — no "resolve" step needed), and a separate guard-script
> bug was periodically destroying it. Section 8 has the full, verified picture.

---

## 8. RESOLVED — the data is present; the bank tools just point at an unset sqlite DSN (plus a guard bug)

Verified by read-only SSH into `Mac-Studio-liko.local` (via `ProxyCommand="cloudflared access ssh
--hostname %h" liko@sshmac.transportech.ai`, 2026-07-22) **plus reading the DEPLOYED server source**
(`~/triad/TriadLearning`, branch `feat/tg-daily-scoreboard`). The investigation flip-flopped once;
this is the landed conclusion.

**1. The bank data exists — in two places.** Postgres `triaddtbnk.databank_shadow_trades` (read via
`TRIAD_MCP_DATABANK_RO_DSN`, ~411k rows) is the source of truth, and a `pg_to_sqlite_bridge` feeds a
**local sqlite** `~/triad/databank/triad.db` that holds **2,839,451 rows** in `databank_shadow_trades`.
Nothing needs building; no data is missing.

**2. Which datastore the bank tools actually read (the crux).** In the deployed source
(`src/triad_core/mcp/families/shadow.py`), `get_bank_priced` / `get_shadow_bank` / `get_bank_dedup`
read the **local sqlite only**, via `_bank_con()` → `TRIAD_DATABANK_DSN`. There is **no Postgres
fallback** in these tools — the PG path (`TRIAD_MCP_DATABANK_RO_DSN`, line ~385) belongs to a
different tool, `get_l3_reconciliation`. (The earlier recon note "bank.py reads PG first" conflated
the two.) So the bank tools are dead for a simple reason: **`TRIAD_DATABANK_DSN` is unset on the
running `triad_core.mcp.server`**, so `_bank_con()` returns None and the tools answer
`unavailable · TRIAD_DATABANK_DSN unset or file missing` — even though the 2.84M-row sqlite is right
there. The launch chain (`~/triad/bin/triad_mcp_autoupdate.sh`) exports only PATH; no DSN is set.

**3. Two more factors that made it read dead (both now benign or fixed):**

- **Transient load / 502 flap.** When the Mac is under load (observed load avg 8.74 right after a
  reboot) the MCP proc is too swamped to answer heavy reads, so Cloudflare returns 502 and the
  bank panels read absent. Load-driven, not config.
- **A guard-script bug that turned a lock into "corruption" (the real culprit).** The live trading
  stack continuously writes the 2.8 GB local SQLite file `~/triad/databank/triad.db`, so
  `PRAGMA quick_check` transiently returns `Error: in prepare, database is locked (5)`. The cron
  guard `~/triad/bin/sqlite_guard.sh` captured that on **stderr** (`2>&1`) and mis-read the lock as
  corruption, then ran a destructive `.recover` on the 2.8 GB file — killing the writers, pegging
  CPU/IO, and causing the 502s. Log proof: two `CORRUPT (… database is locked (5)) — auto-recovering`
  events (2026-07-18, 2026-07-22), each followed by `RECOVERED ok` (it was never corrupt), plus four
  `triad.db.corrupt-*` scar files.

**4. Guard fix applied 2026-07-22 (user OK'd):** added a skip-branch to `sqlite_guard.sh` so a
`database is locked` / `database is busy` result is logged and skipped instead of triggering
`.recover`. Real-corruption recovery is untouched (lock = `SQLITE_BUSY`, a different error class
from `disk image is malformed`). Backup `sqlite_guard.sh.bak-1784741547`; `bash -n` + logic
dry-tested. It only reduces the guard's false-positive destructive recovery — no trading-stack
change. This is liko's LIVE box; never bounce the stack.

**5. To light up the SUITE bank panels (the one remaining step — deferred to 2026-07-23):** set
`TRIAD_DATABANK_DSN=sqlite:////Users/liko/triad/databank/triad.db` in the `triad_core.mcp.server`
launch environment (add the `export` in `~/triad/bin/triad_mcp_autoupdate.sh` before its relaunch,
or set it in the launchd plist) and let the `*/10` autoupdate cron pkill+relaunch pick it up. **No
data migration or `triad-databank-resolve` is needed** — the local sqlite is already bridge-fed
(2.84M rows). The guard fix (point 4) is what keeps that sqlite intact once it is the live read
target. This changes what the MCP serves and needs a server restart, so it is more consequential
than the guard patch: back up the launch config first, and ideally do it with liko's awareness.
Once set, `get_bank_priced` / `get_shadow_bank` / `get_bank_dedup` — and the SUITE Overview + Symbols
bank tables — light up with no app change.

---

## 9. CURRENT STATE (2026-07-23) — what landed, what remains, who owns it

### 9.1 Fixed + deployed since the diagnosis above

| Fix | Where | Detail |
|---|---|---|
| **DSN set — bank tools LIVE** | Mac (server env) | `TRIAD_DATABANK_DSN=sqlite:///$TRIAD/databank/triad.db` added to `~/triad/TriadEngine/deploy/macstudio/triad_mcp_keeper.sh` (the watchdog respawn path — it was missing there while present in `run_stack.sh`; the keeper's respawn used to silently drop it). `get_bank_priced` / `get_shadow_bank` / `get_bank_dedup` / `get_books_scoreboard` answer with real data now. Section 4B/8.5 is DONE. |
| **Guard bug patched** | Mac (`~/triad/bin/sqlite_guard.sh`) | `database is locked/busy` now logs + skips instead of triggering a destructive `.recover` on the live 2.8 GB sqlite (section 8.4). Verified over multiple cron ticks. |
| **sqlite WAL + indexes** | Mac (`~/triad/databank/triad.db`) | WAL journal mode + `idx_opened_at` + `idx_shadow_outcome` so `ORDER BY opened_at` reads go ~32s (tool_timeout) → ~6s. ⚠ OPEN: something on the box periodically wipes custom indexes and reverts WAL→delete (suspects: pg_to_sqlite_bridge recreate, `triad_mcp_maintenance.sh` 09:00 cron, ledger compact). Durable fix pending: `CREATE INDEX IF NOT EXISTS` + `PRAGMA journal_mode=WAL` in the writer's ensure-schema, or appended to the guard script. |
| **Bank tools SQL-ified** | MCP server (branch `sqlify-bank-tools`, cherry-picked on the Mac) | `get_bank_priced` + `get_bank_dedup` moved from fetch-all-rows-into-Python to SQL aggregates; `get_shadow_bank` gained `resolved_only:Bool` (row sample narrowed to win/loss/expired with `pnl_r`, bank totals stay whole-population). Heavy reads no longer time out. |
| **Client timeout 60s** | app (`LiveMcpClient.kt`, `fb52684`) | ktor/OkHttp default 10s read timeout silently nulled ~15s bank reads. Now 60s request/socket. Also dropped unrendered `get_bank_dedup` from the Overview poll (it cost ~85s of poll stall for a tool no card read). |
| **Lab saves durable** | app (`fbd2931`) | SAVE writes `propose_action(kind=other, type:lab_save)` and the books read BACK from the server proposals registry (merged with the device copy, `REG ✓` tag). No approval step — the proposals inbox is used as an append-only registry, disposition ignored. Survives reinstall. |
| **TradeLogs P&L parse** | app (`4e4fe76`) | `AnalyseViews.kt` now reads the live shape (`count`/`pnl_r_sum`/`symbol`+`label`) — the section-2 NOTE is fixed. |
| **Analytics workbench LIVE** | app (`bca3796`, `5194076`) | The workbench (equity curve, cohort chips, outcome mix) aggregates the live `get_shadow_bank` rowset (resolved rows, chronological), demo rowset only as fallback. |

### 9.2 Remaining work, by owner

**Ours — app** (small, additive, the SUITE-Overview pattern):
- Bucket A unchanged: Connections 19 `get_config_preset`/`get_continuity`; Reader/Writer 05
  `get_feed_health`/`get_view_catalog` — fetched but never rendered.

**Ours — MCP server** (deploy path proven: branch off TriadLearning main → cherry-pick on the
Mac → `pkill -f triad_core.mcp.server`, keeper respawns):
- `get_books_scoreboard` redesign — it fetches all rows AND trains a GBT per call; needs
  precompute/cache or SQL. Parked.
- `get_vr_scoreboard` — times out on big windows; same treatment. Parked.
- Bucket B tools if/when wanted: the Overview PEND trio, `get_detector_split`. NOTE:
  `get_combo_registry` should NOT be built as speced here — it is superseded by the upcoming
  SUITE experiment-registry design (per-coin config sets, forward clocks per cell).

**Ours — Mac box** (careful, live trading runs there):
- The index-wiper / WAL-reverter hunt + durable schema fix (see 9.1 row 3). This is the one
  fragile piece left under the bank tools.

**Liko — infra/engine** (the ONLY truly engine-side items):
- Prometheus transport for Executor 02 / Ops 04 latency+lane reads (bucket C).
- Market feed liveness (feed_absent windows on `get_scan_board`).
- Judge fan-out / structure-split facelift (WITH-LLM arm is data-sparse outside M1 until it lands).
- M-LIVE promote/routing consumption for the SUITE rebuild — contract to be agreed via his
  config doc (pending).
