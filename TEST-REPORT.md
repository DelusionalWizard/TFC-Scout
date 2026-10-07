# Test notes - 6 October 2026

> Preset names changed in 0.2.5: God = Dream Start, Good = Easy Start, Average = Fair Start, Hard = Rugged Start, Super Hard = Wilderness Start. Older entries below keep the old names. The preset ids in the config file (`god`, `good`, `average`, `hard`, `super_hard`) did not change.



Tests ran in separate development instances on Windows 11, Java 21.0.7, Minecraft 1.21.1, NeoForge 21.1.234, TFC 4.2.11 and Patchouli 1.21.1-93-NEOFORGE.



## Build and automated checks



`gradlew.bat test build` passed for 0.2.1: 16 tests, no failures or errors. They check the score weights, required-resource gates, fingerprint matching, profile limits, cancellation, biome ANY/ALL choices, spawn filters, wishlist ranges and saved-report round trips. An unconfirmed requirement still blocks selection even when it adds no points to the match score.



The final Minecraft run also passed. It opened the TFC world-creation screen and rendered all six wishlist tabs. The revised wording and layout were checked in the screenshots. A real wishlist match was saved and loaded again; its exported report omitted coordinates. Native spawn biome, rock, forest type, tree cover and height filters passed for matching choices, and a deliberately wrong spawn biome was rejected.



## Native generation checks



Seed `123456789` gave equal spawn-biome locations and 289 equal biome/rainfall/rock samples in separate seed-local adapters. Real generated chunk climate matched the adapter. Two separate native FEATURES chunks had this same block-state SHA-256:



`57b1e018e18d91f8d087d97b7c597b3a0274b61c1f66fc225344cabf73fee242`



The final wording/filter check used zero-second search budgets. That checks loading, filters, history and clean cancellation; it is not a seed-search benchmark.



## All five presets



The final search benchmark after the decorated-ground fix gave each preset 60 seconds, with the default 24 target chunks per required resource:



| Starting style | Seeds tried | Region shortlist | Detailed shortlist | Fully confirmed matches | Best provisional score |

|---|---:|---:|---:|---:|---:|

| God | 67 | 11 | 5 | 0 | 82 |

| Good | 34 | 16 | 5 | 0 | 84 |

| Average | 12 | 9 | 7 | 0 | 63 |

| Hard | 10 | 9 | 8 | 0 | 88 |

| Super Hard | 9 | 8 | 8 | 0 | 88 |



The requirements were kept as configured. These are small random-seed samples, not a measured success rate. A missed match can mean a resource or open patch was outside the inspection budget. All incomplete results remained unavailable for selection.



Earlier runs found positive starter copper, copper veins, flux, tin, graphite, kaolin, freshwater and clay blocks. One earlier run appeared to find Hard and Super Hard matches in about 21 and 20 seconds. The independent terrain check below showed why those earlier results cannot be counted as current acceptance evidence. The newer check is stricter and corrects that problem.



## Creating worlds without Scout



A separate QA project loaded Minecraft, NeoForge, TFC, Patchouli and a development inspection helper. TerraFirmaScout was absent, including its classes and mixins. The helper created two normal saved worlds using Minecraft's world-creation flow.



A climate-plus-spawn-biome wishlist reproduced its natural spawn, biome and yearly climate in both worlds. The later repeat also gave equal runtime spawn-chunk hashes. An earlier runtime hash comparison differed after world startup; that difference was not fully diagnosed. Runtime hashes are therefore kept as diagnostics, not a claim that ticking worlds always stay identical.



The Hard fixture reproduced the reported water, log, clay and copper-vein blocks. Its terrain check exposed ground beneath a generated pine branch being treated as an open building site. Scout now reads a private heightmap made from the decorated blocks and checks three blocks of clear headroom. It leaves the chunk's generation maps untouched. The old fixture no longer passes all of its requirements, so its previous confirmed status is not valid for the corrected build.



The independent helper source is included separately so these checks can be repeated. It is a development tool, not a gameplay mod or a general validator for every wishlist.



## Still to prove



A fully confirmed God seed and reproduction of every God resource in normal worlds without Scout remain outstanding. Broader modpack compatibility has not been tested. The original goals of 100 seeds per second and a usual 30-90-second God result have not been reached. This remains an experimental build, with honest unconfirmed results instead of relaxed requirements.



The source registers no generation content, moves no spawn and applies only a seed number. Those design choices support unchanged worlds, but do not replace the remaining end-to-end tests.



## Memory retention fix in 0.2.1



A focused regression reproduced the old retention behavior by disabling cleanup on two persistent scanner threads. Each seed queried native biome, rainfall and rock data at nine positions. Weak references tracked the seed-local RegionGenerator objects; heap readings followed explicit GC in the development harness only.



| Run | Seeds sampled | Region generators still retained | Heap after GC |

|---|---:|---:|---:|

| Baseline, cleanup disabled | 100 | 100 | 2,404 MiB |

| Fixed, early checkpoint | 100 | 0 | 1,014 MiB |

| Fixed, reported slowdown point | 1,000 | 0 | 1,015 MiB |

| Fixed, final checkpoint | 1,200 | 0 | 1,015 MiB |



The baseline started at 1,014 MiB and returned there when its scanner threads exited. Its native Area callbacks retained their RegionGenerator, which retained the ThreadLocal keys: weak keys alone could not collect this cycle. The fix removes each seed's native cache entries in scan/verification finally blocks. Scratch generation now uses two owned threads instead of leaving seed-local caches on the game's shared workers, and joins those threads when the scratch world closes. Search completion also waits for scanner workers to exit.



Early fixed throughput was about 2.77 seeds/second; the final 200-seed batch averaged about 2.80. These are focused native sampling rates, not full search throughput or a game FPS benchmark. A separate 100-seed before/after comparison gave identical query checksums. The checksum printed in the raw report is the extended 1,200-seed checksum.



Twenty-four scratch worlds generated the same native FEATURES block hash at chunk (0,0). All 72 weak references to levels, chunk generators and region generators were cleared after close/GC, and scratch-generation thread count returned from zero to zero. The final build passed all 16 automated tests. A separate five-second God search tested 23 seeds, then cancelled and waited for scanner workers to exit; it returned no confirmed match. The 289-sample cache-reset check passed, and the spawn-chunk repeat check retained its previous SHA-256. The raw measurements are supplied as `memory-regression.txt`.



This confirms the identified cache leak is fixed in the tested setup. It does not establish that every modpack has no other memory or performance issues. Production code does not force GC; ordinary allocation and native chunk generation still cost CPU and memory during a search.



## 0.2.2 optional checks regression



Two new tests failed against 0.2.1: missing optional rock diversity reduced a fully confirmed preset score below 100, and a wishlist containing only zero-weight requirements could not reach a minimum match of 100. After the fix, all 19 unit tests pass, including a separate test proving that optional evidence cannot change shortlist distance ordering or the displayed best result.



Scoring and candidate ranking now use required checks only. A wishlist containing only zero-weight checks receives 100 when all its requirements are confirmed, otherwise 0. Optional checks display "Not needed for this search" without warning colours, pending details or coordinates. Region land coverage, detailed terrain targets and rock-diversity sampling are skipped when not requested. Copper vein centers are still used when required to locate loose starter copper.



The release still needs every selected requirement confirmed; removing optional influence does not bypass those gates. Earlier benchmark scores used the old scoring formula and should not be compared directly with 0.2.2 scores.


The 0.2.2 development Minecraft smoke run passed: a real climate-and-biome wishlist reached 100 with a minimum match of 100, optional target lists were empty, matching spawn choices passed and a wrong biome failed. History round trips and coordinate-hidden exports passed, and the rendered result labelled unused checks clearly. The 289 native samples and repeated chunk block hashes remained equal. Search budgets were zero in this run; it was a regression check, not a performance benchmark.

## 0.2.3 World-tab button


All 19 tests and native Minecraft smoke checks passed. The Scout button is owned by the World-tab grid, centered at width 310; Game and More tabs contain no Scout button. Switching back to World restores it and clicking opens search. Screenshots were inspected. The 289 native samples and repeat generation hash remained unchanged. Full preview-mod combinations were not exercised in this run.
