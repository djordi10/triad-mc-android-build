# TRIAD Mission Control — design polish backlog

Goal (user's words): **a human reading any card understands what they're looking at.** Write for people who
skim, not for an AI that reads long context. Distill to the intent; drop what doesn't help understanding.

Voice rules that carry: **no em-dash** in user-facing copy; plain language; jargon lives in a detail/why
section, not the headline.

Status: ⬜ todo · 🔨 in progress · ✅ done (verified on emulator)

---

## Batch 1 — structure & components (concrete asks, global, low-judgment)

| # | Item | Detail | Status |
|---|---|---|---|
| B1-1 | **Wire stamp colored bg** | Thread 3 OK, but give the box a soft fill by meaning: amber fill for WIRE PENDING, green fill for WIRED·LIVE. Keep dashed border on pending. | ✅ |
| B1-2 | **Section-header hierarchy** | Section titles (e.g. "01 · SIGNALS / The engine plane") are the PARENT of the cards under them, but read weaker than the dark card headers. Add contrast / restyle so the parent clearly outranks its child cards. | ✅ |
| B1-3 | **Stat row → card** | The TRIAD Analytics loose stat numbers (3,014 decisions · 2 takes · 8,008 bank rows · …) should be their own card, not floating text. | ✅ |
| B1-4 | **Kill the meta intro** | "TRIAD Analytics — Every card below shows a real number…" paragraph: if the numbers are real, don't explain that they're real. Remove it (and trim the WIRED/WIRE-PENDING legend note). | ✅ |
| B1-5 | **Shorten MUZZLED** | Even the distilled version is too long. Target: "N proposed, N blocked by safety checks. Good signals, but they break the risk rules." Core intent only. | ✅ |

## Batch 2 — thread 1 content sweep (the tricky, iterative one)

Distill prose across all views to plain, human-first sentences. **Case-by-case**: some prose should be cut
to the core, some just needs the jargon moved to a detail section, some paragraphs should be **removed
entirely** (like B1-4 — if a thing is self-evident, don't narrate it). Decide per card.

Cards/views still carrying long prose or em-dashes (non-exhaustive, fill in as swept):
- Overview (OPERATE): "The estate — 1 of 14 nodes…" prose + em-dashes ⬜
- Analytics remaining Notes (Latency+CAG, Conviction drift-tell, Payoff, workbench definition-law) ⬜
- Intelligence: funnel Note, validator prose ⬜
- Strategy, ModelLearn, Control, SUITE prose blocks ⬜
- Global em-dash cleanup in user-facing copy ⬜

## Batch 3 — thread 2 rollout (section labels inside multi-part cards) — pattern approved

`SectionLabel` component built + approved on Exec quality (THE NUMBERS / WHY IT'S EMPTY). Roll out to other
multi-part cards where the parts aren't obviously separate (image 6 "sole keyholder", image 7 cards, etc.). ⬜

---

## Components built (shared, in `ui/components/Components.kt`)

- `SectionLabel(label, divider)` — within-card section heading (hairline + mono eyebrow). Thread 2. ✅
- `WirePending(tool)` / wire stamp — de-emphasised amber/green status stamp. Thread 3. ✅ (colored fill = B1-1)
- `PlaneSection(...)` — parent-level section header with clear hierarchy. B1-2. ✅

## Reference patterns (already good, source of the direction)

- Topology card (`ControlPlaneViews.kt` ~1710): Pine banner + bold headline + `▸` mono bullets — the
  "distill to key facts, show visually" target for thread 1 where bullets fit.
- Topology private helpers promoted: `SrcHeading` → `SectionLabel`, `TopoPend` → `WirePending`.
