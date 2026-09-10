# Energy Exchange

[简体中文](README.md) · [Development](docs/DEVELOPMENT.en.md) · [Data packs](docs/DATAPACKS.en.md)

An independent **Minecraft Java 26.2 + Fabric** mod inspired by equivalent-exchange gameplay. It contains no ProjectE code or assets and is not an official port.

v0.1 delivers a command-based loop: **ordinary items → personal Energy and knowledge → learned items**. Transmutation tables, a GUI, automation and recipe-based value calculation are outside this version.

## Install and build

- Java 25, Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2.
- Install the mod and Fabric API on both client and server for complete English/Simplified Chinese localization. Select English (US) or 简体中文 in game. Server-only installation also works, with English fallback messages for clients without the mod.
- Linux/macOS: `./gradlew build`; Windows: `gradlew.bat build`. Install `build/libs/energyexchange-0.1.0.jar`, not the sources JAR.
- Successful GitHub Actions runs upload the JAR and test reports. Follow the [acceptance checklist](docs/DEVELOPMENT.en.md) in a test world before release.
- Compatibility is declared as `~26.2` (the 26.2 series). Later 26.3+ releases require rebuilding and validation; future compatibility is not assumed.

## Play

Commands require no OP or cheats. Transactions require a living survival/adventure player.

| Command | Effect |
| --- | --- |
| `/ee` or `/ee help` | Help; `/energyexchange` is the full prefix |
| `/ee value` | Show the default main-hand item's unit value |
| `/ee burn` | Convert one main-hand item and automatically learn it |
| `/ee burn 32` | Convert 32 main-hand items |
| `/ee burn all` | Convert the main-hand stack, **not the whole inventory** |
| `/ee learn` | Consume one sample to learn it without earning Energy; known items consume nothing |
| `/ee balance` | Show the exact balance |
| `/ee list [page]` | List learned item IDs, 12 per page |
| `/ee buy minecraft:cobblestone 16` | Buy 16 learned items; count defaults to 1; Tab completes learned IDs |

Example: hold 64 cobblestone and use `/ee burn all` to earn 64 Energy and learn cobblestone. `/ee buy minecraft:cobblestone 16` returns 16 cobblestone and leaves 48 Energy.

## Limits and safety

- Exact `BigInteger` arithmetic from `0` through `10^128−1`. No floating point, truncation or wraparound; overflow rejects the entire transaction.
- The server determines prices, balance, knowledge, held stacks and capacity. No custom packet accepts client balances or item stacks.
- Burning/learning requires components identical to a fresh default stack. Names, enchantments, damage, written content, potion variants, container contents and custom data are unsupported. Purchases produce only default stacks.
- Missing/disabled rules prohibit burning, learning and buying. Twenty basic resources have starter values; no recipe inference is performed. Pack authors must audit cross-mod crafting/machine arbitrage.
- Purchases accept 1–2304 items and must fit entirely in the 36 main inventory/hotbar slots. Insertion is simulated first; insufficient space spends nothing and drops nothing. Armor, offhand and external containers are excluded; close containers before trading.
- Personal balances and knowledge use the persistent player attachment `energyexchange:account`. They survive death, dimension changes, reconnects and normal restarts; wallets are not shared.
- Knowledge is capped at 1024 item IDs and serialized accounts at 60,000 characters. Corrupt/unsupported account data locks trading and preserves the original string. Back up before removing the mod; saving while it is absent may discard attachments.
- Trading pauses during reload and stays locked after a failed reload until a corrected `/reload` succeeds. Removed rules do not erase knowledge; restoring rules enables exchange again.
- Persistence follows Minecraft's normal player saves, not a per-transaction disk journal. A crash may roll back to the last player save. Administrators and other mods that modify items, saves or creative mode are outside the trust boundary.

## Documentation and license

[Data-pack format and overrides](docs/DATAPACKS.en.md) · [Development, tests and roadmap](docs/DEVELOPMENT.en.md) · [MIT license](LICENSE)
