package com.diasmart.springapi.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SecretKeySpec secretKeySpec;

    public EncryptionService(@Value("${app.encryption.key}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() != 32) {
            throw new IllegalArgumentException("Encryption key must be exactly 32 characters long.");
        }
        this.secretKeySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            EncryptedPayload encryptedPayload = encryptStructured(plaintext);
            byte[] iv = Base64.getDecoder().decode(encryptedPayload.getNonce());
            byte[] cipherText = Base64.getDecoder().decode(encryptedPayload.getCiphertext());
            byte[] authTag = Base64.getDecoder().decode(encryptedPayload.getAuthTag());

            byte[] encryptedData = new byte[iv.length + cipherText.length + authTag.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);
            System.arraycopy(authTag, 0, encryptedData, iv.length + cipherText.length, authTag.length);

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while encrypting data", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] cipherText = new byte[encryptedData.length - iv.length];
            System.arraycopy(encryptedData, iv.length, cipherText, 0, cipherText.length);

            byte[] decryptedText = cipher.doFinal(cipherText);

            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while decrypting data", e);
        }
    }

    public EncryptedPayload encryptStructured(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] cipherTextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            int tagLengthByte = TAG_LENGTH_BIT / 8;
            int cipherTextLength = cipherTextWithTag.length - tagLengthByte;

            byte[] cipherText = Arrays.copyOfRange(cipherTextWithTag, 0, cipherTextLength);
            byte[] authTag = Arrays.copyOfRange(cipherTextWithTag, cipherTextLength, cipherTextWithTag.length);

            return new EncryptedPayload(
                    Base64.getEncoder().encodeToString(cipherText),
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(authTag)
            );
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while encrypting data", e);
        }
    }

    public String decryptStructured(String ciphertext, String nonce, String authTag) {
        try {
            byte[] iv = Base64.getDecoder().decode(nonce);
            byte[] cipherText = Base64.getDecoder().decode(ciphertext);
            byte[] tag = Base64.getDecoder().decode(authTag);
            byte[] cipherTextWithTag = new byte[cipherText.length + tag.length];

            System.arraycopy(cipherText, 0, cipherTextWithTag, 0, cipherText.length);
            System.arraycopy(tag, 0, cipherTextWithTag, cipherText.length, tag.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] decryptedText = cipher.doFinal(cipherTextWithTag);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while decrypting data", e);
        }
    }

    public static class EncryptedPayload {

        private final String ciphertext;
        private final String nonce;
        private final String authTag;

        public EncryptedPayload(String ciphertext, String nonce, String authTag) {
            this.ciphertext = ciphertext;
            this.nonce = nonce;
            this.authTag = authTag;
        }

        public String getCiphertext() {
            return ciphertext;
        }

        public String getNonce() {
            return nonce;
        }

        public String getAuthTag() {
            return authTag;
        }
    }
}
