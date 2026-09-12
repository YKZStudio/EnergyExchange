# Matter and Infinity armory

[简体中文](ARMORY_zh-CN.md) · [Home](../README.md)

Version 0.3 adds 32 items: six materials, three tiers of swords/pickaxes/axes/shovels and four-piece armor, plus the Red Matter Katar and Morning Star. This is an independent Fabric implementation inspired by ProjectE and Avaritia, not either complete mod.

## Controls and effects

- All gear is unbreakable and fire-resistant as dropped items. Normal left-click mining/combat works normally; high mining speed and weapon attributes use vanilla mechanics.
- Right-click a block with a mining tool: mine a plane aligned to the clicked face. Dark Matter: 3×3; Red Matter: 5×5; Infinity: 7×7. Sneak-right-click mines one block. Cooldown: 10 ticks. The Morning Star combines pickaxe/shovel mining; the Katar also mines axe blocks.
- Right-click air with a sword or Katar: sweep visible hostile mobs within 3/5/7 blocks. Cooldown: 20 ticks, up to 64 successful hits. Sweep damage is 12/24/1024. Area attacks exclude players, passive animals and pets; ordinary melee still follows vanilla PvP rules. Sword base total attack damage is 16/24/1024 before enchantments/effects.
- Dark Matter full suit: fire resistance. Red Matter full suit: fire resistance, water breathing and night vision. Mixing tiers grants the lowest tier present; missing a piece disables full-set bonuses.
- Infinity full suit: the above, flight, regeneration II and food replenishment, plus prevention of ordinary damage. Double-tap jump to fly. Void and `/kill` remain effective; this does not bypass other mods' invulnerability rules or use infinite floating-point damage.
- Unequipping any infinity piece revokes only flight granted by this mod; already granted creative/external flight is preserved. A flying player receives 5 seconds of slow falling when that permission is removed. Death/disconnect clears owned flight. Short potion bonuses expire naturally; night vision can linger for up to 12 seconds to avoid flicker.
- Area mining only operates in Survival, within initial interaction range, in already loaded chunks and within the world border. It honors spawn protection, vanilla restrictions and Fabric block-break callbacks. Containers/block entities, unsuitable blocks and unbreakable blocks are skipped. Each use visits at most 49 blocks; normal loot and enchantments apply. No chunk loading, synthetic loot or direct health setting is used.

## Crafting progression

Every recipe below uses the vanilla 3×3 crafting table. Obtain the first listed material to reveal the recipe in the recipe book. No new workbench or machine is required. Prices follow recipe costs in each installed-mod profile, with exact fractional conversion rules. Craft a new identity once, convert it to learn, then use your Energy as usual. Buying equipment returns its default state, as in 0.2.1.

The table shows baseline purchase prices; installed mods or datapacks can change them. Pattern rows are separated by `/`; `_` means empty. Shapeless recipes can occupy any cells. All recipes output one item.

| Item | 3×3 recipe | Ingredient key / shapeless ingredients | ENERGY |
| --- | --- | --- | ---: |
| Crystal Matrix Ingot | `DED / ESE / DED` | D = minecraft:diamond_block; E = minecraft:emerald_block; S = minecraft:nether_star | 1146880 |
| Dark Matter | `CCC / CDC / CCC` | C = minecraft:coal_block; D = minecraft:diamond_block | 82944 |
| Dark Matter Axe | `MM_ / MR_ / _R_` | M = Dark Matter; R = minecraft:blaze_rod | 251904 |
| Dark Matter Boots | `M_M / M_M` | M = Dark Matter | 331776 |
| Dark Matter Chestplate | `M_M / MMM / MMM` | M = Dark Matter | 663552 |
| Dark Matter Helmet | `MMM / M_M` | M = Dark Matter | 414720 |
| Dark Matter Leggings | `MMM / M_M / M_M` | M = Dark Matter | 580608 |
| Dark Matter Pickaxe | `MMM / _R_ / _R_` | M = Dark Matter; R = minecraft:blaze_rod | 251904 |
| Dark Matter Shovel | `_M_ / _R_ / _R_` | M = Dark Matter; R = minecraft:blaze_rod | 86016 |
| Dark Matter Sword | `_M_ / _M_ / _R_` | M = Dark Matter; R = minecraft:blaze_rod | 167424 |
| Nature's Ruin | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Axe | 390331896 |
| Infinity Boots | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Boots | 390411768 |
| Infinity Catalyst | `Shapeless` | Dark Matter, Red Matter, Crystal Matrix Ingot, Neutronium Ingot, minecraft:nether_star, minecraft:dragon_breath, minecraft:heart_of_the_sea, minecraft:echo_shard, minecraft:netherite_block | 7988415 |
| Infinity Chestplate | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Chestplate | 390743544 |
| Infinity Helmet | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Helmet | 390494712 |
| Infinity Ingot | `NNN / NCN / NNN` | N = Neutronium Ingot; C = Infinity Catalyst | 47834303 |
| Infinity Pants | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Leggings | 390660600 |
| World Breaker | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Pickaxe | 390331896 |
| Planet Eater | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Shovel | 390166008 |
| Sword of the Cosmos | `MMM / MGM / MMM` | M = Infinity Ingot; G = Red Matter Sword | 390247416 |
| Neutronium Ingot | `NNN / NSN / NNN` | N = minecraft:netherite_block; S = minecraft:nether_star | 4980736 |
| Red Matter | `DDD / DSD / DDD` | D = Dark Matter; S = minecraft:nether_star | 925696 |
| Red Matter Axe | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Axe | 7657472 |
| Red Matter Boots | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Boots | 7737344 |
| Red Matter Chestplate | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Chestplate | 8069120 |
| Red Matter Helmet | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Helmet | 7820288 |
| Red Matter Katar | `Shapeless` | Red Matter Sword, Red Matter Axe, Red Matter, Red Matter | 17081856 |
| Red Matter Leggings | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Leggings | 7986176 |
| Red Matter Morning Star | `Shapeless` | Red Matter Pickaxe, Red Matter Shovel, Red Matter, Red Matter | 17000448 |
| Red Matter Pickaxe | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Pickaxe | 7657472 |
| Red Matter Shovel | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Shovel | 7491584 |
| Red Matter Sword | `MMM / MGM / MMM` | M = Red Matter; G = Dark Matter Sword | 7572992 |

## Advanced tooltips

Press F3+H to enable advanced tooltips. `ENERGY <exact purchase unit value>` appears immediately above the registry item ID, for both vanilla and modded items. Sample metadata does not increase this value. `ENERGY —` means no available server price (including disabled items, incomplete synchronization or failed reload). Prices synchronize on join and datapack reload, without sharing wallets or learned items. Normal tooltips remain unchanged.

## Appearance and implementation differences

MIT-licensed ProjectE and Re-Avaritia item/armor textures and animation metadata are retained, including the recognizable Infinity tool shapes. The vanilla equipment renderer replaces their custom armor renderers; this version does not reproduce the Avaritia cosmic shader, custom wings, creative-player killing or bedrock removal. See [asset provenance](armory-assets.json) and [third-party notices](../THIRD_PARTY_NOTICES.md).
