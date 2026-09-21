# -*- coding: utf-8 -*-
"""Ensure README SVGs declare width/height matching viewBox for correct GitHub aspect ratio."""
import re
from pathlib import Path

readme = Path(__file__).resolve().parents[1] / "assets" / "readme"

for path in readme.glob("*.svg"):
    text = path.read_text(encoding="utf-8")
    m = re.search(r'viewBox="0 0 (\d+) (\d+)"', text)
    if not m:
        print("skip", path.name, "no viewBox")
        continue
    w, h = m.group(1), m.group(2)
    if re.search(r'<svg[^>]*\bwidth="' + w + r'"[^>]*\bheight="' + h + r'"', text):
        print("ok", path.name)
        continue
    text = re.sub(
        r'<svg([^>]*)\sviewBox="0 0 ' + w + r' ' + h + r'"',
        f'<svg$1 width="{w}" height="{h}" viewBox="0 0 {w} {h}"',
        text,
        count=1,
    )
    path.write_text(text, encoding="utf-8")
    print("fixed", path.name, w, h)
