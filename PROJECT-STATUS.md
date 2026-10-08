# Current project status

Updated 7 October 2026 by Claude Code. **0.2.5 published as a GitHub prerelease v0.2.5 (jar SHA-256 8ac547b013e1da95d550ca710152b1ba802a87c8f77faa8f913df5534bbfdd65, verified against the uploaded asset digest; release/v0.2.5 + publish-v0.2.5.yml, commit 68d48c6). The picker fixes (commit 1bb00de) are in that jar. 0.2.4 remains unpublished and its files stay untracked.** Cooper alternates between Codex and Claude Code; use these records to continue the same project.

**Related project (7 October 2026, Claude Code): VanillaScout 0.1.0, a separate vanilla-Minecraft seed finder, lives in outputs/VanillaScout with its own PROJECT-STATUS.md, CHANGELOG.md and local git repo (no remote). Built, tested (11 JUnit, dev-client walkthrough, 7 real worlds); not installed or published. Nothing in this TFC repo changed for it.**

Source/build version: **0.2.5 (installed in Cooper's Prism instance for testing; source pushed to main on 7 October 2026 (commits 170f238, f94b60c and 4142551, the last containing the stale-message fix and matching the installed jar); no GitHub release published, no release folder or workflow for 0.2.5, and the 0.2.4 release files were deliberately not pushed)**. 0.2.4 was replaced by 0.2.5 there (0.2.4 is unpublished; its release files remain in release/v0.2.4 and the jar is backed up in work/previous-release/prism-instance). Last published release: **0.2.3**. Target: Minecraft 1.21.1, NeoForge 21.1.234+, TFC **4.2.11 exactly**, Java 21.

**1.0.0 candidate (8 October 2026, Claude Code, pushed and published as GitHub release (not a prerelease)): result of the audit of the whole mod; see CHANGELOG and TEST-REPORT. 39 JUnit tests passed; development-client smoke (benchmarkAll) passed. Release jar terrafirmascout-1.21.1-1.0.0.jar, SHA-256 e5a281a647d8349d1ba7a2c1345c061d5fe8de3e3242c4db4de47e288c6d7857, no development classes, built but NOT launched in a real instance (Cooper's Prism instance is still running the 0.2.8 jar).**

**0.2.9 (8 October 2026, Claude Code, pushed and published as GitHub prerelease): Animals that can spawn wishlist check (see CHANGELOG). Tests that ran: 37 JUnit passed; development-client smoke with benchmarkAll passed (Animals 8/8 seeds, other checks as in 0.2.7/0.2.8). Release jar terrafirmascout-1.21.1-0.2.9.jar built (SHA-256 88e2981eef484fa40e97f22d161ae16ab0e74f2350369712fff3f7f54710252c), contains no development classes, not launched, not installed. Note: Cooper's Prism instance (TerraFirmaCraft-First-Steps-0.1.1-Prism) is running the 0.2.8 release jar and a Scout search was run there by hand (log shows Scout chunk-generator threads working, no Scout exceptions; the only errors are vanilla "Detected setBlock in a far chunk" and World Preview TFC lines).**

**0.2.8 (8 October 2026, Claude Code, pushed and published as GitHub prerelease): animal catalog groundwork only (no behaviour change). Tests that ran: 37 JUnit passed; development-client smoke with benchmarkAll passed (animal catalog: 47 animals across 116 biomes; water, crops and farmland checks unchanged). The release jar terrafirmascout-1.21.1-0.2.8.jar (SHA-256 ed6a207c41a24bc134cfe6f35c796a3e1755698a398524f5f6a9ef5a7872e155) was installed in Cooper's Prism instance (TerraFirmaCraft-First-Steps-0.1.1-Prism; the 0.2.5 jar was moved to work/previous-release/prism-instance) and the game launched to the title screen with the mod loaded and no errors in the log; the Scout screens were not clicked through by hand. Prism corrected terrafirmascout-common.toml on first start (new distance keys, old freshwater key dropped).**

**0.2.7 (8 October 2026, Claude Code, pushed and published as GitHub prerelease): Crops and Farmland moisture wishlist checks added (see CHANGELOG). Tests that ran: 37 JUnit passed; development-client smoke with benchmarkAll passed, including real-seed checks (Crops 8/8 seeds confirmed, Farmland moisture 2/8). Not built as a release jar yet.**

**Older GitHub releases deleted (8 October 2026, at Cooper's request): v0.2.1, v0.2.2, v0.2.3, v0.2.5 and v0.2.5-1.20.1, with their tags. Later also deleted: v0.2.6, v0.2.7 and their 1.20.1 releases. Also deleted later: v0.2.8 and v0.2.8-1.20.1. Only v0.2.9 and v0.2.9-1.20.1 remain on GitHub; older release descriptions in these records refer to deleted releases. Branches and source history are unchanged.**

**0.2.6 (8 October 2026, Claude Code, pushed and published as GitHub prerelease v0.2.6 (jar digest verified)): source/build version in the 1.21.1 line. Freshwater check removed; River, Lake and Coast or ocean added as separate optional wishlist checks (presets no longer require water, weights rebalanced to 100); trees at spawn (forest type and density 0-4) shown on the Spawn point row; release/ folders and publish-v*.yml no longer tracked (releases live on GitHub Releases; local copies remain and are git-ignored); chunk-generator threads get Scout's class loader as context loader. Tests that ran: 37 JUnit passed; development-client smoke with benchmarkAll passed, including new real-seed checks (River 8/8, Lake 8/8, Coast 8/8 seeds confirmed). Jar built, not installed in Prism, not published: terrafirmascout-1.21.1-0.2.6.jar, 183,505-byte class, SHA-256 c06d89fbaa80cb204f1777f18dfea748ec6808dbf5db707ef8ce85546af4d3ac (final build after the class loader change; the smoke ran on the same sources except that one-line thread change).**

## Release candidate: 0.2.5 (quality of life, new preset names), installed for testing

Preset renames (Dream/Easy/Fair/Rugged/Wilderness Start, config ids unchanged, old names still load), Results screen with matches and close calls plus Look harder, Check this seed, Options (speed, stop after matches or minutes, sound), seeds per second and elapsed time, Copy seed/report, Saved seeds sorting/notes/delete, remembered settings, Pausing/Stopping feedback, stop grace for the verifier, game-log lines for search actions. Details, tests and numbers are in CHANGELOG.md. 36 JUnit tests and the native smoke passed; the jar, rebuilt on 7 October 2026 after the stale-message fix (SHA-256 243ca00ecbed1eeaa98f660ae83f43aeb9b93c3f3c60ef37ffd5e23d5aef636a, build copy and work/build-artifacts) contains no development classes. Installed (7 October 2026) in Cooper's authorized First Steps Prism instance: the 0.2.4 jar was backed up (hash verified) to work/previous-release/prism-instance, the installed 0.2.5 SHA-256 was verified against the tested jar, it is the only Scout jar in mods, and nothing else in the instance was touched (Minecraft was not running). Cooper must restart Minecraft to load it. No release folder or workflow exists for 0.2.5; publishing is waiting for Cooper's testing.

**Picker fixes after the installed 0.2.5 jar (7 October 2026, uncommitted in Git, published nowhere; built into a rebuilt 0.2.5 jar, SHA-256 8ac547b013e1da95d550ca710152b1ba802a87c8f77faa8f913df5534bbfdd65, no development classes, installed in Cooper's Prism instance and copied to Downloads with the hash verified; the previous jar 243ca00e... is backed up in work/previous-release/prism-instance):** the spawn-rock picker now hides slab/stairs/wall variants (profile/RockChoices.java) and the biome picker offers only biomes the world's biome source can produce (tfc/BiomeChoices.java; 125 tfc: biomes of 189 registered, no vanilla). Search, scoring and world generation unchanged. Tests run: 37 JUnit (16+7+14) and the native smoke (including new spawn-rock and biome-choice assertions) passed. Cooper must restart Minecraft to load it.

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
