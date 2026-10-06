TerraFirmaScout 0.2.2 fixes optional checks affecting match scores and search ordering.

- Only requested checks affect your match score and which candidates Scout checks first.
- Unused checks say "Not needed for this search" instead of appearing unconfirmed.
- Skips terrain, land-coverage and rock-variety sampling when it is not needed.
- A fully confirmed wishlist with only unweighted checks can reach 100%.

All 19 automated tests passed. A development Minecraft run confirmed a real climate-and-biome wishlist at a minimum match of 100, empty optional target lists, matching and mismatching spawn filters, saved reports and unchanged repeated native generation. This was a regression check, not a search-speed benchmark.

Includes the memory fix from 0.2.1. Requires Minecraft 1.21.1, NeoForge 21.1.234 or newer, TFC 4.2.11 and Java 21. Replace the old Scout jar in your mods folder, then restart Minecraft.

This is an experimental prerelease. Strict requests may take time or return no match. Broader modpack and dedicated-server compatibility still need testing.
