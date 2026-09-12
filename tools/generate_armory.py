#!/usr/bin/env python3
"""Generate 3x3 armory recipes and exact prices for all installed-mod profiles.
Run after generate_values.py and generate_mod_values.py. No upstream recipe/code is copied.
"""
import json
from pathlib import Path
from fractions import Fraction
from math import ceil
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/energyexchange'
RULES=DATA/'energyexchange'
RECIPES=DATA/'recipe'
recipes={}
def item(s):return s if ':' in s else 'energyexchange:'+s
def shaped(name,pattern,key):
 recipes[name]={'type':'minecraft:crafting_shaped','category':'equipment','pattern':pattern,'key':{k:item(v) for k,v in key.items()},'result':{'id':item(name),'count':1}}
def shapeless(name,*inputs):recipes[name]={'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':[item(x) for x in inputs],'result':{'id':item(name),'count':1}}
shaped('dark_matter',['CCC','CDC','CCC'],{'C':'minecraft:coal_block','D':'minecraft:diamond_block'})
shaped('red_matter',['DDD','DSD','DDD'],{'D':'dark_matter','S':'minecraft:nether_star'})
shaped('crystal_matrix_ingot',['DED','ESE','DED'],{'D':'minecraft:diamond_block','E':'minecraft:emerald_block','S':'minecraft:nether_star'})
shaped('neutronium_ingot',['NNN','NSN','NNN'],{'N':'minecraft:netherite_block','S':'minecraft:nether_star'})
shapeless('infinity_catalyst','dark_matter','red_matter','crystal_matrix_ingot','neutronium_ingot','minecraft:nether_star','minecraft:dragon_breath','minecraft:heart_of_the_sea','minecraft:echo_shard','minecraft:netherite_block')
shaped('infinity_ingot',['NNN','NCN','NNN'],{'N':'neutronium_ingot','C':'infinity_catalyst'})
patterns={'sword':[' M ',' M ',' R '],'pickaxe':['MMM',' R ',' R '],'axe':['MM ','MR ',' R '],'shovel':[' M ',' R ',' R '],'helmet':['MMM','M M'],'chestplate':['M M','MMM','MMM'],'leggings':['MMM','M M','M M'],'boots':['M M','M M']}
for kind,pattern in patterns.items():
 key={'M':'dark_matter'}
 if any('R' in row for row in pattern):key['R']='minecraft:blaze_rod'
 shaped('dark_matter_'+kind,pattern,key)
 for tier,material,previous in [('red_matter','red_matter','dark_matter'),('infinity','infinity_ingot','red_matter')]:
  shaped(tier+'_'+kind,['MMM','MGM','MMM'],{'M':material,'G':previous+'_'+kind})
shapeless('red_matter_katar','red_matter_sword','red_matter_axe','red_matter','red_matter')
shapeless('red_matter_morning_star','red_matter_pickaxe','red_matter_shovel','red_matter','red_matter')
RECIPES.mkdir(exist_ok=True)
for k,v in recipes.items():(RECIPES/(k+'.json')).write_text(json.dumps(v,indent=2)+'\n')
def apply(values,salvage):
 rates={k:Fraction(*map(int,salvage[k])) if k in salvage else Fraction(v) for k,v in values.items()}
 for name,r in recipes.items():
  inputs=r['ingredients'] if 'ingredients' in r else [r['key'][c] for row in r['pattern'] for c in row if c!=' ']
  # Ignore crafting remainders conservatively: subtract returned bottle from dragon breath.
  cost=sum(rates[k]-(rates.get('minecraft:glass_bottle',0) if k=='minecraft:dragon_breath' else 0) for k in inputs)
  assert cost>0 and len(str(cost.numerator))<=128 and len(str(cost.denominator))<=128
  rates[item(name)]=cost;values[item(name)]=str(ceil(cost))
  if cost.denominator!=1:salvage[item(name)]=[str(cost.numerator),str(cost.denominator)]
  else:salvage.pop(item(name),None)
 return dict(sorted(values.items())),dict(sorted(salvage.items()))
v,s=apply(json.loads((RULES/'defaults.json').read_text()),json.loads((RULES/'salvage.json').read_text()))
(RULES/'defaults.json').write_text(json.dumps(v,indent=2)+'\n');(RULES/'salvage.json').write_text(json.dumps(s,indent=2)+'\n')
for p in (RULES/'compat').glob('*.json'):
 o=json.loads(p.read_text());v,s=apply(o['values'],o['salvage']);p.write_text(json.dumps({'values':v,'salvage':s},indent=2)+'\n')
print('Generated',len(recipes),'armory recipes/prices; max crafting grid 3x3')
