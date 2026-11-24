"""Removes the clock text and divider from the user's whale reference so it can ship as a wallpaper.

Each column of the text box is refilled by blending the clean sky just above and just below it,
with matching grain, then feathered back into the original so no seam shows.
Usage: python3 clean_reference.py <input image> <output.webp>
"""
import sys
import numpy as np
from PIL import Image, ImageFilter

src, out = sys.argv[1], sys.argv[2]
im = Image.open(src).convert("RGB")
a = np.asarray(im).astype(np.float32)
h, w, _ = a.shape

x0, x1, y0, y1 = 350, 780, 526, 686          # text "11:11" and the starred divider, with margin
above = a[y0 - 8:y0].mean(axis=0)            # (w, 3)
below = a[y1:y1 + 8].mean(axis=0)
t = np.linspace(0, 1, y1 - y0)[:, None, None]
fill = above[None] * (1 - t) + below[None] * t
rng = np.random.default_rng(11)
grain = a[y0 - 40:y0, x0:x1].std(axis=(0, 1)) * 0.35
patch = fill[:, x0:x1] + rng.normal(0, 1, (y1 - y0, x1 - x0, 3)) * grain

mask = np.zeros((h, w), np.float32)
mask[y0:y1, x0:x1] = 1
mask = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
full = a.copy()
full[y0:y1, x0:x1] = patch
mixed = a * (1 - mask[..., None]) + full * mask[..., None]
Image.fromarray(np.clip(mixed, 0, 255).astype(np.uint8)).save(out, "WEBP", quality=90, method=6)
print("saved", out, im.size)
