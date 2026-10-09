# Translating TerraFirmaScout

Scout's screens read their text from language files, so a language can be added without changing any code.

1. Copy `src/main/resources/assets/terrafirmascout/lang/en_us.json` and name the copy after the Minecraft language code in lower case, for example `de_de.json`, `fr_fr.json` or `pt_br.json`.
2. Translate only the values on the right. Never change the keys on the left.
3. Keep every `%s` in the translated text. Each one is filled in by the game (a seed, a number or a name). The order of the `%s` slots must stay the same, and `%%` shows a single percent sign.
4. Any key you leave out is shown in English, so a partial translation is fine.
5. Run `./gradlew test`. `TranslationTest` fails if a file uses an unknown key or changes the number of `%s` slots.

What is translated so far: buttons, titles and the main labels on the world-creation screen, the search options, saved seeds, notes and results. What is not translated yet, and is still English in every language:

- The names of the wishlist rows and tabs in **Pick your world** (the Criterion names such as "Copper vein").
- The explanation text for each check and the evidence lines in results and reports.
- The preset names and descriptions.
- The messages and status lines on the search screen.
- Biome and rock names, which come from TerraFirmaCraft and its own language files.

These are the next steps, in this order: preset and criterion names, the search status and messages, then the evidence text.
