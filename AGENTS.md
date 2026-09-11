# Project conventions

- Target Minecraft 26.2 Fabric; verify later versions before widening compatibility.
- Documentation defaults to English (`README.md`), with Simplified Chinese companions (`README_zh-CN.md`). Keep both updated. Game text must use translation keys, including the mod name; never combine English and Chinese names with a slash. Keep all bundled locales complete and preserve format placeholders.
- Every stable version such as 0.1.0 needs a GitHub Release with runtime JAR and bilingual changelog. Never silently replace published assets.
- Use identifiers such as 0.2.0-pre2 for prereleases. After a tested version reaches main, automation publishes it with matching notes; prereleases must be marked correctly.
- Prices, knowledge, inventory, XP and permissions are server-authoritative. Client settings cannot alter multiplayer economy rules.
- Preflight every transaction before mutation. Reject unsupported component variants instead of deleting or copying their contents.
- Preserve existing saves or explicitly migrate them. Test respawn through the full server lifecycle.
- See [Chinese conventions](AGENTS_zh-CN.md).
