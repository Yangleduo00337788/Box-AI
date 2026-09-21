# -*- coding: utf-8 -*-
"""Rasterize assets/readme/*.svg to PNG for Gitee/GitHub (avoids SVG img squashing)."""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
readme = ROOT / "assets" / "readme"
WIDTH = 1200

svgs = [
    "hero.svg",
    "architecture.svg",
    "flow-rag.svg",
    "flow-workflow.svg",
    "flow-lifecycle.svg",
]

for name in svgs:
    svg = readme / name
    png = readme / name.replace(".svg", ".png")
    cmd = [
        "npx",
        "--yes",
        "@resvg/resvg-js-cli",
        "--fit-width",
        str(WIDTH),
        str(svg),
        str(png),
    ]
    print(" ".join(cmd))
    subprocess.run(cmd, check=True, cwd=str(ROOT))

print("Done. Commit assets/readme/*.png alongside SVG sources.")
