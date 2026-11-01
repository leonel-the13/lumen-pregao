package ao.lumen.pregao.common.domain;

import java.security.SecureRandom;
import java.util.UUID;

/** UUID versão 7 (RFC 9562): 48 bits de tempo em ms seguidos de aleatório. */
public final class UuidV7 {
    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {}

    public static UUID next() {
        long ts = System.currentTimeMillis();
        long randA = RANDOM.nextInt(1 << 12);
        long msb = (ts << 16) | (0x7L << 12) | randA;
        long lsb = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
