package com.devpulsex.integration.oauth;

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
 * Encrypts/decrypts OAuth tokens stored in the users table.
 *
 * Uses AES-GCM for authenticated encryption. Existing plaintext tokens are
 * tolerated via decryptLenient() to allow zero-downtime migration.
 */
@Component
public class OAuthTokenEncryptor {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthTokenEncryptor(@Value("${oauth.encryption.secret:${trello.encryption.secret:}}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("oauth.encryption.secret (or trello.encryption.secret) is required");
        }

        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("OAuth encryption secret must be at least 32 bytes; provided length=" + raw.length);
        }

        this.keyBytes = Arrays.copyOf(raw, 32);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return null;

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
            throw new IllegalStateException("Failed to encrypt OAuth token", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) return null;

        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            if (decoded.length <= IV_LENGTH) throw new IllegalArgumentException("Invalid ciphertext");

            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt OAuth token", e);
        }
    }

    public String decryptLenient(String value) {
        if (value == null || value.isBlank()) return null;

        try {
            return decrypt(value);
        } catch (Exception ex) {
            // Backward compatibility for already-stored plaintext tokens.
            return value;
        }
    }

    private SecretKey key() {
        return new SecretKeySpec(keyBytes, "AES");
    }
}
