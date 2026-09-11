package com.boxai.user.api;

import java.util.List;

public record SystemContentVO(
        String supportEmail,
        AboutContentVO about,
        LegalContentVO legal
) {
    public record AboutContentVO(
            String slogan,
            String productName,
            String positioning,
            String version
    ) {}

    public record LegalContentVO(
            List<String> privacy,
            List<String> terms,
            String updatedAt
    ) {}
}
