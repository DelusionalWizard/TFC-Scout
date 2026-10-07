TerraFirmaScout 0.2.5 adds a results screen, faster and stoppable searches, new preset names and several quality-of-life options. It also includes the unreleased 0.2.4 speed and Stop work.

- **Presets renamed:** Dream, Easy, Fair, Rugged and Wilderness Start. Saved seeds and reports from older versions still load.
- **Results screen:** every confirmed match stays available after Pause or Stop, plus up to 12 close calls (seeds that missed exactly one check) with a Look harder button. A seed is never offered as a match unless every required check is confirmed.
- **Check this seed:** type a seed number or text and see how it scores against your choices.
- **Options:** search speed, stop after N matches or N minutes, match sound and toast, remembered settings.
- **Progress and sharing:** seeds per second and elapsed time, Copy seed, Copy report. Saved seeds can be sorted, given short notes and deleted.
- **Faster search:** seeds are scanned while a promising one is checked in a temporary world, and checks that cannot change the result are skipped.
- **Prompt Stop:** a seed that makes TFC river generation run for minutes is skipped after 20 seconds. This adds a small check to TFC's river builder that only affects Scout's search threads; normal world generation is unchanged.
- **Pickers:** the spawn-rock list no longer shows slab, stairs or wall variants, and the spawn-biome list shows only biomes a TFC world can produce.

Tested: 37 unit tests, native Minecraft menu/generation checks, a button-press flow, layout checks at four window sizes, and memory/thread cleanup checks, all in a development run. A user report that Pause/Stop seemed unresponsive and that clicking a result restarted the search could not be reproduced and is not confirmed fixed.

Requires Minecraft 1.21.1, NeoForge 21.1.234+, TFC 4.2.11 and Java 21. Replace older Scout jars and restart Minecraft. Experimental prerelease; full modpack/preview combinations, dedicated-server compatibility and very slow computers remain untested.
