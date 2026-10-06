# Seed profiles and research

These are authored defaults informed by player discussions, not a community standard or a statistical ranking of all TFC worlds. There is no established numeric definition of God, Good, Average, Hard, or Super Hard. A profile selects a play style; one seed can satisfy multiple profiles. Average means moderate progression guarantees, not the measured median seed.

## What players value

The [beginner seed discussion](https://www.reddit.com/r/TerraFirmaCraft/comments/1cw093o) emphasizes reducing resource frustration, including graphite and rock-region size. An [excellent starter seed report](https://www.reddit.com/r/TerraFirmaCraft/comments/1l7ur5h) praises plains, nearby copper, kaolin, and graphite. These are player priorities, not evidence that those old-version seeds reproduce on 1.21.1.

The detailed [good-seed selection discussion](https://www.reddit.com/r/TerraFirmaCraft/comments/i6jv7y/how_to_find_a_good_tfc_seed/) emphasizes diverse useful rocks, flux, progression ores, buildable terrain, and land accessibility. Its mechanics target 1.12.2; this addon uses current native 4.2.11 placement instead of copying its ore tables. Some players prefer boating and island exploration, so mainland-only God is a deliberate beginner-oriented choice from the project specification.

[Cold-climate frustration](https://www.reddit.com/r/TerraFirmaCraft/comments/1g3h5lq) and [crop/clothing difficulties](https://www.reddit.com/r/TerraFirmaCraft/comments/1exmnbd) inform the challenge modes. We infer that a cold, drier start and more travel are useful difficulty selectors. We do not claim that every player defines Hard this way, or that base TFC causes the frostbite damage reported by some modpacks.

Current mechanics were checked against [TFC v4.2.11 source](https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/v4.2.11), commit `16b1b8f6312697e8ec8324b7f1ad74a4613ac15a`, and the supplied jar. The [climate guide](https://terrafirmacraft.github.io/Field-Guide/en_us/core_mechanics/climate.html), [pottery guide](https://terrafirmacraft.github.io/Field-Guide/en_us/getting_started/pottery.html), and [ore guide](https://terrafirmacraft.github.io/Field-Guide/en_us/the_world/ores_and_minerals.html) provide background. Version-pinned native APIs and active datapack recipes govern verification.

## Defaults

Temperature is annual, elevation-adjusted average Celsius. Rain is TFC's average rainfall value. Distances are horizontal blocks from the natural spawn; they are maximum travel allowances, not minimum distances or proof that nearer supplies are absent.

| Profile | Allowed temperature | Ideal temperature | Allowed rain | Ideal rain | Fit threshold | Required progression |
|---|---:|---:|---:|---:|---:|---|
| God | 7–17 | 10–15 | 220–400 | 250–350 | 95 | Starter copper, copper vein, flux, tin, graphite, kaolin, mainland route |
| Good | 4–22 | 8–18 | 175–450 | 225–350 | 90 | Same resource guarantees, wider travel limits |
| Average | 0–26 | 5–20 | 125–475 | 175–400 | 85 | Starter copper, copper vein, flux and tin; graphite/kaolin optional |
| Hard | −8–6 | −2–4 | 75–275 | 125–220 | 80 | Cold climate, verified starter supplies; later resources optional |
| Super Hard | −20–0 | −10–−3 | 50–300 | 75–200 | 80 | Freezing average climate plus ≥12-block native terrain span around spawn |

Every profile requires actual freshwater, generated logs, clay, ten distinct accessible surface copper pieces worth at least 100 mB under active heating recipes, a real copper vein, and a dry 16-by-16 construction patch and an 8-by-8 camp patch, each with firm ground, at least 50% grass and at most three blocks of height variation. Challenge profiles retain a buildable patch within a wider radius. Super Hard measures ruggedness across the 48×48 area centered on the spawn chunk using TFC's pre-tree surface heights.

| Maximum distance | God | Good | Average | Hard | Super Hard |
|---|---:|---:|---:|---:|---:|
| Freshwater | 160 | 250 | 400 | 800 | 1,500 |
| Forest/logs | 350 | 500 | 800 | 1,500 | 2,500 |
| Clay | 300 | 500 | 800 | 2,000 | 3,500 |
| Surface copper | 750 | 1,000 | 1,500 | 3,000 | 5,000 |
| Copper vein | 1,000 | 1,500 | 2,500 | 3,500 | 6,000 |
| Flux | 1,200 | 2,000 | 3,500 | Optional | Optional |
| Tin | 2,000 | 3,000 | 4,500 | Optional | Optional |
| Graphite | 4,000 | 5,000 | Optional | Optional | Optional |
| Kaolin | 4,000 | 6,000 | Optional | Optional | Optional |
| Buildable terrain | 250 | 350 | 500 | 1,000 | 1,500 |
| Camp patch | 160 | 250 | 350 | 600 | 1,000 |
| Required land coverage within 1 km | 70% | 65% | 50% | Optional | Optional |

God, Good and Average require mainland region flags and connected land coverage. God and Good additionally require a continuous native land-biome corridor to verified kaolin; freshwater crossings are allowed. This does not assess cliffs, predators, or walking time.

Hard and Super Hard never claim scarce resources merely because a bounded scan missed them. Their challenge is positively verified climate and, for Super Hard, terrain. Wider supply allowances can still admit seeds with supplies nearby.

## Score and configuration

The original 100-point God weights are retained. Other profiles normalize the weights of their required criteria plus sampled rock diversity to 100. Zero-weight requirements still block selection. A high Hard score means a good fit for the harsh profile, not an easy world; scores across profiles are not directly comparable.

Each preset has its own section in `config/terrafirmascout-common.toml`: `god`, `good`, `average`, `hard`, `super_hard`. All numerical thresholds are configurable there. Optional late-resource distances are retained in configuration but are not searched or guaranteed for that profile. Custom uses the strict God requirement set with configurable numbers and a 90-point minimum. Specification is separate: players select required criteria, distances, climate, nearby-biome ANY/ALL matching and spawn filters. Every selected requirement must be verified; a specification never inherits the God label.

These defaults remain provisional until a larger benchmark verifies successful selection for each profile and independent reproduction without the addon. No standard search completion time is guaranteed.
