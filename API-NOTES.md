Studied against the actual supplied TFC 4.2.11 jar and upstream v4.2.11 sources.

| Concern | Native API used | Evidence limit |
|---|---|---|
| Seed stream | `Seed.of`, `RegionGenerator`, `TFCLayers.createRegionBiomeLayer` | Constructed in the same order as `TFCChunkGenerator.initRandomState`; never construct a second chunk-data generator on the advanced seed stream |
| Regions/mainland | `RegionGenerator.getOrCreateRegionPoint`, `Region.Point` | Region flags are coarse; final land coverage enumerates block columns through the native quart biome map |
| Rivers/lakes | `BiomeSourceExtension.getBiomeExtension`, `RegionPartition` | Biome candidates are inferred; final result requires a real freshwater block |
| Temperature/rainfall | `ChunkDataGenerator.generate`, `ChunkData.getAverageSeaLevelTemp/getAverageRainfall` | Final average temperature is adjusted by elevation using TFC's own helper |
| Rocks/layers | `ChunkDataGenerator.generateRock` | Geology and layer samples are candidates; final flux requires a raw/hardened rock block |
| Forests | `ChunkData.getForestType` | Forest suitability is inferred; final tree access inspects generated logs |
| Ore candidates | active configured-feature registry, `VeinFeature.getVeinsAtChunk`, `IVeinConfig` | A deterministic vein center is still inferred: rock replacements, density and decoration can fail |
| Kaolin candidates | active biome feature list, active `ClimatePlacement.isValidNonHemispheral` | Native climate prefilter only. Groundwater changes during terrain generation; final block inspection is required |
| Exact blocks | isolated `ServerLevel`, normal `ServerChunkCache`, `ChunkStatus.FEATURES` | Native noise/surface/carving/feature dependency pipeline; lighting and mobs skipped for resource targets |
| Spawn | `BiomeSourceExtension.findSpawnBiome`, `PlayerRespawnLogic.getSpawnPosInChunk` | TFC's same seed, RNG type and spiral. Spawn checks can require FULL chunks |
| Starter copper | real small ore blocks, active `HeatingRecipe` results | Surface-piece route only. Stochastic panning is deliberately not treated as a guaranteed yield |
| Travel | native biome corridor search | Verifies no saltwater/ocean biome crossing on the selected corridor; it does not prove a flat, dry walking route or estimate travel difficulty |

No features, ores, biomes, presets, or spawn overrides are registered. Accessor mixins expose the pending datapack directory, the scratch server's level map, and TFC's per-thread cache handles. A targeted executor redirect in TFCChunkGenerator.createBiomes/fillFromNoise keeps calls already running on Scout scratch threads on those same owned threads. Normal game generation uses Minecraft's original executor. The native generation algorithms and seed sequence are unchanged.

The region adapter copies its biome source and owns its caches. Chunk verification is serialized on a coordinator thread; region scans use a bounded pool. Pause/cancel are cooperative at scan and chunk boundaries. A native chunk generation task already underway must finish before its scratch world closes.

Search incompleteness is distinct from negative proof: failing a hard climate/terrain limit is FAILED; failing to find a block within a bounded target scan is INFERRED. Neither permits selection.

Fingerprinting hashes Minecraft/mod versions, all loaded mod archives, selected dimension generator codecs/settings, structure/bonus options, preset identity, enabled pack configuration, custom datapack contents, and TFC configuration files. This is intentionally conservative. It does not establish compatibility with arbitrary third-party changes to spawn events, resource loot, or global server lifecycle hooks.

Specification uses the native biome source and generated spawn ChunkData for surface rock, forest type/density and elevation. Nearby biome queries sample a bounded 16-block grid: a positive biome match is exact; failure to sample a tiny biome is not proof of absence. Clay targeting also considers water-edge deposits in dry climate. Ore ranking uses active native vein projection/host settings and the native terrain-height sampler. Ranking never establishes placement or hard-rejects geology.

Terrain inspection builds a private Heightmap using Minecraft's native opaque predicate over decorated blocks, then requires firm dry ground and three blocks of replaceable/air headroom. It does not replace any chunk heightmaps or modify generation state. This avoids stale pre-decoration ground beneath trees passing as an open construction patch.

Seed-local native ThreadLocal Areas can retain their RegionGenerator through layer callbacks, which also retain the owning ThreadLocal keys. Scanners remove biome, uniform biome/rock, forest and rock-layer entries on the calling thread in finally blocks. Verification clears the candidate adapter again on the coordinator. Scratch worlds own a two-thread generation executor, shut it down and join it before releasing resources, clear coordinator caches, and release their server level map. No explicit GC is invoked by production search code.
