# TerraFirmaScout

Find a TerraFirmaCraft seed that fits the way you want to play. Pick an easier start, a tough challenge, or make a wishlist of the climate, biomes, rocks and nearby resources you want.

TerraFirmaScout searches seeds from the world-creation screen and checks promising results using TFC's normal world generation. Every selected requirement must be confirmed before you can use a result through Scout. Checks you did not ask for do not affect the match score or seed selection.

Scout is available from the **World** tab in Create World.

## Features

- Five starting presets, from easiest to harshest: Dream Start, Easy Start, Fair Start, Rugged Start and Wilderness Start.
- Custom requirements for resources (including river, lake and coast or ocean), travel distances, climate, spawn and nearby biomes, surface rocks, forests, spawn height and building spots.
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

Download jars from the [GitHub Releases page](https://github.com/DelusionalWizard/TFC-Scout/releases): `v0.2.6` for Minecraft 1.21.1 (NeoForge) and `v0.2.6-1.20.1` for Minecraft 1.20.1 (Forge). Release files are not kept in the repository, and older releases have been removed.

## Getting started

Place the Scout jar in your instance's `mods` folder, replacing any older Scout version. Open **Create World**, choose the TFC world preset, then open **TerraFirmaScout**. Select a preset or edit **Pick your world**, and click **Find a seed**.

When a confirmed match appears, choose **Use this seed** or keep searching. Strict requests can take a long time or reach the search limit without finding a match. Resource checks confirm blocks found in the areas searched; they do not guarantee a large deposit or an easy journey.

## Current status

Version **0.2.7** is experimental. A fully confirmed Dream Start result and broader modpack compatibility still need further testing.

**What is new in 0.2.7:** two more optional wishlist checks, Crops (enough of the loaded crops fit the yearly climate at spawn) and, on 1.21.1 only, Farmland moisture (rain alone keeps farmland 30-80% moist). **Earlier in 0.2.6:** the single Freshwater requirement is replaced by separate, optional River, Lake and Coast or ocean checks, each with its own distance (the presets no longer require water). The result screen shows the forest type and tree density at spawn. Saved seeds and wishlists from older versions still load. See [the changelog](CHANGELOG.md).

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
