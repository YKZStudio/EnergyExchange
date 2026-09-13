#!/usr/bin/env python3
"""Generate additive ENERGY profiles from pinned optional-mod item models and recipes.

Usage: python tools/generate_ecosystem_values.py client.jar modid=mod.jar [...]
Set EE_RESOURCE_ROOT for a version overlay. Run after the vanilla/mod generators.
Only generated JSON is shipped; upstream code and assets are never copied.
"""
import json, os, re, sys, zipfile
from fractions import Fraction
from math import ceil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = Path(os.environ.get("EE_RESOURCE_ROOT", ROOT / "src/main/resources")) / "data/energyexchange/energyexchange"
if len(sys.argv) < 3 or any("=" not in arg for arg in sys.argv[2:]):
    raise SystemExit(__doc__)
modules = [(arg.split("=", 1)[0], Path(arg.split("=", 1)[1])) for arg in sys.argv[2:]]
base = json.loads((OUT / "defaults.json").read_text())
salvage_path = OUT / "salvage.json"
if not salvage_path.exists():
    salvage_path = ROOT / "src/main/resources/data/energyexchange/energyexchange/salvage.json"
salvage = json.loads(salvage_path.read_text())
rates = {key: Fraction(*map(int, salvage[key])) if key in salvage else Fraction(value) for key, value in base.items()}
resources = {}
items = {}

def add_archive(path, module=None):
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if name.startswith("data/") and name.endswith(".json"):
                try:
                    resources[name] = json.loads(archive.read(name))
                except (json.JSONDecodeError, UnicodeDecodeError):
                    pass
            if module and re.fullmatch(rf"assets/{re.escape(module)}/items/[^/]+\.json", name):
                items.setdefault(module, set()).add(f"{module}:{Path(name).stem}")

add_archive(Path(sys.argv[1]))
for module, path in modules:
    add_archive(path, module)

def fallback(key):
    name = key.split(":", 1)[1]
    if name.endswith("_spawn_egg"): return Fraction(4096)
    if any(x in name for x in ("aeternium", "terminite", "thallasium", "flaming_ruby")): return Fraction(4096)
    if name.endswith(("_helmet", "_chestplate", "_leggings", "_boots", "_sword", "_pickaxe", "_axe", "_shovel", "_hoe", "_spear", "_knife")): return Fraction(8192)
    if name.endswith(("_ingot", "_gem", "_crystal", "_ruby", "_diamond")): return Fraction(1024)
    if name.endswith(("_nugget", "_shard")): return Fraction(128)
    if name.endswith(("_ore", "_block", "_anvil", "_furnace", "_smelter")): return Fraction(4096)
    if name.endswith(("_log", "_wood", "_stem", "_hyphae", "_planks")): return Fraction(32)
    if name.endswith(("_leaves", "_sapling", "_seed", "_seeds", "_flower", "_mushroom", "_grass")): return Fraction(8)
    if any(x in name for x in ("soup", "stew", "salad", "cake", "pie", "sandwich", "burger", "coffee", "tea", "juice", "meal", "rice", "noodle")): return Fraction(64)
    return Fraction(256)

for module_items in items.values():
    for key in module_items:
        rates.setdefault(key, fallback(key))

aliases = {
    "c:strings": "minecraft:string", "c:leathers": "minecraft:leather", "c:eggs": "minecraft:egg",
    "c:feathers": "minecraft:feather", "c:bones": "minecraft:bone", "c:rods/wooden": "minecraft:stick",
    "c:rods/blaze": "minecraft:blaze_rod", "c:gunpowders": "minecraft:gunpowder",
    "c:ender_pearls": "minecraft:ender_pearl", "c:slime_balls": "minecraft:slime_ball",
    "c:dusts/redstone": "minecraft:redstone", "c:dusts/glowstone": "minecraft:glowstone_dust",
    "c:gems/lapis": "minecraft:lapis_lazuli", "c:gems/quartz": "minecraft:quartz",
    "c:crops/wheat": "minecraft:wheat", "c:crops/carrot": "minecraft:carrot",
    "c:crops/potato": "minecraft:potato", "c:crops/beetroot": "minecraft:beetroot",
    "c:foods/bread": "minecraft:bread", "c:foods/milk": "minecraft:milk_bucket",
    "c:buckets/water": "minecraft:water_bucket", "c:buckets/lava": "minecraft:lava_bucket",
    "c:cobblestones": "minecraft:cobblestone", "c:stones": "minecraft:stone",
    "c:obsidians": "minecraft:obsidian", "c:glass_blocks": "minecraft:glass",
    "c:glass_blocks/colorless": "minecraft:glass", "c:glass_panes": "minecraft:glass_pane",
    "c:glass_panes/colorless": "minecraft:glass_pane", "c:chests": "minecraft:chest",
    "c:chests/wooden": "minecraft:chest", "c:player_workstations/crafting_tables": "minecraft:crafting_table",
}
unknown_tags = set()

def ingredient(value, seen=frozenset()):
    if isinstance(value, list):
        result = set()
        for part in value: result.update(ingredient(part, seen))
        return result
    if isinstance(value, dict):
        if "tag" in value: return ingredient("#" + value["tag"], seen)
        return ingredient(value.get("item", value.get("id", "")), seen)
    if not isinstance(value, str) or not value: return set()
    if not value.startswith("#"): return {value} if value in rates else set()
    tag = value[1:]
    if tag in seen: return set()
    namespace, path = tag.split(":", 1)
    for folder in ("tags/item", "tags/items"):
        data = resources.get(f"data/{namespace}/{folder}/{path}.json")
        if data:
            result = set()
            for part in data.get("values", []): result.update(ingredient(part, seen | {tag}))
            if result: return result
    direct = aliases.get(tag)
    if not direct and re.fullmatch(r"c:(ingots|nuggets|gems|dyes)/[a-z_]+", tag):
        group, material = tag[2:].split("/")
        direct = "minecraft:" + material + {"ingots":"_ingot", "nuggets":"_nugget", "gems":"", "dyes":"_dye"}[group]
    if direct in rates: return {direct}
    unknown_tags.add(tag)
    return set()

def outputs(recipe):
    value = recipe.get("result")
    if isinstance(value, list): raw = value
    else: raw = [value]
    result = []
    for entry in raw:
        if isinstance(entry, str): result.append((entry, Fraction(1))); continue
        if not isinstance(entry, dict): continue
        item = entry.get("id", entry.get("item"))
        count = entry.get("count", 1)
        chance = entry.get("chance", 1)
        if isinstance(item, dict):
            count = item.get("count", count); item = item.get("id", item.get("item"))
        if isinstance(item, str): result.append((item, Fraction(str(chance)) * int(count)))
    return result

def inputs(recipe):
    kind = recipe.get("type", "").split(":")[-1]
    if kind == "crafting_shaped":
        return [recipe["key"][char] for row in recipe.get("pattern", []) for char in row if char != " "]
    if kind == "crafting_shapeless": return recipe.get("ingredients", [])
    if kind in ("smelting", "blasting", "smoking", "campfire_cooking", "stonecutting"):
        return [recipe.get("ingredient")]
    if kind == "smithing_transform": return [recipe.get(key) for key in ("template", "base", "addition")]
    if kind in ("cooking", "cutting", "alloying", "baking_mat", "dough", "food_serving"):
        return recipe.get("ingredients", [])
    if kind == "infusion": return [recipe.get("input")] + list(recipe.get("catalysts", {}).values())
    if kind == "smithing" and "input" in recipe: return [recipe["input"]]
    return []

target_namespaces = {module for module, path in modules}
constraints = []
for name, recipe in sorted(resources.items()):
    if "/recipe/" not in name or not isinstance(recipe, dict): continue
    if "/cutting/" in name and "feast" in name: continue
    produced, consumed = outputs(recipe), inputs(recipe)
    # Multi-product salvage/cutting recipes need a joint allocation; forward
    # crafting already constrains their component prices, so do not double-count.
    if len({key for key, count in produced}) > 1: continue
    groups = [ingredient(part) for part in consumed if part is not None]
    if not produced or not groups or not all(groups): continue
    total_units = sum((count for key, count in produced if key in rates), Fraction())
    if total_units <= 0: continue
    kind = recipe.get("type", "").split(":")[-1]
    for key, count in produced:
        if key in rates and count > 0:
            unpacking = len(groups) == 1 and total_units > 1
            item_name = key.split(":", 1)[1]
            natural = item_name.endswith(("_log", "_wood", "_bark", "_stem", "_hyphae", "_leaves", "_sapling", "_seed", "_seeds", "_flower", "_mushroom", "_grass"))
            if key.split(":", 1)[0] in target_namespaces and not unpacking and not natural and kind != "cutting":
                rates[key] = max(rates[key], Fraction(1_048_576))
            constraints.append((key, total_units, groups, name))

for iteration in range(512):
    changed = False
    changed_keys = []
    for output, total_units, groups, source in constraints:
        self_inputs = sum(group == {output} for group in groups)
        if total_units <= self_inputs: continue
        cost = sum(min(rates[key] for key in group) for group in groups if group != {output}) / (total_units - self_inputs)
        if 0 < cost < rates[output]: rates[output] = cost; changed = True; changed_keys.append((output, source, cost))
    if not changed: break
else:
    raise ValueError("Recipe pricing did not converge: " + repr(changed_keys[:8]))

for output, total_units, groups, source in constraints:
    if output.split(":", 1)[0] in target_namespaces:
        assert rates[output] * total_units <= sum(min(rates[key] for key in group) for group in groups), source

module_dir = OUT / "compat/modules"
module_dir.mkdir(parents=True, exist_ok=True)
for module, path in modules:
    values = {key: str(ceil(rates[key])) for key in sorted(items[module])}
    fractions = {key: [str(rates[key].numerator), str(rates[key].denominator)] for key in sorted(items[module]) if rates[key].denominator != 1}
    (module_dir / f"{module}.json").write_text(json.dumps({"values": values, "salvage": fractions}, indent=2) + "\n")
    print(module, len(values), "items", len(fractions), "fractional")
print("Resolved constraints", len(constraints), "unresolved tags", len(unknown_tags))
if unknown_tags: print("Unresolved:", ", ".join(sorted(unknown_tags)))
