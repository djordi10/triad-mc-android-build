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

## Batch 2 — thread 1 content sweep — ✅ DONE (all 11 view files)

Distilled prose + removed every user-facing em-dash across all views, one file at a time (build + spot-verify
+ commit per file/wave). Voice: plain human-first sentence, jargon relocated or dropped, self-evident asides
deleted; conservative on the forensic model/control prose (punctuation fixed, wording kept). Commits:
`f0e7ae8` AnalyseViews, `2e4e410` ModelLearnViews, `def3e9a` OperateViews, `0f9769f` wave-2 (the other 8).

**Intentionally KEPT with em-dashes** (verbatim artifacts, not prose): §16.6 verbatim quotes + get_alerts
quote (ControlViews), the PEND build-spec dumps (ControlPlaneViews/Strategy/PromptStudio), and prompt-template
body text (PromptStudio). All other user-facing prose em-dashes are gone; remaining `—` in the tree are code
comments and `"—"` null-placeholders.

## Batch 3 — thread 2 SectionLabel rollout — ✅ DONE

`SectionLabel` rolled out to multi-part cards across every view (data group / meaning split, contextual first
labels + "what it means"). ~150 labels total. Exceptions by design: Topology uses its own `SrcHeading`,
OperateViews uses `ExecCard` (self-segments), ReaderWriter has a local `SectionLabel(text)` composable.

## Follow-ups / open

- Density check: thread-2 labels are applied to every data+explanation card (~19 in Analytics alone). If it
  reads too busy on review, dial back to only the genuinely-ambiguous cards (cheap: delete SectionLabel lines).
- The kept-verbatim em-dashes above: convert if a strict zero-em-dash policy is wanted.

---

## Components built (shared, in `ui/components/Components.kt`)

- `SectionLabel(label, divider)` — within-card section heading (hairline + mono eyebrow). Thread 2. ✅
- `WirePending(tool)` / wire stamp — de-emphasised amber/green status stamp. Thread 3. ✅ (colored fill = B1-1)
- `PlaneSection(...)` — parent-level section header with clear hierarchy. B1-2. ✅

## Reference patterns (already good, source of the direction)

- Topology card (`ControlPlaneViews.kt` ~1710): Pine banner + bold headline + `▸` mono bullets — the
  "distill to key facts, show visually" target for thread 1 where bullets fit.
- Topology private helpers promoted: `SrcHeading` → `SectionLabel`, `TopoPend` → `WirePending`.
