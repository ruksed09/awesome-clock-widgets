"""Renders Versity's bundled wallpapers and their grid thumbnails.

Everything is procedural (numpy + Pillow), so the set can be regenerated or tuned:
    python3 generate.py <output dir> [ids...]                    portrait 1080x2340 + thumbnail
    python3 generate.py <output dir> --land [ids...]             landscape 2400x1500 (tablets, wp_<id>_land)
Positions are fractions of the canvas and object sizes are fractions of its short side (S), so the
same composition works upright on phones and sideways on tablets.
"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

W = H = S = 0
K = 1.0
YY = XX = U = V = None


def setup(w, h):
    """Sets the canvas. S is the short side; K scales pixel-sized details relative to a 1080 px phone."""
    global W, H, S, K, YY, XX, U, V
    W, H = w, h
    S = min(w, h)
    K = S / 1080
    YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)
    U, V = XX / W, YY / H


setup(1080, 2340)


# ---------------------------------------------------------------- helpers

def hexc(s):
    s = s.lstrip("#")
    return np.array([int(s[i:i + 2], 16) / 255 for i in (0, 2, 4)], np.float32)


def box(a, r, axis):
    if r < 1:
        return a
    c = np.cumsum(np.pad(a, [(r + 1, r) if i == axis else (0, 0) for i in range(a.ndim)], mode="edge"), axis=axis)
    hi = np.take(c, np.arange(2 * r + 1, c.shape[axis]), axis=axis)
    lo = np.take(c, np.arange(0, c.shape[axis] - 2 * r - 1), axis=axis)
    return (hi - lo) / (2 * r + 1)


def blur(a, sigma):
    """Float Gaussian approximation: three box passes per axis (no 8-bit banding)."""
    r = max(1, int(round(sigma * 0.9)))
    for _ in range(3):
        a = box(box(a, r, 0), r, 1)
    return a


def smooth_noise(scale, seed, h=None, w=None):
    h = H if h is None else h
    w = W if w is None else w
    rng = np.random.default_rng(seed)
    small = rng.random((h // scale + 3, w // scale + 3)).astype(np.float32)
    img = Image.fromarray(small, "F").resize((w + 3 * scale, h + 3 * scale), Image.BICUBIC)
    return np.asarray(img)[scale:scale + h, scale:scale + w]


def vgrad(stops):
    """Vertical gradient from [(pos, colour)] stops."""
    out = np.zeros((H, W, 3), np.float32)
    pos = [p for p, _ in stops]
    for ch in range(3):
        out[..., ch] = np.interp(V, pos, [c[ch] for _, c in stops])
    return out


def blob(cx, cy, rx, ry):
    return np.exp(-(((XX - cx * W) / (rx * W)) ** 2 + ((YY - cy * H) / (ry * H)) ** 2))


def mix(base, colour, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return base * (1 - a) + colour * a


def screen(base, colour, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return 1 - (1 - base) * (1 - colour * a)


def ellipse_mask(cx, cy, rx, ry, angle=0.0, wobble=0.0, seed=0, soft=1.5):
    ca, sa = math.cos(angle), math.sin(angle)
    dx, dy = XX - cx, YY - cy
    x = dx * ca + dy * sa
    y = -dx * sa + dy * ca
    theta = np.arctan2(y, x)
    rng = np.random.default_rng(seed)
    k = 1 + wobble * (0.6 * np.sin(2 * theta + rng.random() * 6) + 0.4 * np.sin(3 * theta + rng.random() * 6))
    d = np.sqrt((x / rx) ** 2 + (y / ry) ** 2) / k
    edge = soft / min(rx, ry)
    return np.clip((1 - d) / edge, 0, 1)


def rounded_rect_mask(cx, cy, w, h, r, angle=0.0, soft=1.5):
    ca, sa = math.cos(angle), math.sin(angle)
    dx, dy = XX - cx, YY - cy
    x = np.abs(dx * ca + dy * sa) - (w / 2 - r)
    y = np.abs(-dx * sa + dy * ca) - (h / 2 - r)
    d = np.sqrt(np.maximum(x, 0) ** 2 + np.maximum(y, 0) ** 2) + np.minimum(np.maximum(x, y), 0) - r
    return np.clip(-d / soft, 0, 1)


def stars(img, n, seed, size=(0.6, 1.8), bright=(0.35, 1.0), weight=None, tint=(1, 1, 1)):
    rng = np.random.default_rng(seed)
    layer = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(layer)
    placed = 0
    while placed < n:
        x, y = rng.random() * W, rng.random() * H
        if weight is not None and rng.random() > weight[int(y), int(x)]:
            continue
        r = rng.uniform(*size)
        d.ellipse((x - r, y - r, x + r, y + r), fill=int(255 * rng.uniform(*bright)))
        placed += 1
    a = np.asarray(layer).astype(np.float32) / 255
    glow = blur(a, 3) * 1.6
    return screen(img, np.array(tint, np.float32), a + glow)


def sparkle(img, x, y, r, strength=1.0, tint=(1, 1, 1)):
    """Four-point star with concave sides and a soft halo."""
    dx, dy = np.abs(XX - x) / r, np.abs(YY - y) / r
    shape = np.clip(1 - (np.sqrt(dx) + np.sqrt(dy)), 0, 1) ** 1.2
    halo = np.exp(-((XX - x) ** 2 + (YY - y) ** 2) / (r * 1.4) ** 2) * 0.35
    return screen(img, np.array(tint, np.float32), (shape * 2.2 + halo) * strength)


def grain(img, amount, seed):
    rng = np.random.default_rng(seed)
    return img + rng.normal(0, amount, img.shape[:2])[..., None]


def save(img, out_dir, wid, suffix=""):
    rng = np.random.default_rng(99)
    dither = (rng.random(img.shape) - rng.random(img.shape)) / 255  # triangular dither against banding
    rgb = np.clip((img + dither) * 255 + 0.5, 0, 255).astype(np.uint8)
    im = Image.fromarray(rgb, "RGB")
    im.save(f"{out_dir}/wp_{wid}{suffix}.webp", "WEBP", quality=86, method=6)
    if not suffix:
        im.resize((270, 585), Image.LANCZOS).save(f"{out_dir}/wp_{wid}_thumb.webp", "WEBP", quality=86, method=6)
    print("saved", wid + suffix, im.size)


def shade_relief(mask, colour, light=(-0.55, -0.75, 0.9), depth=55.0, spec=0.22, sigma=0.05):
    """Clay relief: a dome from the blurred mask, lit from the top left, with a soft highlight."""
    h = blur(mask, sigma * S) * mask
    gy, gx = np.gradient(h)
    nx, ny, nz = -gx * depth, -gy * depth, np.ones_like(h)
    n = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    lx, ly, lz = (np.array(light) / np.linalg.norm(light)).tolist()
    diffuse = np.clip((nx * lx + ny * ly + nz * lz) / n, 0, 1)
    rz = 2 * diffuse * (nz / n) - lz
    specular = np.clip(rz, 0, 1) ** 24 * spec
    lit = colour[None, None, :] * (0.52 + 0.62 * diffuse[..., None]) + specular[..., None]
    return lit


def clay_scene(base_top, base_bottom, pebbles, seed, shadow_tint):
    img = vgrad([(0, hexc(base_top)), (1, hexc(base_bottom))])
    img = mix(img, hexc(base_top) * 1.04, blob(0.2, 0.15, 0.6, 0.3) * 0.5)
    for i, (cx, cy, rx, ry, ang, col) in enumerate(pebbles):
        m = ellipse_mask(cx * W, cy * H, rx * S, ry * S, ang, 0.08, seed + i, soft=2.0)
        sh = blur(np.roll(np.roll(m, int(0.035 * S), 0), int(0.02 * S), 1), 0.04 * S)
        img = mix(img, hexc(shadow_tint), sh * 0.32)
        contact = blur(np.roll(m, int(0.01 * S), 0), 0.008 * S)
        img = mix(img, hexc(shadow_tint), contact * 0.18)
        img = mix(img, shade_relief(m, hexc(col)), m)
    return grain(img, 0.006, seed)


def frosted(img, mask, tint, tint_amount, refract=(0, 0), blur_sigma=26, rim=(1, 1, 1), rim_strength=0.55):
    """Glass: blurred, slightly shifted background inside the mask, a light rim and a soft drop shadow."""
    behind = np.roll(np.roll(img, int(refract[1] * K), 0), int(refract[0] * K), 1)
    behind = np.stack([blur(behind[..., c], blur_sigma * K) for c in range(3)], -1)
    glass = mix(behind, np.array(tint, np.float32), np.full(mask.shape, tint_amount))
    shadow = blur(np.roll(mask, int(18 * K), 0), 22 * K) * 0.16
    out = mix(img, np.zeros(3, np.float32), shadow)
    out = mix(out, glass, mask)
    inner = blur(mask, 6)
    edge = np.clip(mask - inner, 0, 1) * 3.0
    gy, gx = np.gradient(blur(mask, 10))
    toward_light = np.clip(-(gx * 0.6 + gy * 0.8) * 40, 0, 1)
    return screen(out, np.array(rim, np.float32), (edge * (0.35 + toward_light)) * rim_strength)


# ---------------------------------------------------------------- wallpapers

def crystal_sea():
    img = vgrad([(0, hexc("#2C5C8E")), (0.35, hexc("#123A66")), (0.75, hexc("#0A2547")), (1, hexc("#163B66"))])
    beam_d = (XX * 0.9 - YY * 0.42 + 120) / W
    img = screen(img, hexc("#CFE6FF"), np.exp(-(beam_d / 0.16) ** 2) * 0.55 * np.clip(1.15 - V, 0, 1))
    img = screen(img, hexc("#9CC6F2"), blob(1.0, 1.0, 0.7, 0.25) * 0.35)
    # a long crystalline ribbon drifting across the lower half
    t = np.linspace(0, 1, 400)
    cx = -0.15 + 1.3 * t
    cy = 0.74 - 0.22 * t + 0.07 * np.sin(t * 5.5)
    width = 0.07 + 0.09 * np.sin(np.pi * t) ** 1.4
    ribbon = np.zeros((H, W), np.float32)
    for x, y, r in zip(cx, cy, width):
        ribbon = np.maximum(ribbon, np.clip(1 - np.hypot(XX - x * W, YY - y * H) / (r * S), 0, 1))
    m = np.clip(ribbon * 6, 0, 1)
    img = mix(img, hexc("#0E2C52"), m * 0.55)
    rng = np.random.default_rng(4)
    pts = []
    while len(pts) < 260:
        x, y = rng.random() * W, rng.random() * H
        if m[int(y), int(x)] > 0.5:
            pts.append((x, y))
    pts = np.array(pts)
    layer = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(layer)
    for i, p in enumerate(pts):
        dist = np.hypot(*(pts - p).T)
        for j in np.argsort(dist)[1:4]:
            d.line([tuple(p), tuple(pts[j])], fill=150, width=2)
    lines = np.asarray(layer).astype(np.float32) / 255 * m
    img = screen(img, hexc("#9FC8F2"), blur(m, 30) * 0.18)
    img = screen(img, hexc("#E4F1FF"), lines * 0.45 + blur(lines, 5) * 0.6)
    edge = np.clip(m - blur(m, 10), 0, 1) * 2.0
    img = screen(img, hexc("#DDEEFF"), edge * 0.45)
    img = stars(img, 240, 5, weight=np.clip(0.35 + np.exp(-(beam_d / 0.3) ** 2), 0, 1))
    for x, y, r in [(0.17, 0.41, 26), (0.74, 0.27, 20), (0.86, 0.6, 30), (0.28, 0.86, 18)]:
        img = sparkle(img, x * W, y * H, r * K, 0.9)
    return grain(img, 0.008, 1)


def aurora_veil():
    img = vgrad([(0, hexc("#040812")), (0.5, hexc("#08182A")), (0.62, hexc("#0C2231")), (1, hexc("#03070D"))])
    horizon = 0.62
    for k, (base, height, c1, c2, seed, amp) in enumerate([
        (0.34, 0.2, "#4BF2C4", "#8F7BFF", 3, 1.0),
        (0.44, 0.14, "#59E8A6", "#5BC0FF", 8, 0.65),
    ]):
        xs = smooth_noise(70, seed, 1, W + 200)[0, 100:100 + W]
        curve = base + 0.05 * np.sin(U[0] * 7 + k) + 0.03 * (xs - 0.5)
        strength = np.clip((smooth_noise(90, seed + 1, 1, W + 200)[0, 100:100 + W] - 0.3) * 1.8, 0, 1) ** 1.3 * amp
        streak = 0.55 + 0.45 * smooth_noise(4, seed + 2, 1, W + 200)[0, 100:100 + W]
        yb = curve[None, :] * H
        above = np.exp(-np.clip(yb - YY, 0, None) / (height * H))
        below = np.exp(-np.clip(YY - yb, 0, None) / (0.012 * H))
        inten = above * below * (strength * streak)[None, :]
        frac = np.clip((yb - YY) / (height * H), 0, 1)
        colour = hexc(c1)[None, None, :] * (1 - frac[..., None]) + hexc(c2)[None, None, :] * frac[..., None]
        img = 1 - (1 - img) * (1 - colour * np.clip(inten, 0, 1)[..., None] * 0.9)
    sky = (V < horizon).astype(np.float32)
    img = stars(img, 320, 9, weight=np.clip(sky * (1.2 - V * 1.4), 0, 1))
    # still lake: mirrored sky, darker, broken by thin ripples
    sy = np.clip((2 * horizon - V) * H, 0, H - 1).astype(int)
    mirror = img[sy, XX.astype(int)]
    mirror = np.stack([blur(mirror[..., c], 6) for c in range(3)], -1)
    ripple = 0.86 + 0.14 * np.sin(YY * 0.22 + smooth_noise(60, 12) * 6)
    lake = mirror * 0.62 * ripple[..., None]
    img = np.where((V > horizon)[..., None], lake + hexc("#03080E") * 0.6, img)
    img = mix(img, hexc("#02050A"), np.clip((V - horizon) * 2.2, 0, 1) * 0.55)
    img = screen(img, hexc("#7FE9D0"), np.exp(-((V - horizon) * H / 2.5) ** 2) * 0.25)
    return grain(img, 0.007, 2)


def moon_tide():
    horizon = 0.62
    img = vgrad([(0, hexc("#0F1433")), (0.4, hexc("#2A2660")), (horizon, hexc("#6A4F8F")), (horizon + 0.001, hexc("#2A234E")), (1, hexc("#0C0E24"))])
    mx, my, mr = 0.5 * W, 0.36 * H, 0.17 * S
    d = np.hypot(XX - mx, YY - my)
    img = screen(img, hexc("#F6D9C4"), np.exp(-((d - mr) / (0.35 * S)).clip(0) ** 2 * 3) * 0.28 * (d > mr))
    disc = np.clip((mr - d) / 1.5, 0, 1)
    crater = smooth_noise(40, 21) * 0.5 + smooth_noise(14, 22) * 0.5
    moon = hexc("#F7EEE2")[None, None, :] * (0.86 + 0.14 * crater[..., None])
    shade = np.clip(1 - ((XX - mx + mr * 0.25) ** 2 + (YY - my + mr * 0.2) ** 2) / (mr * 1.6) ** 2, 0.75, 1)
    img = mix(img, moon * shade[..., None], disc)
    img = stars(img, 160, 23, weight=np.clip((horizon - V) * 2, 0, 1) * (d > mr * 1.6))
    sea = V > horizon
    rng = np.random.default_rng(24)
    layer = Image.new("L", (W, H), 0)
    dr = ImageDraw.Draw(layer)
    for _ in range(520):
        y = horizon * H + (rng.random() ** 1.7) * (1 - horizon) * H
        spread = 0.05 * W + (y / H - horizon) * 0.55 * W
        x = mx + rng.normal(0, spread * 0.45)
        ln = rng.uniform(10, 50) * (0.5 + (y / H - horizon) * 3)
        dr.line([(x - ln, y), (x + ln, y)], fill=int(rng.uniform(80, 230)), width=2)
    refl = np.asarray(layer).astype(np.float32) / 255
    img = screen(img, hexc("#F3DCC7"), (refl + blur(refl, 3) * 0.8) * sea * 0.8)
    img = screen(img, hexc("#C9A8E6"), np.exp(-((V - horizon) * H / 3) ** 2) * 0.5)
    return grain(img, 0.007, 3)


def glass_bloom():
    img = np.ones((H, W, 3), np.float32) * hexc("#F4F1FB")
    for cx, cy, rx, ry, col, a in [
        (0.15, 0.2, 0.55, 0.22, "#C9B8FF", 0.9), (0.9, 0.38, 0.5, 0.2, "#FFC6AE", 0.85),
        (0.25, 0.72, 0.55, 0.22, "#A9E4F0", 0.85), (0.85, 0.86, 0.5, 0.2, "#F6B3D6", 0.8),
        (0.55, 0.52, 0.4, 0.15, "#FFE7B8", 0.5),
    ]:
        img = mix(img, hexc(col), blob(cx, cy, rx, ry) * a)
    disc = ellipse_mask(0.6 * W, 0.4 * H, 0.3 * S, 0.3 * S, soft=2)
    img = frosted(img, disc, (1, 1, 1), 0.22, refract=(-30, 20), blur_sigma=30)
    tile = rounded_rect_mask(0.33 * W, 0.67 * H, 0.46 * S, 0.46 * S, 0.11 * S, angle=-0.28, soft=2)
    img = frosted(img, tile, (1, 1, 1), 0.28, refract=(24, -16), blur_sigma=30)
    pill = rounded_rect_mask(0.74 * W, 0.8 * H, 0.2 * S, 0.2 * S, 0.1 * S, soft=2)
    img = frosted(img, pill, (1, 1, 1), 0.2, refract=(-10, 10), blur_sigma=24)
    return grain(img, 0.005, 4)


def prism_night():
    img = np.ones((H, W, 3), np.float32) * hexc("#0B0C13")
    for cx, cy, rx, ry, col, a in [
        (0.2, 0.3, 0.45, 0.18, "#5B3BF0", 0.9), (0.85, 0.45, 0.4, 0.16, "#00C2D1", 0.6),
        (0.3, 0.8, 0.5, 0.18, "#C0279E", 0.7), (0.75, 0.15, 0.35, 0.12, "#2457FF", 0.5),
    ]:
        img = mix(img, hexc(col), blob(cx, cy, rx, ry) * a)
    for (cx, cy, w, h, r, ang, ref) in [
        (0.4, 0.42, 0.62, 0.36, 0.08, 0.5, (-36, 18)),
        (0.62, 0.62, 0.5, 0.3, 0.07, -0.35, (30, -10)),
        (0.36, 0.76, 0.34, 0.2, 0.06, 0.15, (-14, 22)),
    ]:
        m = rounded_rect_mask(cx * W, cy * H, w * S, h * S, r * S, ang, soft=2)
        img = frosted(img, m, hexc("#FFFFFF"), 0.06, refract=ref, blur_sigma=34, rim=(0.85, 0.9, 1), rim_strength=0.7)
    img = mix(img, hexc("#07080D"), np.clip(V - 0.85, 0, 1) * 3 * 0.5)
    return grain(img, 0.008, 5)


def silk():
    """Satin folds: a domain-warped band field shaded like cloth, with a narrow sheen on each crest."""
    u, v = U, V
    warp = 0.22 * np.sin(u * 3.2 + v * 2.1 + 0.4) + 0.12 * np.sin(u * 7.3 - v * 3.7 + 1.9)
    d = v * 2.6 - u * 0.9 + warp
    s = 0.5 + 0.5 * np.sin(2 * np.pi * d)
    s = s * s * (3 - 2 * s)
    t = np.clip(v * 0.9 + 0.08 * (s - 0.5), 0, 1)
    pos = [0, 0.3, 0.65, 1]
    cols = [hexc("#16183A"), hexc("#3E3488"), hexc("#9A6CC4"), hexc("#EFB9C4")]
    img = np.stack([np.interp(t, pos, [c[i] for c in cols]) for i in range(3)], -1).astype(np.float32)
    img = img * (0.55 + 0.6 * s[..., None])
    sheen = np.exp(-((s - 0.985) / 0.035) ** 2) * (0.4 + 0.6 * smooth_noise(160, 61))
    img = screen(img, hexc("#FFEFF4"), sheen * 0.45)
    img = mix(img, hexc("#0B0C22"), np.clip(0.3 - v, 0, 1) * 1.8 * 0.5)
    return grain(img, 0.005, 6)


def dunes():
    img = vgrad([(0, hexc("#FFF1E4")), (0.45, hexc("#FBD6BA")), (0.6, hexc("#F4BE9C")), (1, hexc("#F0B08E"))])
    img = screen(img, hexc("#FFF9F0"), blob(0.68, 0.3, 0.35, 0.16) * 0.85)
    sun = np.clip((0.09 * S - np.hypot(XX - 0.68 * W, YY - 0.3 * H)) / 1.5, 0, 1)
    img = mix(img, hexc("#FFF8EE"), sun * 0.9)
    layers = [
        (0.52, "#F2C3A2", "#E8A989", 0.03, 1.1, 0.3),
        (0.6, "#EDB08C", "#DB9070", 0.04, 1.7, 1.3),
        (0.69, "#E39A74", "#C9775A", 0.05, 1.3, 2.1),
        (0.79, "#D3805C", "#B0603F", 0.05, 2.1, 0.7),
        (0.9, "#B9653F", "#8E4529", 0.04, 1.5, 2.8),
    ]
    for base, lit, dark, amp, freq, ph in layers:
        u = U[0]
        crest1 = (base + amp * np.sin(u * freq * 3.1 + ph) + amp * 0.5 * np.sin(u * freq * 7 + ph * 2)) * H
        crest = crest1[None, :]
        inside = np.clip((YY - crest) / 1.5, 0, 1)
        slope = np.gradient(crest1)[None, :] * 0.04
        light = np.broadcast_to(np.clip(0.5 - slope, 0, 1), (H, W))
        depth = np.clip((YY - crest) / (0.18 * H), 0, 1)
        col = hexc(lit)[None, None, :] * (1 - depth[..., None] * 0.6) + hexc(dark)[None, None, :] * (depth[..., None] * 0.6)
        col = col * (0.88 + 0.24 * light[..., None])
        rim = np.exp(-((YY - crest) / 3) ** 2) * 0.35
        img = mix(img, col, inside)
        img = screen(img, hexc("#FFE6D2"), rim * (light > 0.45))
    return grain(img, 0.005, 7)


def pebbles():
    return clay_scene("#EFEAE4", "#DCD4CB", [
        (0.3, 0.28, 0.26, 0.21, 0.3, "#D9896A"),
        (0.74, 0.4, 0.2, 0.17, -0.5, "#A9B79B"),
        (0.38, 0.58, 0.18, 0.15, 0.9, "#8F9BB2"),
        (0.72, 0.72, 0.27, 0.2, 0.2, "#E8C79E"),
        (0.28, 0.85, 0.15, 0.12, -0.2, "#F3ECE3"),
    ], 30, "#5A4A3C")


def dusk_clay():
    return clay_scene("#1D1E26", "#121318", [
        (0.68, 0.26, 0.24, 0.2, 0.4, "#6B4A8C"),
        (0.3, 0.42, 0.2, 0.16, -0.3, "#2F5B7A"),
        (0.66, 0.6, 0.17, 0.14, 0.8, "#B0603F"),
        (0.32, 0.74, 0.27, 0.2, 0.1, "#3A3D52"),
        (0.72, 0.87, 0.14, 0.11, -0.4, "#C79A5A"),
    ], 40, "#000000")



# ---------------------------------------------------------------- cosmic

def fbm(seed, scales=(220, 110, 55, 27, 13), weights=(0.5, 0.25, 0.13, 0.08, 0.04)):
    out = np.zeros((H, W), np.float32)
    for i, (sc, wt) in enumerate(zip(scales, weights)):
        out += smooth_noise(sc, seed + i) * wt
    return out / sum(weights)


def ridges(seed, sharp=7.0):
    """Ridged noise: thin bright veins where the field crosses its middle."""
    n = fbm(seed)
    r = 1 - np.abs(n * 2 - 1)
    return np.clip(r, 0, 1) ** sharp


def deep_space(seed):
    img = vgrad([(0, hexc("#05040E")), (0.5, hexc("#080617")), (1, hexc("#04030B"))])
    img = screen(img, hexc("#1A1450"), fbm(seed + 50) ** 2 * 0.6)
    return img


def nebula(img, seed, palette, strength=1.0):
    veins = ridges(seed, 6.5)
    fine = ridges(seed + 10, 10.0)
    hue = fbm(seed + 20)
    cols = [hexc(c) for c in palette]
    t = np.clip((hue - 0.3) * 2.5, 0, 1)
    col = np.stack([np.interp(t, np.linspace(0, 1, len(cols)), [c[i] for c in cols]) for i in range(3)], -1).astype(np.float32)
    density = np.clip((fbm(seed + 30) - 0.35) * 2.2, 0, 1)
    glow = blur(veins * density, 6)
    img = screen(img, col, (veins * 0.9 + glow * 1.4) * density * strength)
    img = screen(img, np.array([1, 0.92, 1], np.float32), fine * density * 0.55 * strength)
    return img


def planet(img, cx, cy, r, base, rim, seed, light=(-0.6, -0.5), bands=False):
    dx, dy = (XX - cx) / r, (YY - cy) / r
    d2 = dx ** 2 + dy ** 2
    inside = np.clip((1 - np.sqrt(d2)) * r / 1.5, 0, 1)
    nz = np.sqrt(np.clip(1 - d2, 0, 1))
    lx, ly = light
    lz = math.sqrt(max(0.0, 1 - lx * lx - ly * ly))
    diffuse = np.clip(dx * lx + dy * ly + nz * lz, 0, 1)
    if bands:
        tex = 0.65 + 0.35 * np.sin((dy * 9 + fbm(seed) * 3.0) * 2.2)
    else:
        rough = fbm(seed, (90, 45, 22, 11, 6))
        tex = 0.35 + 0.9 * np.clip((rough - 0.25) * 1.6, 0, 1) * (0.75 + 0.25 * smooth_noise(4, seed + 9))
    surface = hexc(base)[None, None, :] * tex[..., None] * (0.12 + 0.95 * diffuse[..., None])
    fres = (1 - nz) ** 3 * inside
    surface = screen(surface, hexc(rim), fres * 0.9 + (1 - nz) ** 8 * 0.6)
    img = mix(img, surface, inside)
    atmo = np.exp(-np.clip(np.sqrt(d2) - 1, 0, None) * r / (0.035 * S)) * (d2 > 1)
    return screen(img, hexc(rim), atmo * 0.65)


def starburst(img, x, y, r, colour, rays=18, seed=0, strength=1.0):
    ang = np.arctan2(YY - y, XX - x)
    dist = np.hypot(XX - x, YY - y)
    rng = np.random.default_rng(seed)
    phases = rng.random(rays) * 0.5
    ray = np.zeros_like(dist)
    for k in range(rays):
        a = k / rays * 2 * np.pi + phases[k]
        ray = np.maximum(ray, np.exp(-((np.angle(np.exp(1j * (ang - a)))) / 0.02) ** 2) * rng.uniform(0.4, 1.0))
    fall = np.exp(-dist / (r * 0.38))
    core = np.exp(-(dist / (r * 0.12)) ** 2)
    halo = np.exp(-(dist / (r * 0.45)) ** 2)
    img = screen(img, hexc(colour), (ray * fall * 0.9 + halo * 0.55) * strength)
    return screen(img, np.array([1, 0.98, 0.92], np.float32), core * 1.3 * strength)


def nebula_veins():
    img = deep_space(70)
    img = nebula(img, 71, ["#3A1BD8", "#B21FD6", "#FF2E6E", "#FF4B2B"])
    img = stars(img, 900, 72, size=(0.5, 1.4), bright=(0.3, 1.0))
    for x, y, r in [(0.82, 0.1, 22), (0.2, 0.62, 16), (0.7, 0.78, 26)]:
        img = sparkle(img, x * W, y * H, r * K, 0.8, tint=(1, 0.85, 0.95))
    return grain(img, 0.01, 73)


def twin_worlds():
    img = deep_space(80)
    img = nebula(img, 81, ["#2A2BE0", "#9B1FD6", "#FF2E8A", "#FF3B2B"], 0.85)
    img = stars(img, 700, 82, size=(0.5, 1.4))
    img = planet(img, -0.22 * W, 0.78 * H, 0.78 * S, "#1E5570", "#4FE3FF", 83, light=(0.55, -0.75))
    img = planet(img, 0.58 * W, 0.24 * H, 0.16 * S, "#7C8690", "#9FE6FF", 84, light=(-0.7, 0.35))
    img = starburst(img, 0.44 * W, 0.3 * H, 0.42 * S, "#FFB347", rays=22, seed=85)
    img = planet(img, 0.84 * W, 0.86 * H, 0.045 * S, "#6B7C8A", "#B8E8FF", 86, light=(-0.6, -0.4))
    return grain(img, 0.01, 87)


def ringed_giant():
    img = deep_space(90)
    img = nebula(img, 91, ["#1B2BD8", "#5B1FD6", "#C21FD6"], 0.6)
    img = stars(img, 600, 92, size=(0.5, 1.3))
    cx, cy, r = 0.5 * W, 0.58 * H, 0.34 * S
    # rings behind the planet (upper half), planet, rings in front (lower half)
    def ring_layer(front):
        ang = -0.35
        ca, sa = math.cos(ang), math.sin(ang)
        x = (XX - cx) * ca + (YY - cy) * sa
        y = -(XX - cx) * sa + (YY - cy) * ca
        e = np.sqrt((x / (r * 1.9)) ** 2 + (y / (r * 0.42)) ** 2)
        band = np.clip(1 - np.abs(e - 0.82) / 0.16, 0, 1) * (0.55 + 0.45 * np.sin(e * 90))
        side = (y > 0) if front else (y <= 0)
        return band * side
    img = screen(img, hexc("#E8D7FF"), ring_layer(False) * 0.5)
    img = planet(img, cx, cy, r, "#7A5CC9", "#9FB7FF", 93, light=(-0.55, -0.6), bands=True)
    img = screen(img, hexc("#F3E8FF"), ring_layer(True) * 0.75)
    img = planet(img, 0.18 * W, 0.2 * H, 0.05 * S, "#8B9BB8", "#C9DAFF", 94, light=(0.6, -0.3))
    return grain(img, 0.01, 95)


def supernova():
    img = deep_space(100)
    img = nebula(img, 101, ["#0E5BFF", "#7A1FFF", "#FF2E9A", "#FF8A2B"], 1.1)
    cx, cy = 0.5 * W, 0.46 * H
    d = np.hypot(XX - cx, YY - cy) / S
    shell = np.exp(-((d - 0.32) / 0.05) ** 2) * (0.5 + 0.5 * fbm(102))
    img = screen(img, hexc("#FF5FA8"), shell * 0.6)
    img = screen(img, hexc("#6FB8FF"), np.exp(-((d - 0.2) / 0.04) ** 2) * fbm(103) * 0.5)
    img = stars(img, 800, 104, size=(0.5, 1.4))
    img = starburst(img, cx, cy, 0.55 * S, "#9FD0FF", rays=26, seed=105, strength=1.1)
    return grain(img, 0.01, 106)


# ---------------------------------------------------------------- minimal, line art, layers, material, geometric

def smooth_field(seed, big=1.0):
    """Large, calm noise: only broad scales, sized to the short side so it reads the same on any canvas."""
    sc = [int(S * 0.62 * big), int(S * 0.31 * big), int(S * 0.16 * big)]
    return fbm(seed, tuple(max(8, v) for v in sc), (0.6, 0.28, 0.12))


def mesh(base, blobs, seed, grain_amt=0.006):
    """Soft mesh gradient: large overlapping colour fields over a base, finished with fine grain."""
    img = np.ones((H, W, 3), np.float32) * hexc(base)
    for cx, cy, r, col, a in blobs:
        d = np.hypot((XX - cx * W) / S, (YY - cy * H) / S)
        img = mix(img, hexc(col), np.exp(-(d / r) ** 2) * a)
    return grain(img, grain_amt, seed)


def mesh_dawn():
    return mesh("#FBF6EF", [
        (0.05, 0.08, 0.75, "#FFC9A8", 0.95), (0.95, 0.3, 0.7, "#D9C8FF", 0.9),
        (0.15, 0.7, 0.8, "#BFEBDD", 0.85), (0.9, 0.95, 0.7, "#FFE3A3", 0.85), (0.55, 0.5, 0.45, "#FFFFFF", 0.5),
    ], 110)


def mesh_ink():
    return mesh("#0A0B10", [
        (0.0, 0.1, 0.8, "#0F4C5C", 0.95), (1.0, 0.35, 0.75, "#2A2F8F", 0.9),
        (0.2, 0.85, 0.85, "#4E2468", 0.9), (0.85, 0.95, 0.5, "#9C3E28", 0.65),
    ], 111, 0.008)


def contour():
    """Topographic map: isolines of a smooth height field, every fifth line heavier."""
    img = vgrad([(0, hexc("#102024")), (1, hexc("#0A1013"))])
    f = smooth_field(120, 1.1)
    levels = 24
    g = f * levels
    gy, gx = np.gradient(g)
    slope = np.hypot(gx, gy) + 1e-6
    dist = np.abs(g - np.round(g)) / slope
    index = (np.round(g).astype(int) % 5 == 0)
    width = np.where(index, 1.8 * K, 1.0 * K)
    line = np.clip(1 - dist / width, 0, 1)
    img = screen(img, hexc("#9CCFC2"), line * np.where(index, 0.85, 0.5))
    img = mix(img, hexc("#05090B"), np.clip(np.hypot(U - 0.5, V - 0.5) * 1.3 - 0.35, 0, 1) * 0.6)
    return grain(img, 0.005, 121)


def waveform():
    """Ridgeline: stacked lines lifted by noise in the middle, each hiding the ones behind it."""
    bg = (13, 16, 33)
    im = Image.new("RGB", (W, H), bg)
    d = ImageDraw.Draw(im)
    rng = np.random.default_rng(130)
    lines = 58
    top, bottom = 0.2 * H, 0.86 * H
    xs = np.linspace(0, W, 220)
    u = xs / W
    for i in range(lines):
        base = top + (bottom - top) * i / (lines - 1)
        env = np.exp(-((u - 0.5) / 0.2) ** 2) * (0.35 + 0.65 * np.sin(np.pi * i / lines))
        n = np.interp(u, np.linspace(0, 1, 30), rng.random(30))
        n = np.convolve(n, np.ones(9) / 9, mode="same")
        peaks = np.clip(n - 0.3, 0, None) ** 1.6 * 3.2
        y = base - env * peaks * 0.11 * H
        pts = list(zip(xs.tolist(), y.tolist()))
        d.polygon(pts + [(W, H), (0, H)], fill=bg)
        d.line(pts, fill=(242, 233, 220), width=max(2, int(2.2 * K)), joint="curve")
    img = np.asarray(im).astype(np.float32) / 255
    img = screen(img, hexc("#3B4BB0"), np.exp(-((V - 0.5) / 0.35) ** 2) * 0.12)
    return grain(img, 0.006, 131)


def paper_layers():
    """Papercut: wavy layers from a pale sky down to deep plum, each with a soft halo shadow."""
    img = vgrad([(0, hexc("#FCF1E6")), (0.45, hexc("#F8DCC8")), (1, hexc("#F3C7AE"))])
    img = screen(img, hexc("#FFF8EE"), np.exp(-(np.hypot((XX - 0.7 * W) / S, (YY - 0.3 * H) / S) / 0.22) ** 2) * 0.9)
    cols = ["#EFC0A4", "#E29F86", "#CF7F78", "#B06474", "#874E6E", "#5C3D62", "#33284A"]
    for i, col in enumerate(cols):
        base = 0.42 + i * 0.075
        u = U[0]
        edge = (base + 0.035 * np.sin(u * (2.6 + i * 0.35) * np.pi + i * 1.7) + 0.018 * np.sin(u * 9 + i)) * H
        m = np.clip((YY - edge[None, :]) / 1.5, 0, 1)
        halo = blur(m, 16 * K) * (1 - m)
        img = mix(img, hexc("#2A1A2A"), halo * 0.45)
        shade = np.clip((YY - edge[None, :]) / (0.12 * H), 0, 1)
        layer = hexc(col)[None, None, :] * (1.02 - shade[..., None] * 0.12)
        img = mix(img, layer, m)
    return grain(img, 0.005, 140)


def misty_peaks():
    """Blue-hour mountains: ridges fade with distance into horizontal mist below a low, warm sky."""
    img = vgrad([(0, hexc("#1B2747")), (0.36, hexc("#46507F")), (0.54, hexc("#D99A8F")), (1, hexc("#E7B2A0"))])
    sun = np.hypot((XX - 0.62 * W) / S, (YY - 0.5 * H) / S)
    img = screen(img, hexc("#FFE1C8"), np.exp(-(sun / 0.08) ** 2) * 0.9 + np.exp(-(sun / 0.35) ** 2) * 0.25)
    far, near = hexc("#C4919A"), hexc("#141A33")
    u = U[0]
    for i in range(6):
        t = i / 5
        rng = np.random.default_rng(150 + i)
        ph = rng.random(4) * 6
        # A few broad sines make believable massifs; a little smooth noise roughens the skyline.
        shape = (0.55 * np.sin(u * 2.1 * np.pi + ph[0]) + 0.3 * np.sin(u * 4.3 * np.pi + ph[1])
                 + 0.15 * np.sin(u * 9.7 * np.pi + ph[2]))
        rough = smooth_noise(max(8, int(S * 0.04)), 160 + i, 1, W + 200)[0, 100:100 + W] - 0.5
        base = (0.5 + t * 0.08) * H
        ridge = base - (shape * 0.5 + 0.5) * (0.09 - t * 0.035) * H - rough * 0.012 * H
        m = np.clip((YY - ridge[None, :]) / 1.5, 0, 1)
        col = far * (1 - t) + near * t
        img = mix(img, col[None, None, :] * np.ones((H, W, 3), np.float32), m)
        mist = np.exp(-np.clip(YY - base, 0, None) / (0.035 * H)) * np.clip((YY - (base - 0.03 * H)) / (0.03 * H), 0, 1)
        img = screen(img, hexc("#F4D3CC"), mist * m * (0.42 - t * 0.06))
    return grain(img, 0.006, 151)


def calm_sea():
    """Minimal seascape: pale sky, a low sun on a clean horizon, and a soft shimmer on the water."""
    horizon = 0.6
    img = vgrad([(0, hexc("#F7EEE4")), (horizon - 0.001, hexc("#F3CDB0")), (horizon, hexc("#A9BCCB")), (1, hexc("#3E5A72"))])
    sun_d = np.hypot((XX - 0.5 * W) / S, (YY - horizon * H) / S)
    sky = V < horizon
    img = mix(img, hexc("#FFF6EA"), np.clip((0.06 - sun_d) * S / 1.5, 0, 1) * sky)
    img = screen(img, hexc("#FFE6CF"), np.exp(-(sun_d / 0.25) ** 2) * 0.35 * sky)
    rng = np.random.default_rng(170)
    layer = Image.new("L", (W, H), 0)
    dr = ImageDraw.Draw(layer)
    for _ in range(int(380 * (W * H) / (1080 * 2340))):
        y = horizon * H + (rng.random() ** 1.5) * (1 - horizon) * H
        depth = (y / H - horizon) / (1 - horizon)
        x = 0.5 * W + rng.normal(0, 0.03 * S + depth * 0.12 * S)
        ln = (6 + rng.random() * 26) * K * (0.6 + depth * 2)
        dr.line([(x - ln, y), (x + ln, y)], fill=int(rng.uniform(90, 220)), width=max(1, int(1.6 * K)))
    refl = np.asarray(layer).astype(np.float32) / 255
    img = screen(img, hexc("#FFF1E2"), refl * 0.6 * (V >= horizon))
    img = screen(img, hexc("#FFFFFF"), np.exp(-((V - horizon) * H / (1.2 * K)) ** 2) * 0.4)
    return grain(img, 0.004, 171)


def glass_orbs():
    """Three glass spheres refracting a dark mesh: inverted view through each, a lit rim and a highlight."""
    img = mesh("#121320", [(0.1, 0.15, 0.7, "#5B3BF0", 0.7), (0.9, 0.45, 0.6, "#00A6B8", 0.55), (0.3, 0.9, 0.7, "#C0279E", 0.6)], 180, 0.0)
    base = img.copy()
    for cx, cy, r in [(0.62, 0.34, 0.24), (0.3, 0.6, 0.17), (0.72, 0.8, 0.1)]:
        cx, cy, r = cx * W, cy * H, r * S
        dx, dy = (XX - cx) / r, (YY - cy) / r
        d2 = dx ** 2 + dy ** 2
        inside = np.clip((1 - np.sqrt(d2)) * r / 1.5, 0, 1)
        nz = np.sqrt(np.clip(1 - d2, 0, 1))
        sx = np.clip(cx - dx * r * 0.9 * nz, 0, W - 1).astype(int)
        sy = np.clip(cy - dy * r * 0.9 * nz, 0, H - 1).astype(int)
        seen = base[sy, sx]
        shadow = blur(np.clip((1 - np.hypot((XX - cx - 0.15 * r) / r, (YY - cy - 1.05 * r) / (0.25 * r))), 0, 1), 0.2 * r)
        img = mix(img, np.zeros(3, np.float32), shadow * 0.45)
        fres = (1 - nz) ** 2.5
        sphere = screen(seen * 1.15, np.array([0.85, 0.9, 1.0], np.float32), fres * 0.85)
        spec = np.exp(-(((dx + 0.38) / 0.22) ** 2 + ((dy + 0.45) / 0.14) ** 2))
        sphere = screen(sphere, np.ones(3, np.float32), spec * 0.9)
        img = mix(img, sphere, inside)
    return grain(img, 0.006, 181)


def liquid_chrome():
    """Molten metal: a warped field read through a banded chrome ramp, with a cool and warm cast."""
    n1, n2 = smooth_field(190, 1.4), smooth_field(191, 1.4)
    f = np.sin(U * 3.2 + n1 * 4.0 + 1.6 * np.sin(V * 2.6 + n2 * 3.0)) + 0.5 * np.sin(V * 3.4 + n2 * 2.0)
    t = 0.5 + 0.5 * np.cos(f * 1.7)
    pos = [0, 0.18, 0.32, 0.5, 0.62, 0.8, 1]
    ramp = ["#07080C", "#2B313D", "#8D97A8", "#F1F4F9", "#6E7889", "#161A22", "#C9D3E3"]
    img = np.stack([np.interp(t, pos, [hexc(c)[i] for c in ramp]) for i in range(3)], -1).astype(np.float32)
    img = img * (1 + 0.06 * np.stack([np.sin(f * 3), np.zeros_like(f), np.cos(f * 3)], -1))
    img = mix(img, hexc("#05060A"), np.clip(np.hypot(U - 0.5, V - 0.5) * 1.4 - 0.4, 0, 1) * 0.7)
    return grain(img, 0.005, 192)


def iridescent():
    """Holographic foil: pastel hues cycling over a warped field, with soft sheen bands."""
    n1, n2 = smooth_field(200, 1.3), smooth_field(201, 1.3)
    t = U * 0.5 + V * 0.35 + n1 * 1.1 + 0.25 * np.sin(n2 * 4)
    a = np.array([0.82, 0.8, 0.86], np.float32)
    b = np.array([0.16, 0.18, 0.14], np.float32)
    d = np.array([0.0, 0.33, 0.67], np.float32)
    img = a + b * np.cos(2 * np.pi * (t[..., None] + d))
    sheen = np.exp(-((np.sin(t * 6) - 0.94) / 0.06) ** 2)
    img = screen(img, np.ones(3, np.float32), sheen * 0.35)
    return grain(img, 0.005, 202)


def bauhaus_shapes():
    """Flat geometric composition on warm paper: a sun disc, half and quarter circles, fine rules."""
    img = np.ones((H, W, 3), np.float32) * hexc("#EFE6D8")
    img = mix(img, hexc("#E7DCCB"), smooth_noise(int(6 * K) + 2, 210) * 0.5)

    def disc(cx, cy, r):
        return np.clip((r - np.hypot(XX - cx, YY - cy)) / 1.5, 0, 1)

    img = mix(img, hexc("#E0A32C"), disc(0.66 * W, 0.36 * H, 0.27 * S))
    half = disc(0.0, 0.74 * H, 0.42 * S) * (XX > 0)
    img = mix(img, hexc("#C6533B"), half)
    quarter = disc(W, H, 0.5 * S)
    img = mix(img, hexc("#1F3A57"), quarter)
    img = mix(img, hexc("#1B1B1B"), disc(0.24 * W, 0.24 * H, 0.05 * S))
    for k, (x0, y0, x1, y1) in enumerate([(0.08, 0.5, 0.92, 0.47), (0.55, 0.1, 0.62, 0.92)]):
        p0 = np.array([x0 * W, y0 * H]); p1 = np.array([x1 * W, y1 * H])
        dvec = p1 - p0
        tt = np.clip(((XX - p0[0]) * dvec[0] + (YY - p0[1]) * dvec[1]) / (dvec @ dvec), 0, 1)
        dist = np.hypot(XX - (p0[0] + tt * dvec[0]), YY - (p0[1] + tt * dvec[1]))
        img = mix(img, hexc("#1B1B1B"), np.clip(1.2 * K - dist, 0, 1))
    return grain(img, 0.007, 211)


def oled_ring():
    """AMOLED: true black with one thin luminous ring and a small orbiting light."""
    img = np.zeros((H, W, 3), np.float32)
    cx, cy, r = 0.5 * W, 0.52 * H, 0.36 * S
    d = np.hypot(XX - cx, YY - cy)
    ang = (np.arctan2(YY - cy, XX - cx) / (2 * np.pi)) % 1
    cols = [hexc("#4FE3FF"), hexc("#9B6CFF"), hexc("#FF5FA8"), hexc("#4FE3FF")]
    col = np.stack([np.interp(ang, [0, 0.33, 0.66, 1], [c[i] for c in cols]) for i in range(3)], -1).astype(np.float32)
    core = np.clip(1 - np.abs(d - r) / (1.6 * K), 0, 1)
    glow = np.exp(-((d - r) / (14 * K)) ** 2) * 0.55 + np.exp(-((d - r) / (60 * K)) ** 2) * 0.18
    img = screen(img, col, glow) + col * core[..., None] * 0.9
    a = math.radians(-40)
    px, py = cx + r * math.cos(a), cy + r * math.sin(a)
    dot = np.hypot(XX - px, YY - py)
    img = screen(img, np.ones(3, np.float32), np.exp(-(dot / (5 * K)) ** 2) + np.exp(-(dot / (26 * K)) ** 2) * 0.4)
    return np.clip(img, 0, 1)


WALLPAPERS = {
    "crystal_sea": crystal_sea,
    "aurora_veil": aurora_veil,
    "moon_tide": moon_tide,
    "glass_bloom": glass_bloom,
    "prism": prism_night,
    "silk": silk,
    "dunes": dunes,
    "pebbles": pebbles,
    "dusk_clay": dusk_clay,
    "nebula_veins": nebula_veins,
    "twin_worlds": twin_worlds,
    "ringed_giant": ringed_giant,
    "supernova": supernova,
    "mesh_dawn": mesh_dawn,
    "mesh_ink": mesh_ink,
    "contour": contour,
    "waveform": waveform,
    "paper_layers": paper_layers,
    "misty_peaks": misty_peaks,
    "calm_sea": calm_sea,
    "glass_orbs": glass_orbs,
    "liquid_chrome": liquid_chrome,
    "iridescent": iridescent,
    "bauhaus": bauhaus_shapes,
    "oled_ring": oled_ring,
}

if __name__ == "__main__":
    args = sys.argv[1:]
    out = args.pop(0)
    land = "--land" in args
    only = [a for a in args if not a.startswith("--")] or list(WALLPAPERS)
    if land:
        setup(2400, 1500)
    for wid in only:
        save(WALLPAPERS[wid](), out, wid, "_land" if land else "")
