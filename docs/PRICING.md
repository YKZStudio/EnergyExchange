# Optional-mod pricing

[简体中文](PRICING_zh-CN.md) · [Home](../README.md)

Run these commands in order to reproduce prices:

```sh
python tools/generate_values.py client.jar /path/to/tacz
python tools/generate_mod_values.py client.jar /path/to/tacz travelersbackpack.jar
python tools/generate_armory.py
```

Use the pinned 26.2 vanilla client, TaCZ source commit `05ec226310545d89cec9169a10105e32d1ed262b`, and Traveler's Backpack Fabric 26.2-11.3.2. The backpack archive is checked with SHA-256. Tools extract item, tag and recipe facts; no upstream code/assets are bundled.

The ordinary recipe graph uses the cheapest resolved ingredient alternative and output count. Identical-input copy recipes are solved algebraically. Purchase prices round up; conversion rates stay fractional and round down once per batch. Each profile asserts all resolved recipe constraints. Conditional backpack recipe variants are conservatively priced at the cheapest available definition. Optional Comforts recipes are skipped because Comforts is not part of this compatibility target.

Active profiles are `energyexchange/compat/tacz.json`, `travelersbackpack.json` and `tacz-travelersbackpack.json` under `data/energyexchange`. Each contains `values` and `salvage` maps with the same bounds as defaults. They only load when matching mods are present. Individual `values/` and `variants/` data-pack overrides apply afterward and remain authoritative. These are startup/reload profiles, not live recalculation of arbitrary third-party machine recipes.

| Example | 0.2 price | 0.2.1 combined purchase / conversion |
| --- | ---: | ---: |
| TaCZ AK-47 | 11584 | 11432 / 11432 |
| TaCZ 7.62×39 ammunition | 71 | 8 / 278÷35 |
| Gunpowder with TaCZ | 192 | 58 / 514÷9 |
| Standard Traveler's Backpack | unavailable | 384 / 384 |
| Diamond tier upgrade | unavailable | 65616 / 65616 |

Firearms/attachments with recipes reflect their material cost; unpriced addon models use documented base fallbacks. Metadata never increases value. Buying a data-bearing sample's identity produces a fresh default item. Additional gun packs, custom recipes and machine/villager cycles still require server-owner overrides.
