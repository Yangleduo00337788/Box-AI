package com.boxai.tenant.payment;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentGatewayRegistry {

    private final List<PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public PaymentGateway resolve(PaymentProperties properties) {
        return gateways.stream()
                .filter(gateway -> gateway.supports(properties))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "支付网关未配置或不可用"));
    }

    public Optional<PaymentGateway> findByChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return Optional.empty();
        }
        return gateways.stream()
                .filter(gateway -> channel.equalsIgnoreCase(gateway.channel()))
                .findFirst();
    }
}
