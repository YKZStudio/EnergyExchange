"""Generate a target's vanilla prices, preserving authored non-vanilla values.
生成指定版本原版价格，保留已维护的非原版价格。
Usage: python tools/generate_target_values.py 26.3 /path/to/client.jar /path/to/tacz
"""
import json
import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
target, client, tacz = sys.argv[1:]
if target not in {'26.1.2', '26.2', '26.3'}:
    raise ValueError('Unsupported Minecraft target')
resources = root / 'versions' / target / 'resources'
output = resources / 'data/energyexchange/energyexchange'
output.mkdir(parents=True, exist_ok=True)
subprocess.run([sys.executable, str(root / 'tools/generate_values.py'), client, tacz],
               env={**os.environ, 'EE_RESOURCE_ROOT': str(resources)}, check=True)
for name in ('defaults.json', 'salvage.json'):
    baseline = json.loads((root / 'src/main/resources/data/energyexchange/energyexchange' / name).read_text())
    generated = json.loads((output / name).read_text())
    values = {key: value for key, value in generated.items() if key.startswith('minecraft:')}
    values.update({key: value for key, value in baseline.items() if not key.startswith('minecraft:')})
    (output / name).write_text(json.dumps(dict(sorted(values.items())), indent=2) + '\n')
