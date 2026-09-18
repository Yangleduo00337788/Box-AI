package com.boxai.tenant.config;

import com.boxai.tenant.payment.PaymentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class TenantPaymentConfiguration {
}
