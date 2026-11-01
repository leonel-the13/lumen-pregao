package ao.lumen.pregao.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Relógio do servidor controlável nos testes (o cliente nunca decide prazos). */
public class MutableClock extends Clock {
    private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

    public void advance(Duration d) { offset.updateAndGet(o -> o.plus(d)); }
    public void setTo(Instant at) { offset.set(Duration.between(Instant.now(), at)); }
    public void reset() { offset.set(Duration.ZERO); }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return Instant.now().plus(offset.get()); }
}
