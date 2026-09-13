# Project conventions

- Target Minecraft 26.1.2 and 26.2 Fabric with separate JARs. Test minimum Loader 0.18.4; optional mods may require newer Loader. Verify each target before widening compatibility.
- Documentation defaults to English (`README.md`), with Simplified Chinese companions (`README_zh-CN.md`). Keep both updated. Game text must use translation keys, including the mod name; never combine English and Chinese names with a slash. Keep all bundled locales complete and preserve format placeholders.
- Every stable version such as 0.1.0 needs a GitHub Release with runtime JAR and bilingual changelog. Never silently replace published assets.
- Use identifiers such as 0.2.0-pre2 for prereleases. After a tested stable version reaches main, automation publishes it with matching notes. Versions containing pre must not create GitHub Releases; CI artifacts remain available.
- Prices, knowledge, inventory, XP and permissions are server-authoritative. Client settings cannot alter multiplayer economy rules.
- Preflight every transaction before mutation. Value decorated/data-bearing items by base identity only. Discard their metadata and contents on conversion; purchases use server-created default prototypes. Preserve only validated TaCZ/LR model IDs, and disclose data loss in localized UI/docs.
- Preserve existing saves or explicitly migrate them. Test respawn through the full server lifecycle.
- See [Chinese conventions](AGENTS_zh-CN.md).
