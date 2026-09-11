# Data-pack rules

[简体中文](DATAPACKS_zh-CN.md) · [Home](../README.md)

## Item overrides

One file per item: `data/<item namespace>/energyexchange/values/<item path>.json`.

Examples: `minecraft:diamond` uses `data/minecraft/energyexchange/values/diamond.json`; `othermod:materials/dust` uses `data/othermod/energyexchange/values/materials/dust.json`.

```json
{ "value": "8192" }
```

A positive decimal string sets both the purchase unit and conversion unit. An explicit item override replaces any generated fractional conversion yield for that item. Disable an item with:

```json
{ "enabled": false }
```

`enabled` is an optional boolean, defaulting to true. An enabled rule requires `value`; a disabled rule must omit it. Values range from 1 through `10^128−1`. Numeric JSON values, signs, leading zeros, exponents, whitespace, duplicate keys, comments and unknown fields are rejected. Maximum 4,096 characters per individual file; maximum 16,384 item rules and 16,384 variant rules; maximum 256 characters per full knowledge key.

## TaCZ and LRTactical variants

Knowledge keys combine the registered item and the model: `tacz:modern_kinetic_gun#tacz:ak47`.

A per-model override lives at:

`data/<model namespace>/energyexchange/variants/<base namespace>/<base path>/<model path>.json`

Example: `data/tacz/energyexchange/variants/tacz/modern_kinetic_gun/ak47.json`, using the same `value` or `enabled:false` schema. For model `mypack:rifles/example`, use `data/mypack/energyexchange/variants/tacz/modern_kinetic_gun/rifles/example.json`.

Base paths for this variant format are a single path segment, matching TaCZ/LRTactical registered items. Model paths can contain subdirectories. Disabling the base item disables every model, even models with explicit prices. Otherwise an explicit model rule/default wins; additional loaded models without a specific price fall back to the base item's rule. A base price does not overwrite existing specific model prices. Absent mods/models are not added to the runtime catalog merely because a price file exists.

## Defaults and fractional conversion

Bundled `data/energyexchange/energyexchange/defaults.json` maps full item/model keys to positive decimal strings. It has 1,738 explicit keys, including 1,537 vanilla IDs. `salvage.json` maps fractional conversion keys to `["numerator", "denominator"]`. Both files support normal whole-resource pack replacement, with a 2,000,000-character limit; normally prefer small individual override files.

Generated purchase units round up; conversion batches compute `floor(numerator × count / denominator)` using exact integers. A batch yielding zero is rejected without consuming items or learning the identity. Fractions never enter saved balances. Conversion rates cannot exceed purchase prices. If replacing both bulk resources, keep their keys and bounds consistent. A malformed winning rule locks trading; the loader does not silently fall back to a lower pack.

Defaults are an authored balance baseline constrained by supported vanilla recipes at generation time, not a runtime recipe solver. Editing recipes with another pack does not automatically reprice them. Special component-bearing recipes, brewing, villagers and other mods' machines need a separate economic audit. The generator reads official item IDs/recipe facts and TaCZ index/recipe facts, without copying upstream code or assets.

## Overrides and reload

1. Copy the entire [example pack](../examples/value-overrides) into `<world>/datapacks/`; `pack.mcmeta` belongs at its root. Minecraft 26.2 uses data-pack format 107.1.
2. Edit the cobblestone price; the example also disables diamond.
3. As an administrator run `/reload`, and use `/datapack list` to confirm activation.
4. Hold an item and use `/ee value`, or reopen the table and inspect its tooltip.

Minecraft pack priority chooses the winning resource at each location. Individual item/variant resources override the bulk defaults. Fields are not merged. Deleting an override reveals the default again; use `enabled:false` to prohibit it.

Reload pauses every transaction, including XP. Failure remains locked until a successful corrected reload. Menus reject stale rule revisions and refresh their catalog before another purchase; current input stacks and saved knowledge remain intact. Disabling/removing a model preserves knowledge but blocks exchange until that model and its rule return. Price changes affect future transactions without altering existing balances.

## Component and server settings policy

Prices do not grant permission to copy arbitrary components. Vanilla uses default stacks; TaCZ/LRTactical uses independently built standard prototypes. Extra ammunition, attachments, renamed items, written data, damage and filled containers remain rejected. `allow_nbt` and similar flags are unsupported.

XP enablement and cost are server configuration, not data-pack rules. Edit `config/energyexchange.json` and restart the world/server. Mod Menu edits the local defaults for the next server start, not a remote server's economy.
