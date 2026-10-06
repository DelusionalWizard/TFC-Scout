# Current project status

Updated 6 October 2026 by Codex. Cooper alternates between Codex and Claude Code; use these records to continue the same project.

Current source/build/installed version: **0.2.3**. Target: Minecraft 1.21.1, NeoForge 21.1.234+, TFC **4.2.11 exactly**, Java 21.

## Latest change and verification

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

Next: address Cooper's next request; follow up on full preview/modpack UI and dedicated-server tests when in scope. Do not restart from an old archive. Record actual changed files, tests, installed hash and published references when handing between tools.
