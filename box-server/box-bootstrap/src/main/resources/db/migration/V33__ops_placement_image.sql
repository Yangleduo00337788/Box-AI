ALTER TABLE ops_placement
    ADD COLUMN image_url VARCHAR(512) DEFAULT NULL COMMENT 'Banner / 广告配图' AFTER icon_svg;
