# TerraFirmaScout

Find a TerraFirmaCraft seed that fits the way you want to play. Pick an easier start, a tough challenge, or make a wishlist of the climate, biomes, rocks and nearby resources you want.

TerraFirmaScout searches seeds from the world-creation screen and checks promising results using TFC's normal world generation. Every selected requirement must be confirmed before you can use a result through Scout.

## Features

- God, Good, Average, Hard and Super Hard starting presets.
- Custom requirements for resources, travel distances, climate, spawn and nearby biomes, surface rocks, forests, spawn height and building spots.
- Pause, resume and stop controls.
- Saved matching seeds and exported reports.
- Hidden resource coordinates until you choose Reveal.

## Requirements

Minecraft **1.21.1**, NeoForge **21.1.234 or newer**, TerraFirmaCraft **4.2.11**, and Java **21**. TFC itself requires Patchouli; Scout adds no extra mod dependencies to a working TFC installation.

## Getting started

Place the Scout jar in your instance's `mods` folder, replacing any older Scout version. Open **Create World**, choose the TFC world preset, then open **TerraFirmaScout**. Select a preset or edit **Pick your world**, and click **Find a seed**.

When a confirmed match appears, choose **Use this seed** or keep searching. Strict requests can take a long time or reach the search limit without finding a match. Resource checks confirm blocks found in the areas searched; they do not guarantee a large deposit or an easy journey.

## Current status

Version **0.2.1** is experimental. A fully confirmed God-preset result and broader modpack compatibility still need further testing.

This version fixes seed caches being retained on scanner threads. A focused 1,200-seed native sampling test held memory steady with zero old region generators retained after collection. Twenty-four scratch-world checks also released their levels, generators and worker threads. These checks do not measure gameplay FPS or guarantee compatibility with every modpack.

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
