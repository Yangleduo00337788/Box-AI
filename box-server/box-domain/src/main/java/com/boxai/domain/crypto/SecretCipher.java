package com.boxai.domain.crypto;

public interface SecretCipher {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
