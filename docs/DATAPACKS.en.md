# Data-pack rules

[简体中文](DATAPACKS.zh-CN.md) · [Home](../README.en.md)

Use one file per item: `data/<item namespace>/energyexchange/values/<item path>.json`. For example, `minecraft:diamond` uses `data/minecraft/energyexchange/values/diamond.json`; `othermod:materials/dust` uses `data/othermod/energyexchange/values/materials/dust.json`.

Enabled value:

```json
{ "value": "8192" }
```

Disabled rule:

```json
{ "enabled": false }
```

| Field | Contract |
| --- | --- |
| `value` | Required when enabled. A decimal **string**, from 1 through `10^128−1`; numeric JSON values, fractions, exponents, signs, whitespace and leading zeros are rejected |
| `enabled` | Optional boolean, defaults to `true`. A disabled rule must omit `value` |

Unknown fields are rejected. Limits: 4096 characters per file, 16384 rule resources and 256 characters per full item ID. Use UTF-8 JSON. IDs belonging to absent mods may stay in packs, but purchases require a registered, feature-enabled item.

## Overrides and reload

1. Copy the entire [example pack folder](../examples/value-overrides) into `<world>/datapacks/`. Its `pack.mcmeta` must be at the folder root.
2. Edit the cobblestone value. The example also disables diamond.
3. As an administrator, run `/reload`; use `/datapack list` if needed to check activation.
4. Hold cobblestone and run `/ee value`. To restore a default, delete its override and reload. To prohibit a lower-priority default, use `enabled:false`; merely deleting an override exposes the default again.

Minecraft's normal pack priority selects the file at each **resource location**. The winning file replaces the whole lower-priority file; fields are not merged and traversal order does not select a price. Other item rules are unaffected. v0.1 has no tag-based pricing or recipe inference.

The loader parses the complete resource set before publishing an immutable snapshot. Trading pauses during reload. Any invalid rule fails the reload and keeps trading locked until a corrected reload succeeds. Invalid startup rules behave like ordinary data-pack load errors; errors never become free items.

Knowledge is independent of prices. Disabling a rule preserves learned IDs while prohibiting further burning, learning or buying. New prices affect future transactions only; balances are not retroactively adjusted. Server owners should avoid uncontrolled price-change arbitrage and audit their packs and other mods' recipes.

## Component policy

Rules specify item IDs only and cannot relax the default-component restriction. Pricing shulker boxes, potions, tools or books still only accepts components equal to a fresh default stack and produces only that default. Arbitrary component exchange needs a separate future design; fields such as `allow_nbt` are unsupported.
