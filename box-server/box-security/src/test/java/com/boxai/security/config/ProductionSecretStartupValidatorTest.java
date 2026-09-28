package com.boxai.security.config;

import com.boxai.infrastructure.crypto.CryptoProperties;
import com.boxai.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSecretStartupValidatorTest {

    private JwtProperties jwtProperties;
    private CryptoProperties cryptoProperties;
    private ProductionSecretStartupValidator validator;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        cryptoProperties = new CryptoProperties();
        validator = new ProductionSecretStartupValidator(jwtProperties, cryptoProperties);
    }

    @Test
    void skipsValidationForDevProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        jwtProperties.setSecret(ProductionSecretStartupValidator.DEFAULT_JWT_SECRET);
        cryptoProperties.setAesKey(ProductionSecretStartupValidator.DEFAULT_AES_KEY);
        assertDoesNotThrow(() -> validator.validate(environment));
    }

    @Test
    void rejectsDefaultJwtSecretInProdProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        jwtProperties.setSecret(ProductionSecretStartupValidator.DEFAULT_JWT_SECRET);
        cryptoProperties.setAesKey("custom-aes-key-32-chars-minimum!");
        assertThrows(IllegalStateException.class, () -> validator.validate(environment));
    }

    @Test
    void rejectsDefaultAesKeyInProdProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        jwtProperties.setSecret("custom-jwt-secret-with-32-chars-min");
        cryptoProperties.setAesKey(ProductionSecretStartupValidator.DEFAULT_AES_KEY);
        assertThrows(IllegalStateException.class, () -> validator.validate(environment));
    }

    @Test
    void acceptsCustomSecretsInProdProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        jwtProperties.setSecret("custom-jwt-secret-with-32-chars-min");
        cryptoProperties.setAesKey("custom-aes-key-32-chars-minimum!");
        assertDoesNotThrow(() -> validator.validate(environment));
    }
}
