package com.devpulsex.integration.trello;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Small utility to encrypt/decrypt Trello user tokens at rest using AES-GCM.
 */
@Component
public class TrelloTokenEncryptor {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public TrelloTokenEncryptor(@Value("${trello.encryption.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("trello.encryption.secret is required and must be set via TRELLO_ENC_SECRET");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("trello.encryption.secret must be at least 32 bytes; provided length=" + raw.length);
        }
        // Derive a 256-bit key from the configured secret (padded/trimmed UTF-8 bytes)
        this.keyBytes = Arrays.copyOf(raw, 32);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(cipherBytes, 0, output, iv.length, cipherBytes.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt Trello token", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            if (decoded.length <= IV_LENGTH) throw new IllegalArgumentException("Invalid ciphertext");

            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt Trello token", e);
        }
    }

    private SecretKey key() {
        return new SecretKeySpec(keyBytes, "AES");
    }
}