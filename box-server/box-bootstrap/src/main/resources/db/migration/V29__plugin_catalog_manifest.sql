ALTER TABLE plugin_catalog
    ADD COLUMN manifest_json JSON NULL COMMENT '安装到工作空间时使用的资源配置' AFTER description;
