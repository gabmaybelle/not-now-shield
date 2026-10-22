"""Builds the YouTube Short from VideoTour's frames.

python assemble.py <video_tour dir> <sounds dir> <out dir>
(sounds from get_sounds.py; set CAPTION_FONT to a heavy font file if none of the usual ones is found)

Makes not-now-shield-short.mp4 (the game's own sounds), not-now-shield-short-silent.mp4 and
not-now-shield-short-thumbnail.png, 1080x1920. It opens on the axe stripping a log with the shield
in hand, and its last shot leads back into the first, so a replay loops; no title or end card.

Captions are bold white capitals with a black outline straight on the footage, one word in the
icon's diamond blue. A small Not Now, Shield badge stays in one place all the way through. If a
scene changes in VideoTour, change its caption here too.
"""
import csv, os, random, shutil, subprocess, sys
from PIL import Image, ImageDraw, ImageFont

frames_dir, sounds_dir, out = sys.argv[1:4]
FPS = 20
BLUE = (94, 205, 228)
repo = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')

W, H, SIZE, STROKE, LEAD = 1080, 1920, 84, 7, 104
NAME = 'not-now-shield-short'
# Scene, the frames kept (first, last; None = to the end), caption lines ("*word*" in blue) and
# where the caption's top sits.
SCENES = [
    ('s_strip', 0, None, ['SHIELD UP,', 'AXE *WORKS*'], 390),
    ('s_more', 0, None, ['NO MORE', '*BLOCKING*'], 390),
    ('s_garden', 0, None, ['HOES AND', 'SHOVELS *TOO*'], 390),
    ('s_view', 0, None, ['SEE MORE OF', 'YOUR *WORLD*'], 390),
    ('s_loop', 0, None, None, 0),
]
# For the voiced version (HOLD=1 in the environment): frames to hold each scene's last frame for,
# so the lines have room to breathe. Each scene ends on a still result (the stripped log, the bright
# copper, the path, the view, the loop's first spot), so holding it shows nothing that didn't happen.
HOLD = {'s_garden': 16, 's_loop': 32} if os.environ.get('HOLD') == '1' else {}
# Also for the voiced version: frames to open a scene on its first frame (the axe aimed at the log,
# before anything happens) while the first line is said, pulling slowly back from ZOOM to the
# normal view so the opening still moves; the bark then comes off as the line ends. The scene's
# caption waits for the real frames' result (CAPTION_FROM, counted in the scene's own frames).
PRE = {'s_strip': 39} if os.environ.get('HOLD') == '1' else {}
ZOOM = 1.08
CAPTION_FROM = {'s_strip': 3}
BADGE_BOX = (40, 250, 540, 350)

FONTS = [os.environ.get('CAPTION_FONT', ''), 'C:/Windows/Fonts/ariblk.ttf', '/usr/share/fonts/truetype/msttcorefonts/Arial_Black.ttf',
         '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf']
BADGE_FONTS = [os.environ.get('BADGE_FONT', ''), 'C:/Windows/Fonts/segoeuib.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf']


def font(paths, size):
    for p in paths:
        if p and os.path.exists(p):
            return ImageFont.truetype(p, size)
    sys.exit('no caption font found; set CAPTION_FONT')


def run(*args):
    subprocess.run(args, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


CAPTION = font(FONTS, SIZE)


def caption(lines, top):
    """A transparent frame-sized layer with the caption lines, centred."""
    layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    y = top
    for line in lines:
        parts = [(p, BLUE if i % 2 else (255, 255, 255)) for i, p in enumerate(line.split('*')) if p]
        x = (W - sum(d.textlength(p, font=CAPTION) for p, _ in parts)) / 2
        for text, colour in parts:
            d.text((x, y), text, font=CAPTION, fill=colour, stroke_width=STROKE, stroke_fill=(0, 0, 0))
            x += d.textlength(text, font=CAPTION)
        y += LEAD
    return layer


def badge():
    """A small mod-list style badge: the icon, the name, the loaders."""
    layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    box = BADGE_BOX
    d.rounded_rectangle(box, 18, fill=(20, 20, 24, 225))
    side = box[3] - box[1] - 28
    icon = Image.open(os.path.join(repo, 'common/src/main/resources/assets/shieldstrip/icon.png')).convert('RGBA')
    icon = icon.resize((side, side), Image.NEAREST)
    layer.alpha_composite(icon, (box[0] + 14, box[1] + 14))
    x = box[0] + side + 32
    d.text((x, box[1] + 12), 'Not Now, Shield', font=font(BADGE_FONTS, 40), fill=(255, 255, 255))
    d.text((x, box[1] + 60), 'Fabric · NeoForge', font=font(BADGE_FONTS, 24), fill=(170, 170, 180))
    return layer


BADGE = badge()

with open(os.path.join(frames_dir, 'actions.tsv')) as f:
    actions = [(r[0], int(r[1]), r[2], r[3]) for r in csv.reader(f, delimiter='\t')]

work = os.path.join(out, 'work-short')
shutil.rmtree(work, ignore_errors=True)
os.makedirs(work)

FADE_IN = 4  # frames for a caption to fade in; scenes cut hard
kept = {}
for scene, first, last, lines, top in SCENES:
    names = sorted(n for n in os.listdir(frames_dir) if n.startswith(scene + '_') and n.endswith('.png'))
    if not names:
        print('no frames for', scene, '(skipped)')
        continue
    end = len(names) if last is None else min(last + 1, len(names))
    kept[scene] = (first, end)
    dest = os.path.join(work, scene)
    os.makedirs(dest)
    cap = caption(lines, top) if lines else None
    pre = PRE.get(scene, 0)
    cap_from = pre + CAPTION_FROM.get(scene, 0) if pre else 0
    for i, n in enumerate([first] * pre + list(range(first, end)) + [end - 1] * HOLD.get(scene, 0)):
        img = Image.open(os.path.join(frames_dir, names[n])).convert('RGBA')
        if img.size != (W, H):
            img = img.resize((W, H), Image.LANCZOS)
        if i < pre:
            # Ease from ZOOM back to 1 over the opening hold, centred.
            k = i / pre
            z = ZOOM - (ZOOM - 1) * (1 - (1 - k) ** 2)
            cw, ch = W / z, H / z
            box = ((W - cw) / 2, (H - ch) / 2, (W + cw) / 2, (H + ch) / 2)
            img = img.resize((W, H), Image.LANCZOS, box=box)
        img.alpha_composite(BADGE)
        if cap is not None and i >= cap_from:
            a = min(1.0, (i - cap_from + 1) / FADE_IN)
            layer = cap.copy()
            layer.putalpha(layer.getchannel('A').point(lambda v: int(v * a)))
            img.alpha_composite(layer)
        img.convert('RGB').save(os.path.join(dest, '%04d.png' % i))

clips, starts, t = [], {}, 0.0
for scene, *_ in SCENES:
    if scene not in kept:
        continue
    clip = os.path.join(work, scene + '.mp4')
    run('ffmpeg', '-y', '-framerate', str(FPS), '-i', os.path.join(work, scene, '%04d.png'),
        '-vf', 'format=yuv420p', '-r', '30', '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', clip)
    starts[scene] = t
    first, end = kept[scene]
    t += (PRE.get(scene, 0) + end - first + HOLD.get(scene, 0)) / FPS
    clips.append(clip)
total = t
with open(os.path.join(work, 'list.txt'), 'w') as f:
    f.writelines(f"file '{c.replace(os.sep, '/')}'\n" for c in clips)
silent = os.path.join(out, NAME + '-silent.mp4')
run('ffmpeg', '-y', '-f', 'concat', '-safe', '0', '-i', os.path.join(work, 'list.txt'), '-c', 'copy', '-movflags', '+faststart', silent)

# The game's own sounds at the logged frames: bark stripping, copper scraping, the hoe and the shovel.
random.seed(7)
SOUND = {'strip': ['strip1', 'strip2', 'strip3'], 'scrape': ['scrape1', 'scrape2'], 'till': ['till1', 'till2'], 'path': ['path1']}
cues = []
for scene, frame, kind, what in actions:
    if scene not in starts or kind not in SOUND:
        continue
    first, end = kept[scene]
    if not first <= frame < end:
        continue
    at = starts[scene] + (PRE.get(scene, 0) + frame - first) / FPS
    cues.append((at, random.choice(SOUND[kind]), 0.9 + random.random() * 0.2, 0.8))
inputs, filters = [], []
for i, (at, sound, pitch, volume) in enumerate(cues):
    inputs += ['-i', os.path.join(sounds_dir, sound + '.ogg')]
    ms = int(at * 1000)
    filters.append(f'[{i}:a]aformat=sample_rates=44100:channel_layouts=stereo,asetrate={44100 * pitch:.0f},aresample=44100,'
                   f'volume={volume},adelay={ms}|{ms}[s{i}]')
mix = ''.join(f'[s{i}]' for i in range(len(cues)))
filters.append(f'{mix}amix=inputs={len(cues)}:normalize=0,apad=whole_dur={total:.3f},atrim=0:{total:.3f}[a]')
audio = os.path.join(work, 'sound.wav')
run('ffmpeg', '-y', *inputs, '-filter_complex', ';'.join(filters), '-map', '[a]', audio)
run('ffmpeg', '-y', '-i', silent, '-i', audio, '-map', '0:v', '-map', '1:a', '-c:v', 'copy', '-c:a', 'aac', '-b:a', '160k',
    '-shortest', '-movflags', '+faststart', os.path.join(out, NAME + '.mp4'))

# The thumbnail: a clean frame from the opening scene, just after the bark comes off, with the
# mod's name large above the cottage.
raw = sorted(n for n in os.listdir(frames_dir) if n.startswith('s_strip_'))
stripped = [f for s, f, k, _ in actions if s == 's_strip' and k == 'strip']
pick = min(len(raw) - 1, (stripped[0] + 8) if stripped else len(raw) * 2 // 3)
thumb = Image.open(os.path.join(frames_dir, raw[pick])).convert('RGBA').resize((W, H), Image.LANCZOS)
d = ImageDraw.Draw(thumb)
size, top = 150, 300
big = font(FONTS, size)
for i, parts in enumerate([[('NOT NOW,', (255, 255, 255))], [('SHIELD', BLUE)]]):
    x = (W - sum(d.textlength(text, font=big) for text, _ in parts)) / 2
    for text, colour in parts:
        d.text((x, top + i * size * 1.1), text, font=big, fill=colour, stroke_width=size // 12, stroke_fill=(0, 0, 0))
        x += d.textlength(text, font=big)
thumb.convert('RGB').save(os.path.join(out, NAME + '-thumbnail.png'))
print('total seconds', round(total, 2))
for scene in kept:
    first, end = kept[scene]
    print(f'  {scene}: {(end - first) / FPS:.2f} s')
