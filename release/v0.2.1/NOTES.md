TerraFirmaScout 0.2.1 fixes a seed-cache leak that made searches slow down as more seeds were tested.

- Clears each seed's native caches when scanning finishes.
- Releases scratch-world generation threads and caches after verification.
- Waits for scanner workers to stop before a search is marked finished.

The regression test passed 1,200 native seed samples with stable live memory and no old region generators retained after collection. Twenty-four scratch worlds also released their levels, generators and worker threads. All 16 automated checks passed, and repeated native chunk generation stayed identical.

Requires Minecraft 1.21.1, NeoForge 21.1.234 or newer, TerraFirmaCraft 4.2.11, and Java 21. Install alongside a working TFC installation, replacing any older Scout jar.

This is an experimental prerelease. Strict searches can take time or return no match. A fully confirmed God-preset result and broader modpack compatibility still need further testing.
