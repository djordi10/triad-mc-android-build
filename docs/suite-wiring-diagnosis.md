# SUITE wiring diagnosis + connect plan (LIVE-VERIFIED 2026-07-22)

Scope: the five SUITE views (21 Overview · 22 Symbols · 23 Lab · 24 Tables · 25 Venue),
`ui/views/SuiteViews.kt` + `ui/views/SuiteData.kt`.

All numbers below are from a **live MCP session on 2026-07-22** (the origin was 502 for a few
minutes, then the watchdog respawned it and I pulled real tool dumps). This replaces the
earlier snapshot guesswork.

Split the way you asked: what the app can connect now, and what has to be connected on your
side (one server env var), so the app diff stays small.

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
not a hang). Fix: `sshpass -p 2026 ssh liko@sshmac.transportech.ai`, `pgrep -fl triad-mcp`,
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
