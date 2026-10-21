"""Copies the game sounds the Short uses out of Loom's asset cache (filled by any game-test run).

python get_sounds.py <out dir>
The sounds are Mojang's, so they're read from the cache each time rather than kept in the repo.
"""
import glob, json, os, shutil, sys

out = sys.argv[1]
os.makedirs(out, exist_ok=True)
assets = os.path.expanduser('~/.gradle/caches/fabric-loom/assets')
SOUNDS = {'strip1': 'item/axe/strip1', 'strip2': 'item/axe/strip2', 'strip3': 'item/axe/strip3',
          'scrape1': 'item/axe/scrape1', 'scrape2': 'item/axe/scrape2',
          'till1': 'item/hoe/till1', 'till2': 'item/hoe/till2', 'path1': 'item/shovel/flatten1',
          # The second Short: footsteps, the creeper's fuse and blast, the shield taking it.
          'fuse': 'random/fuse'}
SOUNDS.update({f'grass{i}': f'step/grass{i}' for i in range(1, 7)})
SOUNDS.update({f'explode{i}': f'random/explode{i}' for i in range(1, 5)})
SOUNDS.update({f'block{i}': f'item/shield/block{i}' for i in range(1, 6)})
objects = {}
for index in sorted(glob.glob(os.path.join(assets, 'indexes', '*.json'))):
    objects.update(json.load(open(index))['objects'])
for name, file in SOUNDS.items():
    h = objects[f'minecraft/sounds/{file}.ogg']['hash']
    source = os.path.join(assets, 'objects', h[:2], h)
    if not os.path.exists(source):
        sys.exit(f'{file}.ogg is not in the asset cache; run a game test with resources.download.minecraft.net reachable')
    shutil.copy(source, os.path.join(out, name + '.ogg'))
    print(name)
