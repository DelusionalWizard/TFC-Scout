# TerraFirmaScout

Find a TerraFirmaCraft seed that fits the way you want to play. Pick an easier start, a tough challenge, or make a wishlist of the climate, biomes, rocks and nearby resources you want.

TerraFirmaScout searches seeds from the world-creation screen and checks promising results using TFC's normal world generation. Every selected requirement must be confirmed before you can use a result through Scout. Checks you did not ask for do not affect the match score or seed selection.

Scout is available from the **World** tab in Create World.

## Minecraft 1.20.1 (Forge) build and compatibility

This branch builds TerraFirmaScout for **Minecraft 1.20.1, Forge 47.4 and TerraFirmaCraft 3.2.25 on Java 17**. Put `terrafirmascout-1.20.1-0.2.8.jar` in `mods`. The 1.21.1 / NeoForge version is on `main`.

Scout reads what the world can actually generate, so it works with addons and packs that change generation. If a pack removes a resource, that requirement is skipped and the search screen says so. Features for a particular pack only run when that pack's mod is installed.

### What was tested

Tested on 7 October 2026 by launching the real game (Forge 47.4.18) and running Scout's automated checks: world creation screen, searching, Pause/Stop, results, picking a seed, and confirmation in a real temporary world.

- **TerraFirmaCraft 3.2.25 with Patchouli, no other mods:** passes.
- **TerraFirmaGreg Modern 0.13.10 (the complete pack, 262 mods at its exact versions):** passes. This includes, among others, TerraFirmaGreg-Core 0.9.23, GregTech CEu Modern 7.5.3, KubeJS 2001.6.5 with KubeJS TFC, Lithostitched 1.4.11, TFC ruined world 0.0.4, TFC Ruins 1.0.1, TFC Tumbleweed 1.2.2, Tumbleweed 0.5.5, FirmaLife 2.1.28, ArborFirmaCraft 1.0.22, Firma: Civilization 1.0.9, Roads and Roofs TFC 0.2.5, Create 6.0.8, Ad Astra 1.15.20, Beneath 1.0.6, TFCGenViewer 1.5.1, Cherished Worlds 6.1.7 and FancyMenu 3.8.1.

On 8 October 2026 version 0.2.6 was rerun the same way (plain TFC and the full TerraFirmaGreg pack, including new river, lake and coast checks on real seeds).

Individual addons were tested only as part of that pack, not one at a time. Other versions of TerraFirmaGreg, other packs and dedicated servers are untested.

### TerraFirmaGreg

- Scout follows TerraFirmaGreg's changed world generation. Searches are slower there, because the pack's generation is heavier and Scout has to check seeds one at a time.
- TerraFirmaGreg removes TFC's default ore veins and adds its own that place GregTech ores. Scout confirms copper, tin, graphite, iron and coal by those ores. TFC's ore melting is removed too, so the loose copper requirement is skipped in that pack.
- Only with TerraFirmaGreg installed, the Dream, Easy and Fair presets also require iron and coal. Iron and coal are optional resources in the wishlist everywhere.
- Pause now takes effect in well under a second in that pack (0.2.8 and later; earlier versions could take a few seconds).
- While Scout checks seeds in that pack, the game log (`latest.log`) will show extra lines from GregTech such as "Tried to set output item stack that doesn't exist", about 8 per seed checked. GregTech already logs the same kind of lines at every normal start of the pack. They come from the pack's own data, are not crashes, and do not affect Scout's results.

## Features

- Five starting presets, from easiest to harshest: Dream Start, Easy Start, Fair Start, Rugged Start and Wilderness Start.
- Custom requirements for resources (including river, lake, coast or ocean and crop suitability), travel distances, climate, spawn and nearby biomes, surface rocks, forests, spawn height and building spots.
- Pause, resume and stop controls, with Pausing and Stopping shown while a check in progress finishes.
- A Results screen with every confirmed match from the search, plus close calls (seeds that missed exactly one check) you can give a closer look.
- Check any seed (a number or text) against your choices, and copy a seed or a plain-text report to share.
- Options for search speed, stopping after a number of matches or minutes, and a sound when a match is found. Your choices are remembered.
- Saved matching seeds, with sorting, short notes and delete, and exported reports.
- Hidden resource coordinates until you choose Reveal.
- The result shows the forest type and tree density at spawn.

## Requirements

Minecraft **1.21.1**, NeoForge **21.1.234 or newer**, TerraFirmaCraft **4.2.11**, and Java **21**. TFC itself requires Patchouli; Scout adds no extra mod dependencies to a working TFC installation.

## Download

Download jars from the [GitHub Releases page](https://github.com/DelusionalWizard/TFC-Scout/releases): `v0.2.8` for Minecraft 1.21.1 (NeoForge) and `v0.2.8-1.20.1` for Minecraft 1.20.1 (Forge). Release files are not kept in the repository, and older releases have been removed.

## Getting started

Place the Scout jar in your instance's `mods` folder, replacing any older Scout version. Open **Create World**, choose the TFC world preset, then open **TerraFirmaScout**. Select a preset or edit **Pick your world**, and click **Find a seed**.

When a confirmed match appears, choose **Use this seed** or keep searching. Strict requests can take a long time or reach the search limit without finding a match. Resource checks confirm blocks found in the areas searched; they do not guarantee a large deposit or an easy journey.

## Current status

Version **0.2.8** is experimental. A fully confirmed Dream Start result and broader modpack compatibility still need further testing.

**What is new in 0.2.8:** faster, more responsive seed checks in big packs such as TerraFirmaGreg (1.20.1 build) and groundwork for animal checks. **Earlier in 0.2.7:** two more optional wishlist checks, Crops (enough of the loaded crops fit the yearly climate at spawn) and, on 1.21.1 only, Farmland moisture (rain alone keeps farmland 30-80% moist). **Earlier in 0.2.6:** the single Freshwater requirement is replaced by separate, optional River, Lake and Coast or ocean checks, each with its own distance (the presets no longer require water). The result screen shows the forest type and tree density at spawn (as a percentage on this build). Saved seeds and wishlists from older versions still load. See [the changelog](CHANGELOG.md).

See [the test report](TEST-REPORT.md) for results and limits, [preset research](RESEARCH-AND-PROFILES.md) for the starting styles, and [API notes](API-NOTES.md) for generation details.

## Building

Use a Java 21 JDK. Place these dependencies in the project's `libs` folder:

- TFC 4.2.11 as `libs/tfc-4.2.11.jar`
- Patchouli 1.21.1-93-NEOFORGE as `libs/patchouli.jar`

These third-party jars are not included in this repository. Obtain them from their official project pages.

On Windows, run `gradlew.bat test build`. On Linux or macOS, run `bash gradlew test build`. The mod jar appears in `build/libs`.

`runClient` starts a separate development instance. `runSmokeClient -PsmokeSeconds=5` checks native generation and search cancellation. Add `-PmemoryTest=true -PsmokeSeconds=0` for the longer native cache retention regression. Development harness classes are excluded from release jars.

## Credits and license

TerraFirmaScout is an independent addon, not an official TerraFirmaCraft project. It relies on TerraFirmaCraft and Minecraft's native world generation. See [third-party notices](THIRD-PARTY-NOTICES.md).

Development and documentation used substantial generative AI assistance. The mod does not use generative AI during gameplay.

Licensed under [EUPL-1.2](LICENSE.txt).
