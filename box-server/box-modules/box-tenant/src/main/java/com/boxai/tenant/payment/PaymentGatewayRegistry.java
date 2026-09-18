package com.boxai.tenant.payment;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentGatewayRegistry {

    private final List<PaymentGateway> gateways;
    private final MockPaymentGateway mockPaymentGateway;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways, MockPaymentGateway mockPaymentGateway) {
        this.gateways = gateways;
        this.mockPaymentGateway = mockPaymentGateway;
    }

    public PaymentGateway resolve(PaymentProperties properties) {
        return gateways.stream()
                .filter(gateway -> gateway != mockPaymentGateway && gateway.supports(properties))
                .findFirst()
                .or(() -> mockPaymentGateway.supports(properties) ? Optional.of(mockPaymentGateway) : Optional.empty())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "支付网关未配置或不可用"));
    }
}
