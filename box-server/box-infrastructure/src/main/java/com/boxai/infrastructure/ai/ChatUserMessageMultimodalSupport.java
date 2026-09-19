package com.boxai.infrastructure.ai;

import com.boxai.infrastructure.storage.PublicAssetImageLoader;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts chat-stored {@code [图片: name]\n/api/v1/public-assets/...} blocks into multimodal user messages.
 */
public final class ChatUserMessageMultimodalSupport {

    private static final Pattern IMAGE_BLOCK = Pattern.compile(
            "\\[图片:\\s*([^\\]]+)\\]\\s*\\n+((?:https?://[^\\s]+)?/api/v1/public-assets/[^\\s]+)",
            Pattern.MULTILINE);
    private static final Pattern UNTRUSTED_USER = Pattern.compile(
            "^UNTRUSTED_USER_START\\s*\\n([\\s\\S]*?)\\nUNTRUSTED_USER_END\\s*$",
            Pattern.MULTILINE);

    private ChatUserMessageMultimodalSupport() {
    }

    public record UserContentPart(String type, String text, String dataUrl) {
        public static UserContentPart text(String value) {
            return new UserContentPart("text", value, null);
        }

        public static UserContentPart image(String dataUrl) {
            return new UserContentPart("image", null, dataUrl);
        }
    }

    public static boolean containsImageBlock(String content) {
        if (content == null || !content.contains("[图片:")) {
            return false;
        }
        return IMAGE_BLOCK.matcher(unwrapUntrustedUser(content)).find();
    }

    public static String unwrapUntrustedUser(String content) {
        if (content == null || content.isBlank()) {
            return content == null ? "" : content;
        }
        Matcher matcher = UNTRUSTED_USER.matcher(content.trim());
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return content;
    }

    public static List<UserContentPart> buildContentParts(String content, PublicAssetImageLoader loader) {
        String normalized = unwrapUntrustedUser(content);
        if (normalized == null || normalized.isBlank()) {
            return List.of(UserContentPart.text(""));
        }
        if (!normalized.contains("[图片:") || loader == null) {
            return List.of(UserContentPart.text(normalized));
        }
        Matcher matcher = IMAGE_BLOCK.matcher(normalized);
        if (!matcher.find()) {
            return List.of(UserContentPart.text(normalized));
        }
        matcher.reset();
        List<UserContentPart> parts = new ArrayList<>();
        int last = 0;
        while (matcher.find()) {
            appendTextPart(parts, normalized.substring(last, matcher.start()));
            String name = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            var loaded = loader.loadFromReference(url);
            if (loaded.isPresent()) {
                PublicAssetImageLoader.LoadedPublicImage image = loaded.get();
                parts.add(UserContentPart.image(toDataUrl(image.bytes(), image.contentType())));
                appendTextPart(parts, "[图片: " + name + "]");
            } else {
                appendTextPart(parts, matcher.group(0));
            }
            last = matcher.end();
        }
        appendTextPart(parts, normalized.substring(last));
        if (parts.isEmpty()) {
            return List.of(UserContentPart.text(normalized));
        }
        return parts;
    }

    public static UserMessage buildUserMessage(String content, PublicAssetImageLoader loader) {
        List<UserContentPart> parts = buildContentParts(content, loader);
        if (parts.size() == 1 && "text".equals(parts.get(0).type())) {
            return UserMessage.from(parts.get(0).text());
        }
        List<Content> messageParts = new ArrayList<>();
        for (UserContentPart part : parts) {
            if ("text".equals(part.type())) {
                appendLangChainText(messageParts, part.text());
            } else if ("image".equals(part.type())) {
                messageParts.add(ImageContent.from(part.dataUrl(), ImageContent.DetailLevel.AUTO));
            }
        }
        if (messageParts.isEmpty()) {
            return UserMessage.from(content);
        }
        return UserMessage.from(messageParts);
    }

    private static void appendTextPart(List<UserContentPart> parts, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        parts.add(UserContentPart.text(text));
    }

    private static void appendLangChainText(List<Content> parts, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        parts.add(TextContent.from(text));
    }

    static String toDataUrl(byte[] bytes, String mimeType) {
        String safeMime = mimeType == null || mimeType.isBlank() ? "image/png" : mimeType;
        return "data:" + safeMime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
