package com.boxai.knowledge.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PdfPageRasterizer {

    private static final Logger log = LoggerFactory.getLogger(PdfPageRasterizer.class);

    private PdfPageRasterizer() {
    }

    public static List<byte[]> renderPagesAsPng(byte[] pdfBytes, int maxPages, float dpi) {
        List<byte[]> pages = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) {
            return pages;
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int total = document.getNumberOfPages();
            int limit = Math.min(total, Math.max(maxPages, 1));
            for (int page = 0; page < limit; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                pages.add(toPng(image));
            }
        } catch (IOException e) {
            log.warn("PDF rasterize failed: {}", e.getMessage());
        }
        return pages;
    }

    private static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
