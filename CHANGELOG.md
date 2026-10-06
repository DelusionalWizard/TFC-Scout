# Change history

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
