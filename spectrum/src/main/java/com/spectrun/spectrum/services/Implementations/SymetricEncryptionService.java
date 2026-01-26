package com.spectrun.spectrum.services.Implementations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import java.util.Base64;

@Service
public class SymetricEncryptionService {
    @Value("${SPECTRUM_SSH_MASTER_KEY}")
    private  String key;
    private static final String AES = "AES";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SALT = "spectrum-ssh-v0";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256; // bits

    // cache derived key (derive ONCE)

    private static final String CIPHER_ALG = "AES/GCM/NoPadding";
    private static final int NONCE_LEN_BYTES = 12;   // 96-bit nonce (12 bytes)
    private static final int TAG_LEN_BITS = 128;     // 16-byte auth tag
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile SecretKey cachedAesKey;

    public SecretKey getOrDeriveAesKey(){
        // to prevent race conditions and ensure the key once generated can be resused
    synchronized (this){
        if(this.cachedAesKey != null ) return  cachedAesKey;
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("SPECTRUM_SSH_MASTER_KEY is missing/blank");
        }
        try{


        KeySpec spec = new PBEKeySpec(
                key.toCharArray(),
                SALT.getBytes(StandardCharsets.UTF_8),
                ITERATIONS,
                KEY_LENGTH_BITS
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        byte [] raw = factory.generateSecret(spec).getEncoded();
        cachedAesKey = new SecretKeySpec(raw,"AES");
        return  cachedAesKey;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    }
    public static byte[] createVector(){
        byte[] initVector = new byte[96];
        SecureRandom secureRandom
                = new SecureRandom();
        secureRandom.nextBytes(initVector);
        return initVector;
    }
    public String symetricEncryptService(String plainText){
        if (plainText == null) {
            throw new IllegalArgumentException("plainText is null");
        }

        try {
            SecretKey aesKey = getOrDeriveAesKey();

            byte[] nonce = new byte[NONCE_LEN_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(CIPHER_ALG);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LEN_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // final blob: nonce || ciphertext (ciphertext includes auth tag at end in Java GCM)
            byte[] out = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(ciphertext, 0, out, nonce.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(out);

        } catch (Exception e) {
            throw new IllegalStateException("Password encryption failed", e);
        }
    }
}

