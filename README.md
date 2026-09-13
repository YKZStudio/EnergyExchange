Warning: truncated output (original token count: 2803)
Total output lines: 108

# Energy Exchange

[简体中文](README_zh-CN.md) · [Changelog](docs/releases/0.3.3.md) · [Data packs](docs/DATAPACKS.md) · [Development](docs/DEVELOPMENT.md)

An independent energy-exchange mod for **Minecraft Java 26.1.2 / 26.2 + Fabric**, inspired by equivalent exchange. It is not an official ProjectE port and includes no ProjectE code or assets.

**0.3.3: items → personal Energy and knowledge → table or villager purchases.**

## Install

Requires Java 25. Install exactly one matching runtime JAR on both client and server; do not install the sources JAR or both target JARs.

| Minecraft | Runtime JAR | Fabric API minimum | Optional Mod Menu |
| --- | --- | --- | --- |
| 26.1.2 | `energyexchange-mc26.1.2-0.3.3.jar` · Loader 0.18.4+ | 0.155.3+26.1.2 | 18.0.1 |
| 26.2 | `energyexchange-mc26.2-0.3.3.jar` · Loader 0.19.3+ | 0.159.0+26.2 | 20.0.2 |

TaCZ R3-hotfix has builds for both targets, but independently requires Loader **0.19.3+** (integration tests use 0.19.5). For 26.1.2 use Forge Config API Port 26.1.5; for 26.2 use 26.2.1. Match TaCZ and gun packs on both sides. Do not downgrade a 26.2 world to 26.1.2; this is a separate game-version build, not a world downgrade converter.

Ten locales remain available. The in-game mod name follows the selected language; Mod Menu uses its translated-name setting.

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
2. The catalog initially shows learned items. Switch to **All items** to browse unknown items; learned entries stay first. Default order is exact Energy descending; Mod Menu also offers ascending Energy and name order. The separate learning button has been removed.
3. Search by localized name or ID, select an entry and choose **Buy: 64 / 32 / 16 / 1**. Quantities exceeding the item stack limit or your balance are disabled. Ender pearls allow 16 or 1; non-stackable items allow 1. Gray entries are not learned. Tooltips show exact prices and yields.
4. Select **Bottle o’ Enchanting**, then use the bottom-right 64/32/16/1 quantity buttons. Bottles go directly into player inventory without prior learning. Default: **896 ENERGY per bottle**, overridable/disableable by server datapacks. Throwing them awards vanilla XP; buying never awards points directly.
5. Closing returns the input. Tables have no shared storage; each player's wallet and input are private.

Only living survival/adventure players can trade. A placed table must exist within eight blocks. A tablet's inventory slot is locked while open; changing the selected slot or losing the tablet invalidates trading.

Villagers also sell Energy Exchange content through the vanilla trade screen: librarians stock the table and tablet, clerics stock the materials, toolsmiths and weaponsmiths stock tools, and armorers stock armor. Offers are added to the existing profession-level pools without replacing vanilla offers. Dark Matter and Red Matter tiers require late-game vanilla materials; Infinity offers have one use and require 64 netherite blocks plus a nether star. New villagers and villagers that acquire a level after in…803 tokens truncated…1`. Inside this menu, conversion/learning uses the input; otherwise it uses the main hand. Other open containers block trading.

- Exact integer balances from zero through `10^128−1`; overflow rejects the whole transaction.
- Server validation covers menu identity, session nonce, request sequence, rule revision, distance, game mode, knowledge, price and inventory. Clients supply neither stacks nor balances. Stale quotes refresh; duplicate requests do not settle twice.
- Command purchases of 1–2304 items simulate all inventory insertion before committing. Insufficient space means no debit, partial delivery or dropped purchase. Armor and offhand are excluded.
- Accounts persist across death/respawn, dimensions and normal restarts. Maximum 4,096 learned keys and 60,000 serialized characters; all vanilla identities fit. Corrupt/future account data locks trading without reset.
- Trading pauses during resource reload; failure remains locked until a successful reload. Disabled identities remain learned.
- Persistence follows Minecraft saves, so power loss or process crashes can roll back to the last save. Direct administrator/other-mod mutations are outside this mod's transaction control.

## Build and releases

`./gradlew build -Pminecraft_version=26.1.2` or `./gradlew build -Pminecraft_version=26.2` compiles and runs unit/server GameTests. `xvfb-run -a ./gradlew runClientGameTest -Pminecraft_version=26.1.2` (or `26.2`) tests the client trading flow on Linux. On Windows use `gradlew.bat`.

Every stable version receives a GitHub Release containing both target runtime JARs and a bilingual changelog. Existing assets are never silently replaced. [0.1.0 release](https://github.com/YKZStudio/EnergyExchange/releases/tag/v0.1.0).

MIT licensed. Bundled Unicode pronunciation data retains its own license; see [third-party notices](THIRD_PARTY_NOTICES.md). The TaCZ adapter is independently implemented against public APIs; no upstream code or assets are bundled.

## 0.3.1 compatibility and pricing

Optional Traveler’s Backpack **Fabric 26.1.2-11.2.10 / 26.2-11.3.2** is supported with prices for all 80 registered items. Filled, dyed or upgraded backpacks convert at the base registered item price and buy back empty at the default tier. Remove anything you want to keep before converting.

The server selects a vanilla-only, TaCZ, backpack, or combined recipe price profile based on installed mods. This fixes the cheaper TaCZ gunpowder crafting loop and recalculates ammunition, firearms, attachments and backpacks from the actual cheapest resolved recipes. Fractional yields remain exact until batch rounding. Data-pack item/variant overrides still take priority. See [pricing details](docs/PRICING.md).

Only stable versions create GitHub Releases with JAR and bilingual notes. Versions containing `pre` still build in CI but do not create a Release.

## 0.3 armory and advanced tooltips

Adds 32 materials, tools, weapons and armor across Dark Matter, Red Matter and Infinity tiers. Every recipe uses the vanilla 3×3 table, with area mining, hostile sweeps and Infinity suit flight. F3+H tooltips show exact server ENERGY above the item ID. See [all recipes and controls](docs/ARMORY.md).

## 0.3.1 input slot

The single input holds **256 identical items with identical components**, including ordinarily unstackable tools. Deposit through clicks or repeated Shift transfers. Left-click withdraws at most one native stack; right-click withdraws half, capped at the native limit; Shift moves as much as fits into inventory. Number-key swaps, dropping, closing/death returns all split into native stacks. Full-inventory returns use vanilla dropping rules. Drag distribution excludes this special slot.

The lower-left discard warning appears only when the input differs from its default prototype; empty/default inputs show no warning. Valid TaCZ model identities compare against their model prototype. Update both client and server to 0.3.2 for their Minecraft version.
