package com.notivio.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for storing sensitive data (Gmail tokens).
 * Uses authenticated encryption to detect tampering.
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    @Value("${encryption.key}")
    private String encryptionKey;

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: Base64(IV + ciphertext + auth-tag)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), paramSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Prepend IV to ciphertext
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts AES-256-GCM encrypted text.
     */
    public String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            // Extract ciphertext
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), paramSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private SecretKeySpec buildKey() {
        // Pad or truncate key to exactly 32 bytes (256 bits)
        byte[] keyBytes = new byte[32];
        byte[] inputBytes = encryptionKey.getBytes();
        System.arraycopy(inputBytes, 0, keyBytes, 0, Math.min(inputBytes.length, 32));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
