package com.boxai.agent.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginCatalogAssetApplicationServiceTest {

    @Mock
    private ObjectStorage objectStorage;

    @InjectMocks
    private PluginCatalogAssetApplicationService service;

    @Test
    void extractSkillMarkdownFromZip_prefersSkillMd() throws Exception {
        String text = PluginCatalogAssetApplicationService.extractSkillMarkdownFromZip(zipWithSkillMd());
        assertTrue(text.contains("技能已启用"));
    }

    @Test
    void extractSkillMarkdownFromZipRejectsArchiveWithoutMarkdown() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("no md".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PluginCatalogAssetApplicationService.extractSkillMarkdownFromZip(baos.toByteArray()));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void uploadRejectsEmptyFile() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload(new MockMultipartFile("file", new byte[0])));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void uploadPutsObjectUnderPluginCatalogPrefix() {
        when(objectStorage.defaultBucket()).thenReturn("box");
        byte[] bytes = "hello skill".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "skill.md", "text/markdown", bytes);

        var vo = service.upload(file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).put(eq("box"), keyCaptor.capture(), any(), anyLong(), eq("text/markdown"));
        assertTrue(keyCaptor.getValue().startsWith("plugin-catalog/"));
        assertTrue(keyCaptor.getValue().endsWith("skill.md"));
        assertEquals("skill.md", vo.fileName());
        assertEquals(bytes.length, vo.size());
    }

    @Test
    void readAssetRejectsKeyOutsidePrefix() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.readAsset("../secret"));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void resolveSkillInstructionsReadsMarkdownAsset() {
        when(objectStorage.defaultBucket()).thenReturn("box");
        when(objectStorage.get("box", "plugin-catalog/a/SKILL.md"))
                .thenReturn(new ByteArrayInputStream("  remember this  ".getBytes(StandardCharsets.UTF_8)));

        assertEquals("remember this", service.resolveSkillInstructions("plugin-catalog/a/SKILL.md", null));
    }

    private static byte[] zipWithSkillMd() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("other.md"));
            zos.write("other".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("SKILL.md"));
            zos.write("【技能已启用】".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
