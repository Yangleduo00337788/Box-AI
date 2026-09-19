package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSafetyPolicyTest {

    @Test
    void sanitizesPathTraversal() {
        assertEquals("report.pdf", FileSafetyPolicy.sanitizeFileName("../../report.pdf"));
    }

    @Test
    void acceptsPlainTextAndLog() {
        byte[] bytes = "hello knowledge".getBytes();
        assertDoesNotThrow(() -> FileSafetyPolicy.validate("note.txt", "text/plain", bytes.length, bytes));
        assertDoesNotThrow(() -> FileSafetyPolicy.validate("run.log", "text/plain", bytes.length, bytes));
    }

    @Test
    void rejectsExecutableAndHtml() {
        assertThrows(BusinessException.class,
                () -> FileSafetyPolicy.validate("malware.exe", "application/octet-stream", 2, new byte[]{'M', 'Z'}));
        assertThrows(BusinessException.class,
                () -> FileSafetyPolicy.validate("page.html", "text/html", 20, "<html>hi</html>".getBytes()));
        assertThrows(BusinessException.class,
                () -> FileSafetyPolicy.validate("note.txt", "text/plain", 20, "<script>alert(1)</script>".getBytes()));
    }

    @Test
    void acceptsPngAndJpegImages() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        assertEquals("png", FileSafetyPolicy.validateImage("avatar.png", "image/png", png.length, png));
        assertEquals("jpg", FileSafetyPolicy.validateImage("avatar.jpg", "image/jpeg", jpeg.length, jpeg));
    }

    @Test
    void rejectsNonImageAsAvatar() {
        assertThrows(BusinessException.class,
                () -> FileSafetyPolicy.validateImage("note.txt", "text/plain", 5, "hello".getBytes()));
        assertThrows(BusinessException.class,
                () -> FileSafetyPolicy.validateImage("avatar.png", "image/png", 4, new byte[]{1, 2, 3, 4}));
    }
}
