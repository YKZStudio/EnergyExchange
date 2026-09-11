# Development and validation

[简体中文](DEVELOPMENT.zh-CN.md) · [Home](../README.en.md)

## Toolchain

Java 25, Gradle Wrapper 9.5.1, Loom 1.17.12, Fabric Loader 0.19.5 and Fabric API 0.159.0+26.2. Dependencies are pinned in `gradle.properties`. Minecraft 26.2 uses official unobfuscated names and `net.fabricmc.fabric-loom`; no Yarn, legacy `modImplementation` or `remapJar` configuration is used.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

Read and accept the Minecraft EULA yourself before starting a development server. Use `gradlew.bat` on Windows. Configure any required proxy locally; do not commit personal proxy settings, credentials or machine paths.

## Structure and extension rules

| Module | Responsibility |
| --- | --- |
| `core/Energy` | Decimal parsing, multiplication and balance limits |
| `core/Account` | Immutable account and pure transactions |
| `core/AccountJson` | Versioned saves; corrupt/future data locks transactions and preserves the original |
| `core/ValueRule` | Individual rule parsing |
| `Rules` | Load priority-resolved resources, immutable snapshots and reload lock |
| `ExchangeService` | Server player, component, count and capacity validation; mutations |
| `ExchangeCommands` / `Messages` | Non-OP command entry points and client-localized messages |

Persistence uses the player's `energyexchange:account` attachment, containing a JSON string with `schema:1`, string `energy` and a `learned` ID array. A string Codec prevents business-level decoding errors from causing Fabric to discard the old account. Values are immutable and updated through `setAttached`; `copyOnDeath` retains them, and accounts are not broadcast to other players.

Transaction order: obtain current server state → validate rules/components → calculate the next account → validate serialization → simulate insertion → update inventory/account on the same server thread → synchronize inventory. Failure paths must not debit items or Energy first. Never await asynchronously inside a transaction. The GUI invokes the same service through bounded requests with session nonces, sequences, quote revisions and rate limiting.

These are all-or-nothing in-memory operations; persistence follows Minecraft player saves, not a database journal. Cross-system atomicity is not guaranteed against other mods throwing exceptions/mutating inventories, administrator save edits or power-loss rollback.

## Validation scope

JUnit covers the pure full loop, balances beyond floating-point/long precision, caps, invalid inputs, unknown knowledge, insufficient funds, knowledge limits, save round trips/corruption/future versions and invalid/disabled rules. CI compiles against actual Minecraft/Fabric dependencies and produces a JAR. GameTests start a disposable server and verify the command loop, isolated wallets, inventory preflight, overflow, component rejection, game modes, NBT persistence, death copying, corrupt-account protection and reload snapshots. `build` includes server GameTests; test configuration accepts the EULA for the disposable server and does not touch real worlds.

Before release, complete this manual checklist in a disposable 26.2 world. Compilation and unit-test success do not mean these scenarios have been manually verified:

- Two non-OP players independently burn, learn and buy; wallets remain isolated and cheats are unnecessary.
- After transactions, verify death/respawn, dimension changes, reconnects and normal restart after `save-all` preserve balance/knowledge.
- Full inventory with merge space, no space, and max-stack-size-one items; failed purchases spend nothing and drop nothing.
- Reject named/damaged/enchanted items, filled containers, potion variants and custom components; repeated learning consumes nothing.
- Reject trading while containers are open or the player is dead/creative/spectator; repeated commands settle against current inventory/balance only.
- Competing data packs for the same ID; disable/remove/restore rules; invalid JSON/huge numbers; failed reload locks trading and a corrected reload restores it.
- Simplified Chinese, English, modded client menus, Tab completion and pagination.

## Roadmap

0.2 adds tables, a localized search UI, private catalog synchronization, XP purchases and optional TaCZ/Mod Menu adapters. Later work may cover richer component variants, live recipe auditing and economy logs. Do not default to arbitrary component copying, creative-mode exchange, cyclic pricing or unbounded numbers.

References: [Fabric 26.2](https://fabricmc.net/2026/06/15/262.html), [data attachments](https://docs.fabricmc.net/develop/serialization/data-attachments), [automated testing](https://docs.fabricmc.net/develop/automatic-testing), [official example](https://github.com/FabricMC/fabric-example-mod/tree/26.2).
