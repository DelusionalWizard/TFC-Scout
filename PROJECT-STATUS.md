# Current project status

Updated 7 October 2026 by Claude Code. Cooper alternates between Codex and Claude Code; use these records to continue the same project.

Source/build version: **0.2.5 (installed in Cooper's Prism instance for testing; source committed and pushed to main on 7 October 2026; no GitHub release published)**. 0.2.4 was replaced by 0.2.5 there (0.2.4 is unpublished; its release files remain in release/v0.2.4 and the jar is backed up in work/previous-release/prism-instance). Last published release: **0.2.3**. Target: Minecraft 1.21.1, NeoForge 21.1.234+, TFC **4.2.11 exactly**, Java 21.

## Release candidate: 0.2.5 (quality of life, new preset names), installed for testing

Preset renames (Dream/Easy/Fair/Rugged/Wilderness Start, config ids unchanged, old names still load), Results screen with matches and close calls plus Look harder, Check this seed, Options (speed, stop after matches or minutes, sound), seeds per second and elapsed time, Copy seed/report, Saved seeds sorting/notes/delete, remembered settings, Pausing/Stopping feedback, stop grace for the verifier, game-log lines for search actions. Details, tests and numbers are in CHANGELOG.md. 36 JUnit tests and the native smoke passed; the jar (SHA-256 9011f805550866b8b3d1647fb6d63df84640b6b16e561caada26e182e8206906, build copy and work/build-artifacts) contains no development classes. Installed (7 October 2026) in Cooper's authorized First Steps Prism instance: the 0.2.4 jar was backed up (hash verified) to work/previous-release/prism-instance, the installed 0.2.5 SHA-256 was verified against the tested jar, it is the only Scout jar in mods, and nothing else in the instance was touched (Minecraft was not running). Cooper must restart Minecraft to load it. No release folder or workflow exists for 0.2.5; publishing is waiting for Cooper's testing.

**Open bug report from Cooper (testing 0.2.4):** Pause and Stop seemed not to work, and clicking a result seemed to start the search again. Not reproduced in the development client: button presses (Find, Pause, Stop, Results, a result, Saved seeds, a saved seed) behaved correctly, and Pause/Stop acted in 0.1-0.8 s even during a check. 0.2.4 has no Results screen (it has Saved seeds). Cause unknown. If it recurs, read the instance's latest.log for lines starting "Scout:" (0.2.5 only) and ask Cooper exactly which button was pressed and what the screen showed. Not confirmed fixed.


## Release 0.2.4: superseded by 0.2.5 in the Prism instance; release files kept local and deliberately not pushed (the workflow would publish it); unpublished

Faster, quicker-stopping search: a verifier thread so scanning continues during checks, bounded early exit that cannot change shown or selectable results, a 5 s stop grace for scan workers, and a mixin (RiverBuildLimit + ScanLimit) that lets a scan worker abandon a seed after 20 s inside TFC's river generation or at once on Stop. Details, measurements and limits are in PROFILING.md and CHANGELOG.md.

Verified: clean build; 21 JUnit tests; native smoke on the final tree (chunk hash unchanged, known fixtures unchanged, slow-seed abandon/cancel/same-spawn test, all grades stopped in 20.3-20.4 s on 20 s budgets); memory regression; thread-leak check. Tested through the development client only: the jar has not been loaded by the launcher, and no real modpack, dedicated server, preview mod or weak hardware was tried.

Jar: release/v0.2.4/terrafirmascout-1.21.1-0.2.4.jar (jar excludes the development harness)
SHA-256: 07a9ec023e59d3bf63caeac8914962d93125797faf7687e2fcb57fcc76570339
Prepared: release/v0.2.4 (jar, SHA256SUMS.txt, NOTES.md) and .github/workflows/publish-v0.2.4.yml.
Installed (6 October 2026) in Cooper's authorized First Steps Prism instance: the 0.2.3 jar was backed up (hash verified) to work/previous-release/prism-instance, 0.2.4 was copied in and its SHA-256 verified against the tested jar, and it is the only Scout jar in mods. Other mods, saves and settings were not touched, and Minecraft was not running. Cooper must restart Minecraft and test it before publishing.
To finish after Cooper confirms: commit, push through the authorized GitHub connector, verify the workflow run and uploaded asset digest.

## Last published release: 0.2.3




Scout now lives in the World tab's grid, aligned with the world controls. It is absent on Game/More and opens search when clicked. All 19 unit tests and native menu/generation smoke checks passed. Screenshots were inspected. Native repeated chunk SHA-256 stayed 57b1e018e18d91f8d087d97b7c597b3a0274b61c1f66fc225344cabf73fee242. The previous optional-check and memory fixes remain included.

Installed in Cooper's authorized First Steps Prism instance; old 0.2.2 jar backed up outside mods. Only 0.2.3 is installed. Restart Minecraft to load it.

Jar: terrafirmascout-1.21.1-0.2.3.jar
SHA-256: f557b1862dba2ac6e24293a40b9e1a399b7949c423d22c92f947404ce6b12ca5

GitHub: https://github.com/DelusionalWizard/TFC-Scout
0.2.3 publication verified: https://github.com/DelusionalWizard/TFC-Scout/releases/tag/v0.2.3
Release/source commit: 03293e7a80ff134ff43fd5bfbf24be248661b001. GitHub Actions run 37441648308 completed successfully; uploaded jar digest matches the installed/tested build.

## Working locations

Git checkout: outputs/TerraFirmaScout-GitHub. Deliverable source: outputs/TerraFirmaScout. Active build/dev instance: work/TerraFirmaScout-build. Absolute paths and setup are in local outputs/Claude-Code-handoff.md. Read AGENTS.md and CHANGELOG.md before work. Keep records and source copies current after every session.

## Remaining limits and next work

Full preview/modpack UI combinations and dedicated-server joining/startup remain untested. No fully confirmed God result or 100-seeds/second goal has been demonstrated. Old Hard/Super Hard accepted fixtures were invalidated by the corrected terrain checks. Zero-second smoke searches are regression checks, not throughput measurements. Focused memory checks are not gameplay FPS proof.

Remaining ideas, not done: native-height flatness pre-screen for TERRAIN/OPEN_GROUND, criteria reorder, relational wishlist conditions. The mixin into TFC River.MultiParallelBuilder needs rechecking whenever TFC is updated. A 20 s per-seed limit could skip legitimate seeds on very slow machines (untested). No confirmed seed has been found in any grade in the profiling runs.

Next: address Cooper's next request; follow up on full preview/modpack UI and dedicated-server tests when in scope. Do not restart from an old archive. Record actual changed files, tests, installed hash and published references when handing between tools.
