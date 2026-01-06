package com.cbrviewer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for AES-256-GCM encryption and decryption.
 * Uses authenticated encryption with a 128-bit authentication tag.
 *
 * Format: Base64(IV || EncryptedData || AuthTag)
 * - IV: 12 bytes (random, generated per encryption)
 * - Auth Tag: 16 bytes (128 bits, embedded in ciphertext by GCM mode)
 */
public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom;

    /**
     * Constructs an EncryptionUtil with the provided master key.
     *
     * @param masterKeyHex 64-character hex string (32 bytes for AES-256)
     * @throws IllegalArgumentException if key is invalid
     */
    public EncryptionUtil(String masterKeyHex) {
        if (masterKeyHex == null || masterKeyHex.isEmpty()) {
            throw new IllegalArgumentException("Encryption master key cannot be null or empty");
        }

        // Remove any whitespace
        masterKeyHex = masterKeyHex.trim();

        // Validate hex string length (64 hex chars = 32 bytes for AES-256)
        if (masterKeyHex.length() != 64) {
            throw new IllegalArgumentException(
                String.format("Invalid master key length: expected 64 hex characters (32 bytes), got %d. " +
                             "Generate with: openssl rand -hex 32", masterKeyHex.length())
            );
        }

        try {
            // Convert hex string to bytes
            byte[] keyBytes = hexToBytes(masterKeyHex);
            this.keySpec = new SecretKeySpec(keyBytes, "AES");
            this.secureRandom = new SecureRandom();

            logger.info("EncryptionUtil initialized with AES-256-GCM");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to initialize encryption key: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     *
     * @param plaintext the text to encrypt
     * @return Base64-encoded string containing IV + ciphertext + auth tag
     * @throws EncryptionException if encryption fails
     */
    public String encrypt(String plaintext) throws EncryptionException {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }

        try {
            // Generate random IV (nonce)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + ciphertext (ciphertext includes auth tag from GCM)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Return Base64-encoded result
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts Base64-encoded ciphertext using AES-256-GCM.
     *
     * @param encryptedBase64 Base64 string containing IV + ciphertext + auth tag
     * @return decrypted plaintext
     * @throws EncryptionException if decryption fails or data is corrupted
     */
    public String decrypt(String encryptedBase64) throws EncryptionException {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }

        try {
            // Decode Base64
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            // Validate minimum length (IV + at least some ciphertext)
            if (combined.length < GCM_IV_LENGTH + GCM_TAG_LENGTH / 8) {
                throw new EncryptionException("Encrypted data is too short to be valid");
            }

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt and verify authentication tag
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (javax.crypto.AEADBadTagException e) {
            logger.error("Decryption failed: Authentication tag mismatch (wrong key or corrupted data)");
            throw new EncryptionException("Decryption failed: Data may be corrupted or encrypted with a different key", e);
        } catch (IllegalArgumentException e) {
            logger.error("Decryption failed: Invalid Base64 encoding");
            throw new EncryptionException("Decryption failed: Invalid Base64 encoding", e);
        } catch (Exception e) {
            logger.error("Decryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    /**
     * Converts hex string to byte array.
     */
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Custom exception for encryption/decryption errors.
     */
    public static class EncryptionException extends Exception {
        public EncryptionException(String message) {
            super(message);
        }

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
