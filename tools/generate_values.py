"""Reproduce starter prices from 26.2 item IDs and the selected TaCZ checkout.
根据 26.2 物品 ID 和指定 TaCZ 仓库生成可复现的初始价格。
Usage: python tools/generate_values.py client.jar /path/to/tacz
"""
import json, sys, zipfile, re
from fractions import Fraction
from math import ceil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
values = {}
anchors = dict(cobblestone=1, dirt=1, stone=1, gravel=1, sand=1, netherrack=1,
               rotten_flesh=1, oak_log=32, oak_planks=8, stick=4, coal=128,
               charcoal=128, iron_ingot=256, gold_ingot=2048, diamond=8192,
               emerald=16384, redstone=64, lapis_lazuli=256, quartz=256,
               copper_ingot=128, gunpowder=192, netherite_ingot=65536)

def price(name):
    if name in anchors: return anchors[name]
    if name in {'air'}: return 1
    if any(x in name for x in ['command_block', 'structure_block', 'structure_void', 'barrier', 'jigsaw', 'debug_stick', 'spawn_egg', 'bedrock', 'light']): return 1000000
    if name in {'elytra','dragon_egg','nether_star','heavy_core'}: return 262144
    if name in {'totem_of_undying','enchanted_golden_apple','mace','trident'}: return 65536
    for material, cost in [('netherite',65536),('diamond',8192),('golden',2048),('iron',256),('copper',128),('stone',4),('wooden',8)]:
        if name.startswith(material+'_'):
            for kind, n in [('pickaxe',3),('axe',3),('sword',2),('shovel',1),('hoe',2),('helmet',5),('chestplate',8),('leggings',7),('boots',4)]:
                if name.endswith('_'+kind): return cost*n+8
    for material,cost in [('netherite',65536),('diamond',8192),('gold',2048),('iron',256),('copper',128),('emerald',16384),('coal',128),('redstone',64),('lapis',256)]:
        if name==material+'_block': return cost*9
        if name==material+'_nugget': return max(1,cost//9)
        if name in {material+'_ore','deepslate_'+material+'_ore','raw_'+material}: return cost
        if name=='raw_'+material+'_block': return cost*9
    if name.endswith(('_log','_wood','_stem','_hyphae')): return 32
    if name.endswith('_planks'): return 8
    if name.endswith('_chest_boat'): return 104
    if name.endswith('_boat'): return 40
    if name.endswith(('_slab','_stairs','_wall','_concrete','_terracotta')): return 4
    if name.endswith(('_leaves','_sapling','_flower','_seeds','_coral','_coral_fan')): return 16
    if name.endswith(('_dye','_wool','_carpet')): return 32
    if name.endswith('_banner'): return 196
    if name.endswith('_bed'): return 120
    if name.endswith('_shulker_box'): return 4160
    if name.endswith('_bucket'): return 1024
    if name.endswith('_smithing_template'): return 57344
    if name.endswith('_pottery_sherd'): return 1024
    if name.endswith('_music_disc') or name.startswith('music_disc_'): return 4096
    return 64

with zipfile.ZipFile(sys.argv[1]) as jar:
    for path in jar.namelist():
        if path.startswith('assets/minecraft/items/') and path.endswith('.json'):
            name=path.removeprefix('assets/minecraft/items/').removesuffix('.json')
            values['minecraft:'+name]=str(price(name))

checkout=Path(sys.argv[2]); base=checkout/'src/main/resources/assets/tacz/custom'
types={'gun':('modern_kinetic_gun',32768),'ammo':('ammo',32),'attachment':('attachment',4096),'block':('gun_smith_table',4096)}
for folder, (item, default) in types.items():
    for f in base.glob(f'**/data/*/index/{folder}/**/*.json'):
        parts=f.parts; i=parts.index('data'); ns=parts[i+1]
        id=ns+':'+str(f.relative_to(Path(*parts[:i+4]))).removesuffix('.json')
        values['tacz:'+item+'#'+id]=str(default)

tags={'#c:ingots/iron':256,'#c:ingots/gold':2048,'#c:ingots/copper':128,'#c:gems/lapis':256,
      '#c:gems/diamond':8192,'#c:gunpowders':192,'#minecraft:logs':32,'#minecraft:planks':8}
for f in base.glob('**/data/*/recipe/**/*.json'):
    obj=json.loads(f.read_text()); result=obj.get('result',{}); kind=result.get('type')
    if kind not in types or not result.get('id'): continue
    cost=sum((tags.get(m.get('item'),int(values.get(m.get('item'),'256'))))*m.get('count',1) for m in obj.get('materials',[]))
    if cost: values['tacz:'+types[kind][0]+'#'+result['id']]=str(max(1,cost//result.get('count',1)))
for name in ['modern_kinetic_gun','ammo','attachment','gun_smith_table','workbench_a','workbench_b','workbench_c','target','statue','ammo_box','target_minecart']:
    values['tacz:'+name]=str({'modern_kinetic_gun':32768,'ammo':32,'attachment':4096}.get(name,4096))
for name,cost in [('throwable',2048),('melee',8192),('consumable',1024),('detonator',4096)]: values['lrtactical:'+name]=str(cost)
# Read only registry/index/recipe facts; upstream code and assets are not copied.
# 仅读取注册、索引及配方事实，不复制上游代码和素材。
for f in checkout.glob('src/main/resources/**/data/*/index/blocks/**/*.json'):
    text=re.sub(r'//[^\n]*','',f.read_text()); obj=json.loads(text)
    parts=f.parts;i=parts.index('data');ns=parts[i+1]
    variant=ns+':'+str(f.relative_to(Path(*parts[:i+4]))).removesuffix('.json')
    item=obj.get('id','tacz:gun_smith_table')
    values[item if item=='tacz:gun_smith_table' else item+'#'+variant]='4096'
for kind,item,cost in [('throwable','throwable',2048),('melee','melee',8192),('consumable','consumable',1024)]:
    for f in checkout.glob(f'src/main/resources/**/data/*/index/{kind}/**/*.json'):
        parts=f.parts;i=parts.index('data');ns=parts[i+1]
        variant=ns+':'+str(f.relative_to(Path(*parts[:i+4]))).removesuffix('.json')
        values['lrtactical:'+item+'#'+variant]=str(cost)
values.update({'energyexchange:transmutation_table':'24960','energyexchange:transmutation_tablet':'16640'})
# Relax ordinary vanilla recipes to avoid cheap-craft -> expensive-burn loops.
# 对普通原版配方做价格约束，避免低价合成后高价分解。
rates={k:Fraction(v) for k,v in values.items()}
recipes=[]
with zipfile.ZipFile(sys.argv[1]) as jar:
    names=set(jar.namelist())
    def ingredients(x,seen=frozenset()):
        if isinstance(x,list): return set().union(*(ingredients(v,seen) for v in x))
        if isinstance(x,dict): return ingredients(x.get('item', '#'+x['tag'] if 'tag' in x else ''),seen)
        if not isinstance(x,str): return set()
        if not x.startswith('#'): return {x} if x in rates else set()
        if x in seen: return set()
        ns,path=x[1:].split(':',1); file=f'data/{ns}/tags/item/{path}.json'
        if file not in names: return set()
        return set().union(*(ingredients(v,seen|{x}) for v in json.loads(jar.read(file)).get('values',[])))
    for file in names:
        if not file.startswith('data/minecraft/recipe/') or not file.endswith('.json'): continue
        obj=json.loads(jar.read(file)); result=obj.get('result',{})
        if not isinstance(result,dict) or result.get('components') or result.get('id') not in rates: continue
        kind=obj.get('type','').split(':')[-1]; inputs=[]
        if kind=='crafting_shaped': inputs=[obj['key'][c] for row in obj['pattern'] for c in row if c!=' ']
        elif kind=='crafting_shapeless': inputs=obj['ingredients']
        elif kind in ('smelting','blasting','smoking','campfire_cooking','stonecutting'): inputs=[obj['ingredient']]
        elif kind=='smithing_transform': inputs=[obj[k] for k in ('template','base','addition')]
        else: continue
        groups=[ingredients(x) for x in inputs]
        if groups and all(groups): recipes.append((result['id'],result.get('count',1),groups))
    # Include the two authored recipes.
    recipes += [('energyexchange:transmutation_table',1,[{'minecraft:obsidian'}]*4+[{'minecraft:diamond'}]*3+[{'minecraft:redstone'}]*2),
                ('energyexchange:transmutation_tablet',1,[{'minecraft:obsidian'}]*4+[{'minecraft:gold_ingot'}]*4+[{'minecraft:diamond'}])]
    for iteration in range(128):
        changed=False
        for output,count,groups in recipes:
            cost=sum(min(rates[k] for k in group) for group in groups)/count
            if 0 < cost < rates[output]: rates[output]=cost;changed=True
        if not changed: break
    else: raise RuntimeError('Recipe prices did not converge / 配方价格未收敛')
for key,rate in rates.items(): values[key]=str(ceil(rate))
salvage={key:[str(rate.numerator),str(rate.denominator)] for key,rate in sorted(rates.items()) if rate.denominator!=1}
(ROOT/'src/main/resources/data/energyexchange/energyexchange/salvage.json').write_text(json.dumps(salvage,indent=2)+'\n')
target=ROOT/'src/main/resources/data/energyexchange/energyexchange/defaults.json'
target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(dict(sorted(values.items())),indent=2)+'\n')
print(f'{len(values)} explicit prices / 条显式价格; vanilla / 原版: {sum(k.startswith("minecraft:") for k in values)}')
