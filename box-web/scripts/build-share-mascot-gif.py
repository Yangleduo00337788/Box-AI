"""从双姿态合成图生成分享页吉祥物 GIF（透明底，动作直接切换，无缩放弹跳）。"""

from __future__ import annotations



from pathlib import Path



from PIL import Image



ROOT = Path(__file__).resolve().parents[1]

SRC = ROOT / "public" / "share" / "box-mascot-source.jpg"

OUT = ROOT / "public" / "share" / "box-mascot.gif"

TARGET_H = 280

HOLD_FRAMES = 36

FRAME_MS = 70

TRANSPARENCY_INDEX = 255





def fit_height(img: Image.Image, height: int) -> Image.Image:

    ratio = height / img.height

    width = int(img.width * ratio)

    return img.resize((width, height), Image.Resampling.LANCZOS)





def remove_white_background(img: Image.Image, threshold: int = 248) -> Image.Image:

    """抠掉 JPG 白底，保留角色与装饰。"""

    rgba = img.convert("RGBA")

    pixels = rgba.load()

    w, h = rgba.size

    for y in range(h):

        for x in range(w):

            r, g, b, a = pixels[x, y]

            if r >= threshold and g >= threshold and b >= threshold:

                pixels[x, y] = (r, g, b, 0)

    return rgba





def pad_center(img: Image.Image, width: int, height: int) -> Image.Image:

    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))

    ox = (width - img.width) // 2

    oy = (height - img.height) // 2

    canvas.paste(img, (ox, oy), img)

    return canvas





def render_pose(pose: Image.Image, canvas_w: int, canvas_h: int) -> Image.Image:

    canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

    ox = (canvas_w - pose.width) // 2

    oy = canvas_h - pose.height

    canvas.paste(pose, (ox, oy), pose)

    return canvas





def rgba_to_palette_transparent(rgba: Image.Image) -> Image.Image:

    alpha = rgba.getchannel("A")

    rgb = rgba.convert("RGB")

    palette_img = rgb.quantize(colors=255, method=Image.Quantize.MEDIANCUT)

    palette_img = palette_img.convert("RGBA")

    palette_img.putalpha(alpha)

    p = palette_img.convert("RGB").convert("P", palette=Image.Palette.ADAPTIVE, colors=255)

    mask = Image.eval(alpha, lambda a: 255 if a < 128 else 0)

    p.paste(TRANSPARENCY_INDEX, mask)

    return p





def main() -> None:

    if not SRC.is_file():

        raise SystemExit(f"缺少源图: {SRC}")



    sheet = remove_white_background(Image.open(SRC).convert("RGBA"))

    w, h = sheet.size

    mid = w // 2

    pose_a = remove_white_background(fit_height(sheet.crop((0, 0, mid, h)), TARGET_H))

    pose_b = remove_white_background(fit_height(sheet.crop((mid, 0, w, h)), TARGET_H))



    tw = max(pose_a.width, pose_b.width)

    th = max(pose_a.height, pose_b.height)

    canvas_w = int(tw * 1.25)

    canvas_h = int(th * 1.2)

    pose_a = pad_center(pose_a, tw, th)

    pose_b = pad_center(pose_b, tw, th)



    base_a = render_pose(pose_a, canvas_w, canvas_h)

    base_b = render_pose(pose_b, canvas_w, canvas_h)



    frames: list[Image.Image] = []

    for _ in range(HOLD_FRAMES):

        frames.append(base_a.copy())

    for _ in range(HOLD_FRAMES):

        frames.append(base_b.copy())



    palette_frames = [rgba_to_palette_transparent(f) for f in frames]

    palette_frames[0].save(

        OUT,

        save_all=True,

        append_images=palette_frames[1:],

        duration=FRAME_MS,

        loop=0,

        disposal=2,

        transparency=TRANSPARENCY_INDEX,

        optimize=True,

    )

    print(f"Wrote {OUT} ({len(palette_frames)} frames, transparent, hard cut)")





if __name__ == "__main__":

    main()

