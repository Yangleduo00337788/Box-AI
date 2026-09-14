ALTER TABLE ops_placement
    ADD COLUMN icon_name VARCHAR(64) DEFAULT NULL COMMENT 'TDesign 图标名' AFTER link_label,
    ADD COLUMN icon_url VARCHAR(512) DEFAULT NULL COMMENT '自定义图标地址' AFTER icon_name;
