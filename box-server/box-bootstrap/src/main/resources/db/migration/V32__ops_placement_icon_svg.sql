ALTER TABLE ops_placement
    ADD COLUMN icon_svg TEXT DEFAULT NULL COMMENT '内联 SVG 图标' AFTER icon_url;
