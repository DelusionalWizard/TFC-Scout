# Change history

> Older GitHub releases (0.2.1, 0.2.2, 0.2.3, 0.2.5 and 0.2.5 for 1.20.1) were deleted on 8 October 2026; only 0.2.6 remains published.

## 0.2.7 for Minecraft 1.20.1 / Forge (8 October 2026, not published)
- Idea taken from a review of the old TFC Seed Maker (GPLv3, TFC 1.7.10; no code copied, its crops item was never finished): two new optional wishlist checks, never part of the presets.
- **Crops:** counts the crops whose climate range (read from the loaded data, so addon crops count) contains the yearly average temperature at the final spawn. It needs at least 10 crops, or half of all loaded crops if fewer exist. The result row lists some of them.
- **Farmland moisture is not offered on this build:** TFC 3.2 farmland moisture comes only from nearby water, and the chunk data has no rain hydration. Use the River, Lake and Coast checks for irrigation.
- Both are checked in the real temporary world after the climate check, so a seed that fails them is dropped before any resource check.

## 0.2.6 for Minecraft 1.20.1 / Forge (8 October 2026, published as prerelease)
- Suggestions from a community comment: the single Freshwater check is gone. River, Lake and Coast or ocean are now separate optional wishlist checks, each with its own distance (River uses the old freshwater distance). The presets no longer require water; their weights were redistributed so the total is still 100.
- Confirming a river or lake needs a water source block in a river or lake biome; coast or ocean needs TFC salt water. Saved results and wishlists from older versions still load (a saved Freshwater requirement is dropped).
- The result screen's Spawn point row now also shows the trees at spawn: forest type and tree density as a percentage (TFC 3.2 chunk data).
- Release files (jar, checksums, notes) are no longer stored in Git; releases are on the GitHub Releases page only. Old release/ folders and one-off publish workflows were removed from the repository (the releases themselves are unchanged).
- Tests: 41 unit tests; real-install smoke with TFC alone passed, including a new check that River, Lake and Coast each confirm on real seeds (8 of 8 each).

## 0.2.5 for Minecraft 1.20.1 / Forge (branch forge-1.20.1, 7 October 2026, pushed to GitHub, prerelease v0.2.5-1.20.1)
- Port of 0.2.5 to Minecraft 1.20.1, Forge 47.4.x, TFC 3.2.25 and Java 17, aimed at the TerraFirmaGreg Modern pack. Same features as the 1.21.1 build.
- Scout now reads the world's own data: only ore veins the world can generate count, and what a vein provides comes from the blocks it places. Resources a world cannot generate are not required, with a notice. Plain TFC behaves as before.
- New optional resources: Iron and Coal.
- TerraFirmaGreg-Core (only when installed): Scout follows its changed world generation and manages its global world seed, and its Dream, Easy and Fair presets also require iron and coal. Searches in that pack are slower.
- Forest types are TFC 3.2's five types; tree cover 0-4 follows their order.
- Fixed while testing in a real install: mixin refmap, a hook that crashed TFC at startup, and a resource finder that used development-only method names. Screens now draw their own background on 1.20.1.
- Tests: 41 unit tests; real-install smoke suites with TFC alone and with the full TerraFirmaGreg Modern 0.13.10 pack. Not tested on a dedicated server.

## 0.2.5 picker fixes (7 October 2026, commit 1bb00de; included in the published 0.2.5 jar)
- The spawn-rock picker no longer offers slab, stairs or wall variants (only real rock types such as andesite, basalt, chalk).
- The spawn-biome picker offers only biomes the world's biome source can produce (125 tfc: biomes), not the vanilla ones that never generate in a TerraFirmaCraft world.
- Tests: 37 JUnit passed; native smoke passed with new rock/biome assertions. Rebuilt into the 0.2.5 jar (SHA-256 8ac547b013e1da95d550ca710152b1ba802a87c8f77faa8f913df5534bbfdd65), installed for testing, copied to Downloads and published as the v0.2.5 prerelease.

## 0.2.5 — 7 October 2026 (published as a prerelease; includes the 0.2.4 work, which was never published separately)

- Renamed the presets: God = Dream Start, Good = Easy Start, Average = Fair Start, Hard = Rugged Start, Super Hard = Wilderness Start. Config ids (`god`, `good`, ...) are unchanged. Saved seeds and reports from older versions still load (old names are accepted and shown with the new name).
- Results screen: every confirmed match from the search stays available after Stop or Pause, plus up to 12 close calls (seeds that missed exactly one check that only ran out of inspection budget). A close call has a Look harder button that re-checks it with a larger budget (96+ chunks per resource) through the same real-world checks; it can never be used unless every required check is confirmed. A result whose only open check was never reached is not offered as a close call.
- Check this seed (Options): type a seed number or text (parsed like Create World) and see how it scores against the current choices; confirmed results are saved. Look harder and Check refuse to run while a search is running, and a closer look refuses results found under different world settings.
- Options: search speed (Low = 1 scanner, Normal = configured, High = up to 8), stop after N matches (runs without pausing at each match) or N minutes (paused time not counted), match sound and toast on/off. Settings are remembered in `terrafirmascout/prefs.json`, including the last preset and minimum match/range per preset.
- Progress line: seeds per second, elapsed time (frozen when the search ends), skipped slow seeds. Copy seed and Copy report (plain text, locations only if revealed) buttons; Save as file keeps the JSON export.
- Saved seeds: sort by newest or best match, short notes (up to 60 characters, stored in `terrafirmascout/history-notes.json`), and two-step Delete.
- Early exit now waits for a second missed check before skipping the rest, so close calls are fully evaluated; the best-result and selection rules are unchanged.
- Pausing, Stopping and Paused states are shown immediately, Pause and Keep searching grey out while stopping, and the stop grace (5 s) now also applies to the verifier thread so Stop cannot wait indefinitely on an uninterruptible TFC chunk generation. Short log lines (search started/paused/resumed/stopped/finished, result picked) now go to the game log to make bug reports easier.
- Bug found by a new unit test and fixed before any release: a close-call check threw when the one open row was missing, which would have stopped a search with an error.
- Tests that ran (build copy): 36 JUnit tests (15 new); clean build; native smoke on the final tree three times (chunk hash unchanged, known fixtures unchanged); new dev-only checks for check-one-seed, closer look (a bigger budget confirmed one more check on the known Rugged Start seed and never lost one), stop after 2 matches (1 scanner, no pause), stop after 1 minute, layout without overlaps at 427x240, 480x270, 640x360 and 854x480 on four screens, and a button-press flow (Find, Pause, Stop, Results, clicking a result, Saved seeds, clicking a saved seed). Pause took effect in 0.1-0.8 s and Stop finished in 0.1-0.6 s even while a seed was being checked. One smoke launch crashed inside NeoForge's splash screen before any Scout code ran and passed on retry. Screenshots inspected.
- Source pushed to main as commit 170f238efde9ffe40dce3a186167a4737e29f6a0 (no release, no new workflow run). Installed in the authorized Prism instance on 7 October 2026 (0.2.4 backed up outside mods, installed hash verified, only Scout jar). Jar: build copy `build/libs/terrafirmascout-1.21.1-0.2.5.jar` (also in work/build-artifacts), SHA-256 243ca00ecbed1eeaa98f660ae83f43aeb9b93c3f3c60ef37ffd5e23d5aef636a; contains no development classes. No release folder or workflow was prepared for 0.2.5.
- Reported by Cooper while testing the installed 0.2.4: Pause/Stop did not seem to work and clicking a result seemed to start the search again. Could not reproduce either in the development client (see above). Cause unknown; 0.2.4 has no Results screen, so Cooper may mean Saved seeds. The log lines and Pausing/Stopping feedback above are meant to make the next report diagnosable. Not confirmed fixed.
- After the first 0.2.5 build (pushed as commit 4142551 and included in the rebuilt jar now installed and in Downloads): the search screen no longer keeps the stale line "Looking for a start that matches your choices." on screen after a search starts (the stage line already says what is happening). New dev-only screenshot tool client/DevelopmentSmokeTestShots.java (env SCOUT_SHOTS, window size via -PshotsWide in a temporary build.gradle edit that was reverted) plays a real session and saves 1920x1080 screenshots for the mod pages; excluded from release jars.
- Rebuilt and reinstalled on 7 October 2026 (12:17) so the installed jar includes the stale-message fix: clean build, 36 unit tests, native smoke and the button-press flow all passed on this tree; jar SHA-256 243ca00ecbed1eeaa98f660ae83f43aeb9b93c3f3c60ef37ffd5e23d5aef636a, no development classes. The first 0.2.5 build (SHA-256 9011f805550866b8b3d1647fb6d63df84640b6b16e561caada26e182e8206906) is backed up in work/previous-release/prism-instance. The jar in Downloads is the rebuilt one. The message fix is pushed as commit 4142551.
- Screenshots for the mod pages are in Downloads/TerraFirmaScout-screenshots (8 gallery images, 5 extras, captions.txt). All are real screens from the 0.2.5 development build; the match, results and saved-seeds images come from a real wishlist search that did not require a building or camp spot (3 confirmed matches, 12 close calls in 478 s). No preset has confirmed a seed yet, so none is shown as a match.
- Found while taking screenshots, not fixed: the wishlist editor's Rocks tab lists slab, stairs and wall variants as spawn-rock choices. Handed to a separate task.
## 0.2.4 — 6 October 2026 (installed for testing by Claude Code; not yet published)

- Release state: version bumped to 0.2.4 in build.gradle, neoforge.mods.toml and README. Clean build and 21 JUnit tests passed; jar contains ScanLimit and RiverBuildLimit and no development harness classes. Final native smoke on the 0.2.4 tree passed (chunk hash unchanged; slow-seed abandon 3,035 ms, cancel 2,029 ms, same spawn afterwards; all five grades stopped in 20.3-20.4 s). Jar: terrafirmascout-1.21.1-0.2.4.jar, SHA-256 07a9ec023e59d3bf63caeac8914962d93125797faf7687e2fcb57fcc76570339. Release files prepared in release/v0.2.4 and .github/workflows/publish-v0.2.4.yml. Installed in the authorized Prism instance (0.2.3 backed up outside mods, installed hash verified). Not committed, pushed or published until Cooper tests it. Before install the jar had only been tested through the development client.
- Source changed: search/ScoutSearchEngine.java (verifier thread and bounded queue), score/CandidateScorer.java (maxPossibleRank), search/SearchSession.java (bestRank), tfc/TFCFeatureProbe.java (bounded early exit), client/DevelopmentSmokeTest.java (dev-only thread-leak check), VerificationTest.java (2 new tests). Version still 0.2.3; no jar installed or published.
- Scanning no longer stops while a seed is verified. Remaining checks on a seed are skipped only when it cannot beat the best result already shown, so selectable results and the best card are unchanged. No old-mode setting.
- Tests that ran: 21 JUnit tests, native smoke/benchmark suite three times, memory regression, per-search thread-leak check. All passed. Seeds scanned per minute rose about 2-3x in single noisy samples. No confirmed seed in any grade, before or after.
- Stop no longer waits for stuck scan workers: slow seeds (about 2% of seeds, 40-270 s inside TFC's uninterruptible river generation during the spawn search) made Stop leave the screen locked for minutes. Stop now waits 5 s for scan workers, then reports finished; a straggler cleans its own caches when it returns. Changed ScoutSearchEngine.java and DevelopmentSmokeTest.java. 21 JUnit tests and the smoke suite passed; one organic straggler exited by itself after 202 s. Details and the remaining slow-seed capacity problem are in PROFILING.md.
- TFC-side cancellation: new mixin RiverBuildLimit plus tfc/ScanLimit.java. A Scout scan worker now abandons a seed after 20 s inside TFC's river generation (or immediately on Stop); other threads and normal TFC generation are unaffected. Changed ScoutSearchEngine.java, SearchSession.java (skippedSlow counter, pause handling), terrafirmascout.mixins.json, DevelopmentSmokeTest.java (opt-in slow-seed test, skipped count). 21 JUnit tests, smoke suites, memory regression, thread-leak check and the slow-seed abandon/cancel/same-spawn test passed; native chunk hash unchanged. Stops now take 20.2-20.4 s for 20 s budgets (was up to 270 s). Version still 0.2.3, no jar installed or published. Details in PROFILING.md.


## Profiling pass before 0.2.4 (6 October 2026, no code change)

- Measurement only; no source change, no new jar, nothing installed or published. Version remains 0.2.3.
- Profiled the scan and verification stages with temporary timing code in the build copy (reverted; patch and raw reports in work/profiling). Findings and candidate fixes are in PROFILING.md.
- Main result: verification on the coordinator thread idles scan workers for most of each minute, and every verified candidate in the run failed a required check (most often the building-site check). No candidate was confirmed in any grade during the 60 s runs.

## 0.2.3 — 6 October 2026

- Moved the Scout button into the Create World screen's World-tab grid. Replaced the global screen-init subscriber so the button follows tab visibility and normal layout instead of sitting over the footer on every tab.
- Added native menu checks for alignment, absence on Game/More, tab switching and opening the search screen.
- Added shared project status, Codex/Claude entry instructions and a local handoff to support alternating between tools without losing changes.
- All 19 unit tests and the menu/native-generation smoke check passed. Repeated native chunk hash stayed unchanged. Full preview-mod and dedicated-server combinations remain untested.

- Installed 0.2.3 in the authorized Prism instance, backed up 0.2.2 outside mods and verified matching checksums. Published v0.2.3; release/source commit 03293e7a80ff134ff43fd5bfbf24be248661b001; workflow 37441648308 succeeded. Jar SHA-256: f557b1862dba2ac6e24293a40b9e1a399b7949c423d22c92f947404ce6b12ca5.

## 0.2.2 — 6 October 2026

- Fixed optional rock variety reducing match scores and optional evidence affecting shortlist/best-result ordering.
- Scoring/ranking now consider required criteria only. Fully confirmed wishlists containing only zero-weight requirements can reach 100.
- Unused checks display “Not needed for this search”. Unrequested terrain, land coverage and rock-diversity sampling are skipped.
- Two regressions reproduced against 0.2.1; all 19 tests passed after fixing them. Native climate/biome wishlist at minimum score 100 passed without optional targets. Installed in Cooper's authorized Prism instance and published as v0.2.2.
- Source/release commit: b1ae0bd59fe2cea035b8044d1c49f9a858590e8b. Jar SHA-256: b443fba9558cb9a5c816c96a2c6d9f75229c2cf8409e0b5fe53335abf27abd3a.

## 0.2.1 — 6 October 2026

- Fixed native seed-cache retention through ThreadLocal callbacks. Cleared caches on owning threads, used owned scratch-generation workers and waited for worker shutdown.
- Focused 1,200-seed sampling held live heap stable, with no old region generators retained. Twenty-four scratch worlds released levels, generators and worker threads. These are not gameplay FPS measurements.
- All 16 tests then present and native generation smoke checks passed.
- Source commit e217d2e7c864c49ac02f19878ba4a71c5baaaec0; release commit 606d13c82e8168a13c41de85fd6409882e197662. Jar SHA-256: 9821eaad3f3fbdddd6f69217bd7f6abfc5d859753bff2f8f53e9b3ca748085e8.

## Earlier development

Renamed TFC GodSeeds to TerraFirmaScout, added preset research, player wishlist filters, saved seeds/reports, clearer wording and stricter independent terrain checks. Old apparent Hard/Super Hard positive fixtures failed the corrected decorated-ground check and must not be treated as confirmed current successes. See TEST-REPORT.md for historical details and limits.
