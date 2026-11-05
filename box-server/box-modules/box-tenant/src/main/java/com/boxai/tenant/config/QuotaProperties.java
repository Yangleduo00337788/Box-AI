package com.boxai.tenant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "box.quota")
public class QuotaProperties {

    /**
     * When false, plan quota checks do not reject (load tests). Keep true in production.
     */
    private boolean enabled = true;
}
