package com.empresa.rcp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SecurityUtil {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);
    private static final String DPAPI_PREFIX = "{DPAPI}";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        if (plainText.startsWith(DPAPI_PREFIX)) {
            return plainText; // Already encrypted
        }
        if (!IS_WINDOWS) {
            logger.warn("Not on Windows. Skipping DPAPI encryption.");
            return plainText;
        }
        try {
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(plainBytes);
            String encoded = Base64.getEncoder().encodeToString(cipherBytes);
            return DPAPI_PREFIX + encoded;
        } catch (Throwable e) {
            logger.error("Failed to encrypt using DPAPI: ", e);
            return plainText;
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        if (!cipherText.startsWith(DPAPI_PREFIX)) {
            return cipherText; // Plain text
        }
        if (!IS_WINDOWS) {
            logger.error("Not on Windows. Cannot decrypt DPAPI text.");
            return cipherText;
        }
        try {
            String base64Data = cipherText.substring(DPAPI_PREFIX.length());
            byte[] cipherBytes = Base64.getDecoder().decode(base64Data);
            byte[] plainBytes = com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            logger.error("Failed to decrypt using DPAPI: ", e);
            return cipherText;
        }
    }
}
