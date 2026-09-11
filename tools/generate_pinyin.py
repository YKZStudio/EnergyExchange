#!/usr/bin/env python3
"""Generate the offline Mandarin index from Unicode 17.0.0 Unihan (Unicode-3.0).
Usage: python tools/generate_pinyin.py /path/to/Unihan.zip
Source: https://www.unicode.org/Public/17.0.0/ucd/Unihan.zip
The checksum also rejects incomplete downloads. No Python packages are required.
"""
from pathlib import Path
from collections import defaultdict
import hashlib
import re
import sys
import unicodedata
import zipfile

SHA256 = 'f7a48b2b545acfaa77b2d607ae28747404ce02baefee16396c5d2d7a8ef34b5e'
data = Path(sys.argv[1]).read_bytes()
if hashlib.sha256(data).hexdigest() != SHA256:
    raise ValueError('Expected the complete Unicode 17.0.0 Unihan.zip')
readings = defaultdict(set)
with zipfile.ZipFile(sys.argv[1]) as archive:
    for line in archive.read('Unihan_Readings.txt').decode('utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        code, field, value = line.split('\t')
        if field not in {'kMandarin', 'kHanyuPinyin', 'kXHC1983', 'kHanyuPinlu'}:
            continue
        for word in value.split():
            word = re.sub(r'\([^)]*\)', '', word.split(':')[-1])
            for syllable in word.split(','):
                syllable = unicodedata.normalize('NFD', syllable.lower()).replace('u\u0308', 'v')
                syllable = ''.join(c for c in syllable if not unicodedata.combining(c))
                if re.fullmatch('[a-z]{1,8}', syllable):
                    readings[int(code[2:], 16)].add(syllable)
    # Simplified/traditional forms share readings for search, not semantic variants.
    links = []
    for line in archive.read('Unihan_Variants.txt').decode('utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        code, field, value = line.split('\t')
        if field in {'kSimplifiedVariant', 'kTraditionalVariant'}:
            links.extend((int(code[2:],16), int(v.split('<')[0][2:],16)) for v in value.split())
    changed = True
    while changed:
        changed = False
        for a,b in links:
            combined = readings[a] | readings[b]
            if readings[a] != combined or readings[b] != combined:
                readings[a] = combined.copy(); readings[b] = combined.copy(); changed = True
output = Path(__file__).resolve().parents[1] / 'src/main/resources/energyexchange/pinyin.tsv'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text('# Unicode 17.0.0 Unihan; Unicode-3.0; see META-INF/licenses/Unicode-3.0.txt\n' + ''.join(f'{cp:X}\t{",".join(sorted(values))}\n' for cp,values in sorted(readings.items()) if values), encoding='ascii')
print(f'{sum(bool(v) for v in readings.values())} characters; {output.stat().st_size} bytes')
