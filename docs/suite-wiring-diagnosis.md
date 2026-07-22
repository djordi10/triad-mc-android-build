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

- **21 Overview** — declares 4 tools, renders only `get_bank_priced` (dead: DSN). Headline
  `candidates 22,356` / `takes 109` + both symbol tables are frozen snapshot. `get_take_rate`,
  `get_pnl_summary`, `get_bank_dedup` are fetched then **never read**. Biggest gap. (fix 4A)
- **22 Symbols** — declares 4 tools, renders none. `SYM_DIR` + `AGG_ROWS` 100% snapshot.
- **23 Lab** — calc is snapshot-math from `MX` in `SuiteData.kt` by design. SAVE →
  `propose_action(kind=other, type:lab_save)` is correctly wired. Leave it.
- **24 Tables** — reads local `LabStore`. Correct.
- **25 Venue** — properly live-wired (`get_venue_session`, `get_open_orders`, `get_positions`).
  The reference for how the others should look.

---

## 4. The connect plan (split)

### 4A. App-side — connectable NOW (live, no DSN needed). This is the small diff I will make.

On Overview 21, add one honest **"Accepted lane"** card sourced from the tools that are already
live, and replace the stale hardcoded stance:

- stance `takes` ← `get_take_rate.by_verdict.take` (live 438), fallback snapshot.
- stance `candidates` ← `get_decision_census.total` (live 832,400), fallback snapshot.
- new card: accepted (take 438) vs the rest, from `get_decision_census.by_reason` (real `n`
  per reason) — the clean split above.
- resolution line: from `get_pnl_summary.groups` — n resolved, net R, wins — "the resolver is
  running" when non-empty, honest "no closed outcomes yet" when empty.
- keep the existing `get_bank_priced` card exactly as-is; it already degrades to absent when the
  DSN is unset. Add a one-line "bank tools need TRIAD_DATABANK_DSN" note so the dead cards read
  as a known config gap, not a mystery 0000.

All additive + guarded (render only when the tool is present), so nothing blanks when a tool is
absent. Field names are confirmed live, so this is screenshot-verifiable now.

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
Today it self-recovered within minutes.
