package com.boxai.agent.application;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginCatalogAssetApplicationServiceTest {

    @Test
    void extractSkillMarkdownFromZip_prefersSkillMd() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("other.md"));
            zos.write("other".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("SKILL.md"));
            zos.write("【技能已启用】".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        String text = PluginCatalogAssetApplicationService.extractSkillMarkdownFromZip(baos.toByteArray());
        assertTrue(text.contains("技能已启用"));
    }
}
