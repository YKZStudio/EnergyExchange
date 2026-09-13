"""Download the pinned integration fixtures for a supported Minecraft version."""
import hashlib,json,sys,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1]
profile=json.loads((root/'versions/test-mods.json').read_text())[sys.argv[1]]
for name in sys.argv[2:]:
    entry=profile[name]
    data=urllib.request.urlopen(entry['url'],timeout=120).read()
    if hashlib.sha256(data).hexdigest()!=entry['sha256']: raise ValueError('Checksum mismatch: '+name)
    target=root/'test-mods'/(name+'.jar');target.parent.mkdir(exist_ok=True);target.write_bytes(data)
    print('Verified',name)
