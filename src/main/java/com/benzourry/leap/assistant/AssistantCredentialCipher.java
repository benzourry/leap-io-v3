package com.benzourry.leap.assistant;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AssistantCredentialCipher {

    private static final String PREFIX = "v1:";
    private static final int NONCE_LENGTH = 12;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AssistantCredentialCipher(@Value("${app.auth.tokenSecret}") String tokenSecret) {
        try {
            byte[] secret = Decoders.BASE64.decode(tokenSecret);
            this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret), "AES");
        } catch (RuntimeException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Assistant credential encryption could not be initialized.", e);
        }
    }

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Assistant API key could not be encrypted.", e);
        }
    }

    public String decrypt(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            throw new IllegalStateException("Stored assistant API key is not encrypted with a supported format.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            if (payload.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted value");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Stored assistant API key could not be decrypted.", e);
        }
    }
}
