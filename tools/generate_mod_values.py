#!/usr/bin/env python3
"""Recipe-constrained optional-mod prices. Run immediately after generate_values.py to restore the vanilla-only baseline.
Usage: python tools/generate_mod_values.py client.jar /path/to/tacz backpack.jar
Backpack input is pinned to official Fabric 26.2-11.3.2; reads facts only.
"""
import json,sys,zipfile,re,hashlib
from pathlib import Path
from fractions import Fraction
from math import ceil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/data/energyexchange/energyexchange'
base=json.loads((OUT/'defaults.json').read_text()); old_fractions=json.loads((OUT/'salvage.json').read_text())
initial={k:Fraction(*map(int,old_fractions[k])) if k in old_fractions else Fraction(v) for k,v in base.items()}
backpack=Path(sys.argv[3])
if hashlib.sha256(backpack.read_bytes()).hexdigest()!='b06753dbd277700e97efaefa541e757fa7fd6cdd91538bf4a1df851072b981de':raise ValueError('Expected Traveler\'s Backpack Fabric 26.2-11.3.2')
resources={}
with zipfile.ZipFile(sys.argv[1]) as z:
 for n in z.namelist():
  if n.startswith('data/') and n.endswith('.json'): resources[n]=json.loads(z.read(n))
with zipfile.ZipFile(backpack) as z:
 bp={n:json.loads(z.read(n)) for n in z.namelist() if n.startswith('data/') and n.endswith('.json')}
 for n in z.namelist():
  if n.startswith('assets/travelersbackpack/items/') and n.endswith('.json'):
   initial['travelersbackpack:'+Path(n).stem]=Fraction(262144)
checkout=Path(sys.argv[2]);tz={}; custom=[]
for p in sorted((checkout/'src/main/resources/data').rglob('*.json')):
 try:tz['data/'+str(p.relative_to(checkout/'src/main/resources/data'))]=json.loads(p.read_text())
 except json.JSONDecodeError:continue
for p in sorted((checkout/'src/main/resources/assets/tacz/custom').glob('**/data/*/recipe/**/*.json')):
 custom.append(json.loads(p.read_text()))
# Existing identity coverage is retained; assign conservative fallback ceilings.
for k in initial:
 if k.startswith('tacz:modern_kinetic_gun'):initial[k]=Fraction(32768)
 elif k.startswith('tacz:ammo'):initial[k]=Fraction(32 if not k.startswith('tacz:ammo_box') else 4096)
 elif k.startswith('tacz:attachment'):initial[k]=Fraction(4096)
 elif k.startswith('lrtactical:'):initial[k]=Fraction(4096 if ':detonator' in k else 1024 if ':consumable' in k else 2048 if ':throwable' in k else 8192)
# Recipe-priced models start high enough to reflect their actual materials.
kinds={'gun':'tacz:modern_kinetic_gun','ammo':'tacz:ammo','attachment':'tacz:attachment','block':'tacz:gun_smith_table','throwable':'lrtactical:throwable','melee':'lrtactical:melee','consumable':'lrtactical:consumable'}
for o in custom+list(tz.values()):
 r=o.get('result',{})
 if isinstance(r,dict) and r.get('type') in kinds and r.get('id'):
  key=kinds[r['type']]+'#'+r['id']
  if key in initial:initial[key]=Fraction(1048576)
aliases={'c:strings':'string','c:leathers':'leather','c:chests/wooden':'chest','c:chests':'chest','c:gunpowders':'gunpowder','c:ender_pearls':'ender_pearl','c:slime_balls':'slime_ball','c:feathers':'feather','c:eggs':'egg','c:bones':'bone','c:rods/wooden':'stick','c:glass_blocks':'glass','c:glass_blocks/colorless':'glass','c:glass_panes':'glass_pane','c:glass_panes/colorless':'glass_pane','c:buckets/water':'water_bucket','c:buckets/lava':'lava_bucket','c:cobblestones':'cobblestone','c:stones':'stone','c:obsidians':'obsidian','c:dusts/redstone':'redstone','c:dusts/glowstone':'glowstone_dust','c:gems/lapis':'lapis_lazuli','c:gems/quartz':'quartz','c:nuggets/iron':'iron_nugget','c:nuggets/gold':'gold_nugget'}
aliases.update({'c:crops/carrot':'carrot','c:crops/nether_wart':'nether_wart','c:crops/wheat':'wheat','c:player_workstations/crafting_tables':'crafting_table','c:rods/blaze':'blaze_rod','c:storage_blocks/lapis':'lapis_block'})
unknown=set()
def solve(use_tacz,use_backpack):
 data=resources | (tz if use_tacz else {}) | (bp if use_backpack else {})
 rates=initial.copy();recipes=[]
 def ingredients(x,seen=frozenset()):
  if isinstance(x,list):return set().union(*(ingredients(y,seen) for y in x))
  if isinstance(x,dict):return ingredients(x.get('item',x.get('id','#'+x['tag'] if 'tag' in x else '')),seen)
  if not isinstance(x,str):return set()
  if not x.startswith('#'):return {x} if x in rates else set()
  tag=x[1:]
  if tag in seen:return set()
  ns,path=tag.split(':',1);key=f'data/{ns}/tags/item/{path}.json'
  if key in data:return set().union(*(ingredients(v,seen|{tag}) for v in data[key].get('values',[])))
  item=aliases.get(tag)
  if not item and re.fullmatch(r'c:(ingots|nuggets|gems|dyes)/[a-z_]+',tag):
   group,material=tag[2:].split('/');item=material+{'ingots':'_ingot','nuggets':'_nugget','gems':'','dyes':'_dye'}[group]
  if item and 'minecraft:'+item in rates:return {'minecraft:'+item}
  unknown.add(tag);return set()
 source=[o for n,o in sorted(data.items()) if '/recipe/' in n]+(custom if use_tacz else [])
 for o in source:
  r=o.get('result',{})
  if not isinstance(r,dict):continue
  output=r.get('id');count=r.get('count',1);kind=o.get('type','').split(':')[-1];inputs=[]
  if 'materials' in o and r.get('type') in kinds:
   output=kinds[r['type']]+'#'+str(output)
   if output not in rates:continue
   for m in o['materials']:inputs.extend([m['item']]*m.get('count',1))
  elif kind in ('crafting_shaped','backpack_shaped'):inputs=[o['key'][c] for row in o['pattern'] for c in row if c!=' ']
  elif kind=='crafting_shapeless':inputs=o['ingredients']
  elif kind in ('smelting','blasting','smoking','campfire_cooking','stonecutting'):inputs=[o['ingredient']]
  elif kind in ('smithing_transform','backpack_upgrade'):inputs=[o[k] for k in ('template','base','addition')]
  else:continue
  groups=[ingredients(x) for x in inputs]
  if output in rates and groups and all(groups) and count>0:recipes.append((output,count,groups))
 for iteration in range(256):
  changed=False
  for output,count,groups in recipes:
   self_inputs=sum(g=={output} for g in groups)
   if count<=self_inputs:continue
   cost=sum(min(rates[k] for k in g) for g in groups if g!={output})/(count-self_inputs)
   if 0<cost<rates[output]:rates[output]=cost;changed=True
  if not changed:break
 else:raise ValueError('Recipe pricing did not converge')
 # Assert every resolved ordinary recipe satisfies the derived conversion constraint.
 for output,count,groups in recipes:
  assert rates[output]*count<=sum(min(rates[k] for k in g) for g in groups),(output,rates[output])
 print('Profile',use_tacz,use_backpack,'constraints',len(recipes))
 return rates
solutions={'tacz':solve(True,False),'travelersbackpack':solve(False,True),'tacz-travelersbackpack':solve(True,True)}
# Offline defaults include all supported IDs; active profiles use the full constrained graph.
combined=solutions['tacz-travelersbackpack']
base_rates={k:initial[k] if k.startswith('minecraft:') else v for k,v in combined.items()}
# Preserve vanilla defaults from the preceding vanilla-only generator.
for k,v in base.items():
 if k.startswith('minecraft:'):base_rates[k]=Fraction(*map(int,old_fractions[k])) if k in old_fractions else Fraction(v)
def encode(rates):
 for k,v in rates.items():
  if v<=0 or max(len(str(v.numerator)),len(str(v.denominator)))>128:raise ValueError(k)
 return {'values':{k:str(ceil(v)) for k,v in sorted(rates.items())},'salvage':{k:[str(v.numerator),str(v.denominator)] for k,v in sorted(rates.items()) if v.denominator!=1}}
base_out=encode(base_rates)
(OUT/'defaults.json').write_text(json.dumps(base_out['values'],indent=2)+'\n');(OUT/'salvage.json').write_text(json.dumps(base_out['salvage'],indent=2)+'\n')
(OUT/'compat').mkdir(exist_ok=True)
for name,rates in solutions.items():(OUT/'compat'/f'{name}.json').write_text(json.dumps(encode(rates),indent=2)+'\n')
print('Unresolved tags (recipes skipped):',sorted(unknown))
for k in ['tacz:modern_kinetic_gun#tacz:ak47','tacz:ammo#tacz:762x39','minecraft:gunpowder','travelersbackpack:standard','travelersbackpack:diamond_tier_upgrade']:
 print(k,'base',base.get(k),'combined',str(combined[k]))
