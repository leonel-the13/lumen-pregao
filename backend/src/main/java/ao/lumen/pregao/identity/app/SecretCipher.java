package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** AES-256-GCM para segredos TOTP guardados na base de dados (chave em MFA_ENC_KEY). */
@Component
public class SecretCipher {
    private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec key;

    public SecretCipher(AppProperties props, Environment env) {
        String b64 = props.security().mfaEncKey();
        byte[] raw;
        if (b64 == null || b64.isBlank()) {
            if (env.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("MFA_ENC_KEY é obrigatória em produção");
            }
            log.warn("MFA_ENC_KEY não definida: a usar chave efémera (só desenvolvimento)");
            raw = new byte[32];
            RANDOM.nextBytes(raw);
        } else {
            raw = Base64.getDecoder().decode(b64);
            if (raw.length != 32) throw new IllegalStateException("MFA_ENC_KEY tem de ter 32 bytes (base64)");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    public String decrypt(String enc) {
        try {
            byte[] all = Base64.getDecoder().decode(enc);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar", e);
        }
    }
}
