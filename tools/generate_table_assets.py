#!/usr/bin/env python3
"""Generate original pixel-art assets. Requires Pillow; no upstream textures used."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/energyexchange'
texture = ROOT / 'textures/block'
texture.mkdir(parents=True, exist_ok=True)
im = Image.new('RGB', (64, 64), '#202d35')
d = ImageDraw.Draw(im)
for y in range(64):
    for x in range(64):
        shade = (x * 17 + y * 31 + x * y) % 9
        d.point((x,y), fill=(29+shade,41+shade,48+shade))
d.rectangle((1,1,62,62), outline='#b69a60', width=2)
d.rectangle((4,4,59,59), outline='#526269')
for box, color in [((8,8,55,55),'#cfb77b'), ((11,11,52,52),'#3e827e'), ((18,18,45,45),'#79c9b5')]:
    d.ellipse(box, outline=color, width=1)
# Eight distinct geometric marks around the ring.
for i in range(8):
    a = i * math.pi / 4
    x, y = round(31.5+19*math.sin(a)), round(31.5+19*math.cos(a))
    d.rectangle((x-2,y-2,x+2,y+2), fill='#25363e', outline='#d0b87c')
    d.point((x,y), fill='#adf0d7')
d.polygon([(32,20),(44,32),(32,44),(20,32)], outline='#bce8d4', width=1)
d.line([(32,24),(40,38),(24,38),(32,24)],fill='#bda36a',width=1)
d.rectangle((30,30,33,33), fill='#daf8db')
for x,y in [(6,6),(57,6),(6,57),(57,57)]:
    d.rectangle((x-1,y-1,x+1,y+1),fill='#72c8b5')
im.save(texture/'transmutation_table_top.png')
side = Image.new('RGB',(16,16),'#29383f'); sd=ImageDraw.Draw(side)
for y in (1,5,9,13):
    sd.line((0,y,15,y),fill='#ac935f');sd.line((0,y+1,15,y+1),fill='#47575b')
side.save(texture/'transmutation_table_side.png')
# Portable stone uses the same motif with transparent bevelled corners.
icon = im.convert('RGBA'); mask=Image.new('L',(64,64)); md=ImageDraw.Draw(mask)
md.polygon([(8,0),(55,0),(63,8),(63,55),(55,63),(8,63),(0,55),(0,8)],fill=255)
icon.putalpha(mask); (ROOT/'textures/item').mkdir(parents=True,exist_ok=True)
icon.save(ROOT/'textures/item/transmutation_tablet.png')
faces={f:{'texture':'#top' if f=='up' else '#side'} for f in ['up','down','north','south','east','west']}
model={'parent':'minecraft:block/block','textures':{'particle':'#side','top':'energyexchange:block/transmutation_table_top','side':'energyexchange:block/transmutation_table_side'},'elements':[{'from':[0,0,0],'to':[16,2,16],'faces':faces},{'from':[1,2,1],'to':[15,3,15],'faces':faces}],'display':{'gui':{'rotation':[30,225,0],'translation':[0,3,0],'scale':[0.75,0.75,0.75]},'ground':{'translation':[0,3,0],'scale':[0.5,0.5,0.5]},'fixed':{'rotation':[90,0,0],'translation':[0,0,-6],'scale':[0.8,0.8,0.8]}}}
(ROOT/'models/block/transmutation_table.json').write_text(json.dumps(model,indent=2)+'\n')
(ROOT/'models/item/transmutation_tablet.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'energyexchange:item/transmutation_tablet'}},indent=2)+'\n')
