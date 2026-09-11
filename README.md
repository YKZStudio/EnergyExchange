# Energy Exchange

[简体中文](README_zh-CN.md) · [Changelog](docs/releases/0.2.0.md) · [Data packs](docs/DATAPACKS.md) · [Development](docs/DEVELOPMENT.md)

An independent energy-exchange mod for **Minecraft Java 26.2 + Fabric**, inspired by equivalent exchange. It is not an official ProjectE port and includes no ProjectE code or assets.

**0.2.0: items → personal Energy and knowledge → table purchases of items or XP.**

## Install

- Java 25, Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2.
- Install this mod and Fabric API on both client and server. The 0.2 blocks, items and menus require both sides.
- Optional: Mod Menu 20.0.2; TaCZ Refabricated 26.2 R3-hotfix with Forge Config API Port 26.2.1. Use matching TaCZ versions and gun packs on both sides.
- Put `energyexchange-0.2.0.jar` in `mods`, not the sources JAR. English, Simplified Chinese, Japanese, German, French, Spanish, Brazilian Portuguese and Russian are included, with additional Traditional Chinese (Taiwan/Hong Kong) locales. The in-game name follows the selected language; Mod Menu uses its standard translated-name setting.
- Compatibility is `~26.2`; later series require fresh validation.
- Existing 0.1 balances and knowledge are retained without scaling or reset. Back up the world before upgrading; 0.2 items and variant knowledge do not support direct downgrade to 0.1.

## Craft and trade

Both recipes use a crafting table and **3×3 vanilla ingredients**. No OP or cheats are required.

| Table | Column 1 | Column 2 | Column 3 |
| --- | --- | --- | --- |
| Row 1 | Obsidian | Diamond | Obsidian |
| Row 2 | Redstone dust | Diamond | Redstone dust |
| Row 3 | Obsidian | Diamond | Obsidian |

| Tablet | Column 1 | Column 2 | Column 3 |
| --- | --- | --- | --- |
| Row 1 | Obsidian | Gold ingot | Obsidian |
| Row 2 | Gold ingot | Diamond | Gold ingot |
| Row 3 | Obsidian | Gold ingot | Obsidian |

Use the placed table, or use a tablet in either hand. The input is on the left, the personal catalog on the right, and inventory below.

1. Insert items and press **Convert all** to consume the entire input stack, earn Energy and learn its identity.
2. The catalog initially shows learned items. Switch to **All items** to browse unknown items; learned entries stay first. The separate learning button has been removed.
3. Search by localized name or ID, select an entry and choose **Buy: 64 / 32 / 16 / 1**. Quantities exceeding the item stack limit or your balance are disabled. Ender pearls allow 16 or 1; non-stackable items allow 1. Gray entries are not learned. Tooltips show exact prices and yields.
4. **Buy 10 XP** awards ten experience points, not levels. Default: 128 Energy per point, controlled by the server.
5. Closing returns the input. Tables have no shared storage; each player's wallet and input are private.

Only living survival/adventure players can trade. A placed table must exist within eight blocks. A tablet's inventory slot is locked while open; changing the selected slot or losing the tablet invalidates trading.

## Prices and compatibility

- Positive purchase prices for all **1,537 vanilla item IDs** in 26.2. Non-survival items still require an obtained and learned sample; they are not automatically unlocked.
- TaCZ guns, ammunition, attachments, workbenches and bundled LRTactical items use model-specific identities. Loaded additional gun-pack models receive their base item's fallback value, with per-model data-pack overrides available.
- `tools/generate_values.py` generates defaults constrained by ordinary vanilla crafting, smelting, stonecutting and smithing recipes. Purchase units round up; fractional conversion yields round down once per batch. Batches worth less than one Energy are rejected without consuming items.
- This is a configurable starter balance baseline, not a proof for every machine, villager trade, special recipe or gun pack. Pack authors should audit additions and override or disable values. Some 0.1 item prices change under the recipe constraints; saved balances do not.
- Vanilla items require default components. Renamed, enchanted, damaged, written-content, non-default potion and filled-container variants are rejected without losing contents.
- TaCZ uses standard safe model prototypes. Unload guns and their chamber, remove attachments, and remove custom appearances, dummy ammunition and other extra data before conversion. Purchases produce empty guns. Fire-selector changes and cleared ordinary runtime state are harmless and accepted.

## Settings and data packs

Mod Menu → Energy Exchange → Configure controls unlearned entries, compact numbers, experimental pinyin search, XP enablement and cost per point.

Pinyin search is **off by default**. Enable it to find Simplified Chinese display names using full pinyin, initials or mixed syllable prefixes: `moyingzhenzhu`, `myzz`, `moyzz` all find 末影珍珠. Case, spaces, tones and full-width letters are normalized. Mixed Chinese/pinyin (`末影zz`, `mo影z珠`), `lv`/`lü`/`lǜ`/`lu:`, and multiple readings (`zhongchui` / `chongchui` for 重锤) are supported. Search works offline and needs no REI. It is phonetic prefix matching, not typo correction; results still respect the Learned/All filter and use the current localized name.

Existing four-field pre1 settings migrate with pinyin off and the initial catalog set to learned-only. XP settings and compact-number preferences are preserved. Balances and learned items are unchanged.

Display preferences are client-side. XP settings are local defaults saved in `config/energyexchange.json`, applied **at the next world/server start**. Multiplayer uses the server owner's configuration; clients cannot change server prices. Dedicated servers edit the same file and restart. Number tooltips and `/ee balance` always show exact integers.

See [data-pack documentation](docs/DATAPACKS.md) for item/variant overrides, disabled rules and `/reload`.

## Commands and protections

`/ee help`, `value`, `balance`, `list [page]`, `learn`, `burn [count|all]`, and `buy <itemID> [count]` remain available. Example: `/ee buy tacz:modern_kinetic_gun#tacz:ak47 1`. Inside this menu, conversion/learning uses the input; otherwise it uses the main hand. Other open containers block trading.

- Exact integer balances from zero through `10^128−1`; overflow rejects the whole transaction.
- Server validation covers menu identity, session nonce, request sequence, rule revision, distance, game mode, knowledge, price and inventory. Clients supply neither stacks nor balances. Stale quotes refresh; duplicate requests do not settle twice.
- Command purchases of 1–2304 items simulate all inventory insertion before committing. Insufficient space means no debit, partial delivery or dropped purchase. Armor and offhand are excluded.
- Accounts persist across death/respawn, dimensions and normal restarts. Maximum 4,096 learned keys and 60,000 serialized characters; all vanilla identities fit. Corrupt/future account data locks trading without reset.
- Trading pauses during resource reload; failure remains locked until a successful reload. Disabled identities remain learned.
- Persistence follows Minecraft saves, so power loss or process crashes can roll back to the last save. Direct administrator/other-mod mutations are outside this mod's transaction control.

## Build and releases

`./gradlew build` compiles and runs unit/server GameTests. `xvfb-run -a ./gradlew runClientGameTest` tests the client trading flow on Linux. On Windows use `gradlew.bat`.

Every stable version receives a GitHub Release containing its runtime JAR and bilingual changelog. Existing assets are never silently replaced. [0.1.0 release](https://github.com/YKZStudio/EnergyExchange/releases/tag/v0.1.0).

MIT licensed. Bundled Unicode pronunciation data retains its own license; see [third-party notices](THIRD_PARTY_NOTICES.md). The TaCZ adapter is independently implemented against public APIs; no upstream code or assets are bundled.
