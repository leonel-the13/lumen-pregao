package ao.lumen.pregao.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

class RateLimiterFallbackTest {

    @Test
    void limitsStillApplyWhenRedisIsDown() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("em baixo"));
        when(redis.getExpire("rl:login:x")).thenThrow(new RedisConnectionFailureException("em baixo"));
        RateLimiter limiter = new RateLimiter(redis);

        for (int i = 0; i < 5; i++) assertThat(limiter.tryAcquire("login:x", 5, Duration.ofMinutes(15))).isTrue();
        assertThat(limiter.tryAcquire("login:x", 5, Duration.ofMinutes(15))).isFalse();
        assertThat(limiter.count("login:x")).isEqualTo(6);
        assertThat(limiter.ttlSeconds("login:x")).isBetween(1L, 900L);
        limiter.reset("login:x");
        assertThat(limiter.count("login:x")).isZero();
    }
}
