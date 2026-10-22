"""Builds the second YouTube Short, "Your shield was hiding this", from VideoTour's frames.

python assemble_short2.py <video_tour dir> <sounds dir> <out dir>
(sounds from get_sounds.py; set CAPTION_FONT to a heavy font file if none of the usual ones is found)

Makes not-now-shield-short-2.mp4 (the game's own sounds), not-now-shield-short-2-silent.mp4 and
not-now-shield-short-2-thumbnail.png, 1080x1920. It opens on the shield at vanilla's height, which
drops to show the creeper behind it; the last shot steps back into the first one's spot, so a
replay loops; no title or end card.

Captions are bold white capitals with a black outline straight on the footage, one word in the
icon's diamond blue, and "FOLLOW FOR MORE COZY MODS" over the last shot. A small Not Now, Shield
badge stays in one place all the way through. If a scene changes in VideoTour, change its caption
here too.
"""
import csv, os, random, shutil, subprocess, sys
from concurrent.futures import ThreadPoolExecutor
from PIL import Image, ImageDraw, ImageFont

frames_dir, sounds_dir, out = sys.argv[1:4]
FPS = 20
BLUE = (94, 205, 228)
repo = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')

W, H, SIZE, STROKE, LEAD = 1080, 1920, 84, 7, 104
NAME = 'not-now-shield-short-2'
# Scene, caption lines ("*word*" in blue), where the caption's top sits, and the frame it starts on.
SCENES = [
    ('s_reveal', ['YOUR SHIELD WAS', '*HIDING* THIS'], 390, 20),
    ('s_block', ['IT STILL', '*BLOCKS*'], 390, 0),
    ('s_walk', ['SEE MORE OF', 'YOUR *WORLD*'], 390, 0),
    ('s_loop', None, 0, 0),
]
# Frames to hold each scene's last (still) frame for, so a voiceover's lines have room to breathe:
# the creeper standing there, the smoke clearing, the loop's first spot. Nothing moves in those
# frames, so holding one shows nothing that didn't happen. HOLD=0 in the environment turns them off.
HOLD = {'s_reveal': 18, 's_block': 8, 's_loop': 16} if os.environ.get('HOLD', '1') != '0' else {}
# Over the last shot, low in the safe zone and left of YouTube's buttons (the right ~19%).
CTA = ['FOLLOW FOR MORE', '*COZY* MODS']
CTA_TOP = 1250
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
    # -nostdin: ffmpeg otherwise waits for keyboard input when run in the background.
    if args[0] == 'ffmpeg':
        args = ('ffmpeg', '-nostdin') + args[1:]
    subprocess.run(args, check=True, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def caption(lines, top, face, width=W, lead=LEAD, stroke=STROKE):
    """A transparent frame-sized layer with the caption lines, centred in the first `width` pixels."""
    layer = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    y = top
    for line in lines:
        parts = [(p, BLUE if i % 2 else (255, 255, 255)) for i, p in enumerate(line.split('*')) if p]
        x = (width - sum(d.textlength(p, font=face) for p, _ in parts)) / 2
        for text, colour in parts:
            d.text((x, y), text, font=face, fill=colour, stroke_width=stroke, stroke_fill=(0, 0, 0))
            x += d.textlength(text, font=face)
        y += lead
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


CAPTION = font(FONTS, SIZE)
BADGE = badge()
CTA_LAYER = caption(CTA, CTA_TOP, font(FONTS, 64), int(W * 0.81), 80, 6)
FADE_IN = 4  # frames for a caption to fade in; scenes cut hard

with open(os.path.join(frames_dir, 'actions.tsv')) as f:
    actions = [(r[0], int(r[1]), r[2], r[3]) for r in csv.reader(f, delimiter='\t')]

work = os.path.join(out, 'work-short-2')
shutil.rmtree(work, ignore_errors=True)
os.makedirs(work)


def faded(layer, i):
    a = min(1.0, (i + 1) / FADE_IN)
    if a >= 1.0:
        return layer
    copy = layer.copy()
    copy.putalpha(copy.getchannel('A').point(lambda v: int(v * a)))
    return copy


jobs, kept = [], {}
for index, (scene, lines, top, from_frame) in enumerate(SCENES):
    names = sorted(n for n in os.listdir(frames_dir) if n.startswith(scene + '_') and n[len(scene) + 1:-4].isdigit())
    if not names:
        print('no frames for', scene, '(skipped)')
        continue
    names += [names[-1]] * HOLD.get(scene, 0)
    kept[scene] = len(names)
    dest = os.path.join(work, scene)
    os.makedirs(dest)
    cap = caption(lines, top, CAPTION) if lines else None
    last = index == len(SCENES) - 1
    for i, n in enumerate(names):
        jobs.append((os.path.join(frames_dir, n), os.path.join(dest, '%04d.png' % i), i, cap, from_frame, last))


def draw(job):
    src, dest, i, cap, from_frame, last = job
    img = Image.open(src).convert('RGBA')
    if img.size != (W, H):
        img = img.resize((W, H), Image.LANCZOS)
    img.alpha_composite(BADGE)
    if cap is not None and i >= from_frame:
        img.alpha_composite(faded(cap, i - from_frame))
    if last:
        img.alpha_composite(faded(CTA_LAYER, i))
    img.convert('RGB').save(dest, compress_level=1)


with ThreadPoolExecutor(os.cpu_count()) as pool:
    list(pool.map(draw, jobs))

clips, starts, t = [], {}, 0.0
for scene, *_ in SCENES:
    if scene not in kept:
        continue
    clip = os.path.join(work, scene + '.mp4')
    run('ffmpeg', '-y', '-framerate', str(FPS), '-i', os.path.join(work, scene, '%04d.png'),
        '-vf', 'format=yuv420p', '-r', '30', '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', clip)
    starts[scene] = t
    t += kept[scene] / FPS
    clips.append(clip)
total = t
with open(os.path.join(work, 'list.txt'), 'w') as f:
    f.writelines(f"file '{c.replace(os.sep, '/')}'\n" for c in clips)
silent = os.path.join(out, NAME + '-silent.mp4')
run('ffmpeg', '-y', '-f', 'concat', '-safe', '0', '-i', os.path.join(work, 'list.txt'), '-c', 'copy', '-movflags', '+faststart', silent)

# The game's own sounds at the logged frames: footsteps on grass while walking, the creeper's fuse,
# the blast and the shield taking it.
random.seed(7)
STEP_EVERY = {'s_reveal': 22, 's_loop': 22, 's_walk': 9}
cues = []
for scene, frame, kind, what in actions:
    if scene not in starts:
        continue
    at = starts[scene] + frame / FPS
    if kind == 'walk':
        # Steps until the next thing logged in the scene (the walk's end is the next log line).
        later = [f for s, f, k, _ in actions if s == scene and f > frame]
        end = min(later) if later else kept[scene]
        for f in range(frame + 4, end, STEP_EVERY.get(scene, 10)):
            cues.append((starts[scene] + f / FPS, 'grass%d' % random.randint(1, 6), 0.9 + random.random() * 0.15, 0.35))
    elif kind == 'fuse':
        cues.append((at, 'fuse', 1.0, 0.9))
    elif kind == 'boom':
        # The blast is logged once the creeper is gone, a frame after it went off.
        cues.append((max(0.0, at - 1 / FPS), 'explode%d' % random.randint(1, 4), 1.0, 0.8))
        cues.append((max(0.0, at - 1 / FPS), 'block%d' % random.randint(1, 5), 1.0, 0.9))
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

# The thumbnail: the reveal's last frame (the creeper plainly there over the lowered shield), with
# the tagline large in the sky.
raw = sorted(n for n in os.listdir(frames_dir) if n.startswith('s_reveal_') and n[9:-4].isdigit())
thumb = Image.open(os.path.join(frames_dir, raw[-1])).convert('RGBA').resize((W, H), Image.LANCZOS)
thumb.alpha_composite(caption(['YOUR SHIELD', 'WAS *HIDING*', 'THIS'], 330, font(FONTS, 120), W, 140, 10))
thumb.convert('RGB').save(os.path.join(out, NAME + '-thumbnail.png'))
print('total seconds', round(total, 2))
for scene in kept:
    print(f'  {scene}: {kept[scene] / FPS:.2f} s')
