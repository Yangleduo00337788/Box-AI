from __future__ import annotations

import shutil
from collections import deque
from pathlib import Path

from PIL import Image


def luminance(r: int, g: int, b: int) -> float:
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def is_grayish(r: int, g: int, b: int, min_lum: float = 200, max_chroma: int = 28) -> bool:
    return luminance(r, g, b) >= min_lum and max(r, g, b) - min(r, g, b) <= max_chroma


def flood_remove_bg(img: Image.Image, tolerance: int = 28) -> Image.Image:
    rgba = img.convert("RGBA")
    width, height = rgba.size
    pixels = rgba.load()
    visited = [[False] * width for _ in range(height)]

    def color_close(c1: tuple[int, ...], c2: tuple[int, ...]) -> bool:
        return all(abs(c1[i] - c2[i]) <= tolerance for i in range(3))

    bg_samples = [
        pixels[0, 0][:3],
        pixels[width - 1, 0][:3],
        pixels[0, height - 1][:3],
        pixels[width - 1, height - 1][:3],
    ]
    bg = tuple(sum(sample[i] for sample in bg_samples) // 4 for i in range(3))

    queue: deque[tuple[int, int]] = deque()
    for x in range(width):
        for y in (0, height - 1):
            if not visited[y][x] and color_close(pixels[x, y][:3], bg):
                visited[y][x] = True
                queue.append((x, y))
    for y in range(height):
        for x in (0, width - 1):
            if not visited[y][x] and color_close(pixels[x, y][:3], bg):
                visited[y][x] = True
                queue.append((x, y))

    while queue:
        x, y = queue.popleft()
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < width and 0 <= ny < height and not visited[ny][nx]:
                if color_close(pixels[nx, ny][:3], bg):
                    visited[ny][nx] = True
                    queue.append((nx, ny))

    for y in range(height):
        for x in range(width):
            if visited[y][x]:
                pixels[x, y] = (pixels[x, y][0], pixels[x, y][1], pixels[x, y][2], 0)

    return rgba


def defringe_edges(
    img: Image.Image,
    passes: int = 8,
    min_lum: float = 90,
    max_chroma: int = 24,
) -> Image.Image:
    rgba = img.convert("RGBA")
    width, height = rgba.size

    for _ in range(passes):
        pixels = rgba.load()
        to_clear: list[tuple[int, int]] = []

        for y in range(height):
            for x in range(width):
                r, g, b, a = pixels[x, y]
                if a == 0:
                    continue

                touches_transparent = any(
                    0 <= nx < width
                    and 0 <= ny < height
                    and pixels[nx, ny][3] == 0
                    for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))
                )
                if touches_transparent and is_grayish(r, g, b, min_lum=min_lum, max_chroma=max_chroma):
                    to_clear.append((x, y))

        if not to_clear:
            break

        for x, y in to_clear:
            r, g, b, _ = pixels[x, y]
            pixels[x, y] = (r, g, b, 0)

    return rgba


def remove_light_opaque(img: Image.Image, min_lum: float = 210, max_chroma: int = 26) -> Image.Image:
    rgba = img.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size

    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 0 and is_grayish(r, g, b, min_lum=min_lum, max_chroma=max_chroma):
                pixels[x, y] = (r, g, b, 0)

    return rgba


def trim_transparent(img: Image.Image, padding: int = 4) -> Image.Image:
    rgba = img.convert("RGBA")
    alpha = rgba.getchannel("A")
    bbox = alpha.getbbox()
    if not bbox:
        return rgba

    left, top, right, bottom = bbox
    left = max(0, left - padding)
    top = max(0, top - padding)
    right = min(rgba.width, right + padding)
    bottom = min(rgba.height, bottom + padding)
    return rgba.crop((left, top, right, bottom))


def process_wordmark(path: Path) -> None:
    img = Image.open(path)
    img = flood_remove_bg(img, tolerance=28)
    img = remove_light_opaque(img, min_lum=205, max_chroma=28)
    img = defringe_edges(img, passes=6, min_lum=175, max_chroma=34)
    img = trim_transparent(img, padding=6)
    img.save(path, "PNG")
    print(f"wordmark saved {path} -> {img.size}")


def process_mascot(path: Path) -> None:
    img = Image.open(path)
    img = flood_remove_bg(img, tolerance=30)
    img = defringe_edges(img, passes=12, min_lum=88, max_chroma=24)
    img = trim_transparent(img, padding=8)
    img.save(path, "PNG")
    print(f"mascot saved {path} -> {img.size}")


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[2]
    source_mascot = root / "docx/images/logo1.png"
    mascot_target = root / "box-web/src/assets/logo-mascot.png"

    for relative in ("box-web/src/assets/logo.png", "docx/images/logo.png"):
        target = root / relative
        if target.exists():
            process_wordmark(target)

    if source_mascot.exists():
        shutil.copy2(source_mascot, mascot_target)
        process_mascot(mascot_target)
        shutil.copy2(mascot_target, source_mascot)
