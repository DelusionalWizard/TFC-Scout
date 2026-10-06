# Shared project instructions

Cooper alternates between Codex and Claude Code as usage allows. Maintain one continuous project; do not restart from an old archive or assume another tool's work is disposable.

Before editing, read PROJECT-STATUS.md, CHANGELOG.md, README.md and relevant TEST-REPORT.md sections. Inspect Git status and the current remote head. Preserve unrelated changes.

After meaningful work, update PROJECT-STATUS.md with the current version, actual tests and limitations, installation state, release/commit references and pending work. Add a dated CHANGELOG.md entry explaining changes and why. Record failures and unfinished work honestly. Keep both deliverable source and Git working checkout aligned. Update the local Claude-Code-handoff.md when paths or workflow change.

Increment build.gradle and mod metadata together for new releases. Preserve old published assets. Verify installed/released jar SHA-256 against the tested build. Use version-specific release files and prerelease workflows; workflow success checks publishing, not compilation.

Keep native TFC world generation unchanged. Every required check must be confirmed; optional checks must not lower scores, reject seeds or influence ranking. Preserve native per-thread cache cleanup, owned scratch executors and cooperative cancellation. Development harnesses must remain excluded from release jars.

Use clear player wording. Do not publish personal marketing disclosures or credentials without authorization. Distinguish actual gameplay/server/modpack tests from code inspection or focused fixtures. Do not kill Cooper's running Minecraft/Java process. Only replace Scout in an authorized instance; preserve other mods, saves and settings.
