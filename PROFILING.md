# Search profiling, 0.2.3 (6 October 2026, Claude Code)

Measurement only. No mod behavior changed and nothing was released. Timing instrumentation was applied to the build copy only; it is saved outside the repo as `work/profiling/profiling-instrumentation.patch` (with the raw per-grade reports) and was then reverted.

Method: `runSmokeClient -PbenchmarkAll=true -PsmokeSeconds=60 -PotherSeconds=60`, one run, default config (4 scan workers, 24 target chunks, 5 finalists). Times are thread-time summed across workers. Samples are small (3–5 verifications per grade), so treat the numbers as orders of magnitude, not benchmarks.

## Results

| Grade | Seeds tested in 60 s | Passed region prefilter | Passed detailed scan | Verified in scratch world | Wall time spent verifying | Confirmed |
|---|---|---|---|---|---|---|
| God | 91 | 15 | 5 | 4 | 35 s | 0 |
| Good | 33 | 14 | 5 | 3 | 42 s | 0 |
| Average | 10 | 8 | 8 | 3 | 43 s | 0 |
| Hard | 13 | 9 | 7 | 4 | 53 s | 0 |
| Super Hard | 17 | 14 | 13 | 5 | 40 s | 0 |

Throughput was 0.2–1.5 seeds/s, against the old 100 seeds/s goal.

Per-seed cost in the scan stage (each worker thread): `spawnBiome` search about 0.6–0.7 s, `RegionScanner.accepts` about 0.3–0.4 s, `DetailedScanner.scan` about 0.25–1.8 s (grows with the number of vein criteria). Adapter construction and cache release are negligible.

Per-candidate verification cost: 8–14 s on average. Natural spawn about 1.2 s, rescan at the real spawn 0.1–1 s, landscape 0–0.9 s, then criteria. The expensive criteria were:
- TERRAIN (building site): always a miss in all 14 verifications, 1.8–8.6 s each.
- TIN: 7.5–8 s per miss.
- STARTER_COPPER: 2–4 s.
- CLAY: up to 5 s on a miss.

## Findings

1. **Verification stalls scanning.** `ScoutSearchEngine.verifyBatch` runs on the coordinator thread. While it runs no new seeds are submitted, so scan workers sit idle for 35–53 of every 60 s. Scan-worker time actually used was about 85 s out of roughly 240 available (God).
2. **Verification is spent on candidates that cannot pass.** Every verified candidate in this run failed at least one required criterion, most often TERRAIN. The criteria loop keeps checking the remaining criteria after a required one has already missed.
3. **TERRAIN is the costly and nearly always failing check**, and it is only tested after chunk generation. The native height filler (already used for vein ranking) could pre-screen flat ground before any scratch-world generation.
4. **The scan-stage prefilter works.** It rejected 84% of God seeds, but the spawn search plus climate sample costs about 1 s per seed per thread and cannot be skipped, since it anchors everything else.

## Candidate fixes (not implemented)

Ordered by expected gain and risk:
- Overlap verification with scanning: a separate verifier thread with a small bounded candidate queue, since candidates retain a region generator (watch the 0.2.1 memory fixes).
- In `TFCFeatureProbe`, test criteria cheapest-first and put TERRAIN/OPEN_GROUND early. Stopping at the first missed required criterion would save most of the 8–14 s but would leave later rows as "Still needs checking" on near-miss results. That is a display tradeoff and needs a decision.
- Add a native-height flatness pre-screen for TERRAIN/OPEN_GROUND in `DetailedScanner`.
- Later: relational wishlist conditions, coarse spawn-anchored grid.

None of these may change scoring, let an unconfirmed requirement through, or alter native TFC generation.

## Limits of this measurement

One run; 3–5 verifications per grade; a 24-thread machine with 4 scan workers, so contention differs on other hardware. These are search-pipeline timings, not gameplay FPS. The Git checkout was clean and unchanged while profiling.

## Change made after profiling (unreleased, same day)

- **Verifier thread.** `ScoutSearchEngine` now verifies shortlisted seeds on a dedicated thread through a queue bounded by `finalists_per_batch`, so scanning continues while a seed is being checked. The closest-looking waiting seed is verified first. If the queue is full, scanning waits (memory stays bounded as before). Cancellation stays cooperative: the verifier is never interrupted inside a scratch world, and the search is not "finished" until scan workers and the verifier have stopped.
- **Bounded early exit.** In `TFCFeatureProbe`, after a required criterion is missed, remaining checks are skipped only when `CandidateScorer.maxPossibleRank` shows the seed cannot outrank the best result already shown. Confirmed matches, the "best so far" card, saved results and optional-check behavior are unchanged. No setting for the old mode was added.

Tests that actually ran (build copy, Git source copied over): 21 JUnit tests passed (19 old + 2 new for the bound); native smoke and the benchmark suite passed three times; memory regression passed (1,200 seeds, retained regions 0, heap 1,011 to 1,008 MiB, 24 scratch worlds retained 0/72, scratch threads 0 before and after); a new dev-only check that no Scout thread survives a finished search passed for all five grades. Known Good/Hard fixtures gave the same results as before (Not confirmed yet, fit 83 and 99).

Seeds scanned inside the time budget, before and after (one sample each, random seeds, so noisy):

| Grade | Before (60 s) | After, run 1 (60 s) | After, run 2 (60 s) | After, run 3 (20 s) |
|---|---|---|---|---|
| God | 91 | 149 | 202 | 73 |
| Good | 33 | 69 | 59 | 54 |
| Average | 10 | 19 | 36 | 17 |
| Hard | 13 | 28 | 26 | 20 |
| Super Hard | 17 | 28 | 27 | 14 |

Roughly 2-3x more seeds per minute. No grade confirmed a seed before or after, so this shows speed, not better matches.

**Open problem found: slow stop.** In several runs a search took 90-255 s to finish after being cancelled (budgets were 20-60 s). A thread dump showed a scan worker still inside TFC's native `RegionGenerator.createRegion` for ~100 s of CPU on a single seed. That native code has no checkpoint and cannot be interrupted, and the scan path was not changed. The old runs never hit it in 5 searches, but they scanned far fewer seeds, so it is probably an existing issue exposed by the higher throughput. That is inferred from the stack, not proven by running the old code at the same volume. Fixing it needs a decision, since native generation must not change (for example a per-seed work limit or abandoning the stuck worker).

## Slow stop: cause and fix (unreleased, same day)

**Cause.** A watchdog (temporary, build copy only, saved as `work/profiling/slow-seed-diagnostics.patch`) logged every scan running over 10 s. Four of about 200 scanned seeds (about 2%) were slow: 47, 220, 261 and 273 s, against about 0.6 s normally. All four were inside `TFCWorldgenAdapter.spawnBiome` → TFC `findSpawnBiome` → `RegionGenerator.createRegion` → `AddRiversAndLakes` → `River$Builder.intersectAny`: TFC's own river building. Replayed alone on one thread with no search engine, the same seeds took 0.8 s (control), 40.5 s and 250.5 s. So this is TFC's cost for those seeds, independent of the verifier thread, the early exit or contention, and the scan path is unchanged from 0.2.3. That native code has no checkpoint and cannot be interrupted.

**UI effect.** After Stop, Find/profile/custom stay disabled until the search reports finished, so a straggler locked the screen for minutes. Leaving the screen or using a result was never blocked.

**Fix.** On stop, `ScoutSearchEngine` waits up to 5 s for scan workers, then reports finished. A straggler is a daemon thread and releases its per-seed caches on its own thread in `scan()`'s `finally` when the native call returns. The verifier and scratch worlds are still always waited for. This relaxes the 0.2.1 rule that search completion waits for scanner workers; cache cleanup still happens on the owning thread, just later.

**Tests (build copy).** 21 JUnit tests passed. Smoke suite passed with 20 s budgets: God stopped after 25.3 s (20 s plus the 5 s grace) with one straggler that outlived the search by 202 s and then exited on its own (the harness now waits up to 15 min for scan threads to exit and fails if one never does); the other four grades stopped in 20.3-20.7 s. The straggler case occurred once in this run, so it is organic evidence rather than a deterministic test.

**Still open: slow seeds waste worker time.** At about 2% of seeds costing 40-270 s each, they plausibly use most of the scan time (estimated from the rates above, not measured). Options, not done: (A) a cancellation check in TFC's river builder that only fires on Scout's search threads, which touches TFC internals and needs care to keep generation output identical; (B) abandon a scan after about 10 s and start a replacement worker with a cap on stragglers, which leaves stuck threads burning CPU until they finish. The same code runs when TFC creates a world on such a seed, so those seeds may also be slow in play (inferred, untested).

## TFC-side cancellation check (unreleased, same day)

**What.** A new mixin, `RiverBuildLimit`, injects `ScanLimit.check()` at the start of TFC's `River.MultiParallelBuilder.intersectAny` (called once per candidate river edge, so the check is cheap next to the quadratic edge comparison). `ScanLimit` throws only on a Scout scan worker thread that opened a scope, and only when the search was cancelled or that one scan has run over 20 s (time spent paused is not counted). For every other thread, and when no search is running, it is a no-op (one volatile read), so TFC's normal generation is unchanged. An abandoned seed's generators are thrown away, nothing partial is cached (`createRegion` never reaches `cellCache.set`), and per-seed caches are still cleared on the same thread in `scan()`'s `finally`. A skipped seed counts as scanned and is tallied as `skippedSlow`. The mixin targets TFC 4.2.11 exactly, and `defaultRequire` makes a mismatch fail loudly. The verifier and scratch generation threads have no scope and are never affected.

**Tests (build copy).** 21 JUnit tests passed. Dev-only slow-seed test (seed 6289961475451757407, which takes about 42 s in the spawn search): abandoned after 3,045 ms with a 3 s limit; stopped by cancel after 2,024 ms; a fresh run without a scope afterwards gave the same spawn (1060, 0, -740) as before the change. The usual native feature-stage chunk hash is unchanged (57b1e018e18d91f8d087d97b7c597b3a0274b61c1f66fc225344cabf73fee242), and the known Good/Hard fixtures gave the same results. Smoke suite with 20 s budgets: every grade stopped in 20.2-20.4 s and the whole suite took 150 s (it took about 600 s with the stragglers). With 60 s budgets every grade stopped in 60.2-60.4 s (earlier runs took 90-270 s) with 4 slow seeds skipped (God 2, Good 1, Super Hard 1). Memory regression passed again (1,200 seeds, retained regions 0, heap 1,011 to 1,009 MiB, 0/72 scratch levels retained, scratch threads 0 before and after). The thread-leak check passed for all grades.

**Effect on speed.** Seeds scanned in 60 s: God 179, Good 79, Average 26, Hard 26, Super Hard 24. These are in the same range as the earlier runs (God 149-202), so no clear extra speed-up shows in 60 s samples; slow seeds now free their worker after 20 s instead of holding it for minutes, which should matter more in long searches (not measured). The 5 s stop grace stays as a fallback for any other uninterruptible call.

**Risks and limits.** A 20 s limit could skip a legitimate seed on a very slow or heavily loaded machine; normal scans take about a second here, so there is a wide margin, but it is not tested on weak hardware. Skipping slow seeds slightly biases sampling away from river-heavy regions, which does not affect confirmed results. The change touches a TFC class, so any TFC update needs the mixin rechecked. In-game behavior with the full modpack, dedicated servers and the preview mod is still untested.
