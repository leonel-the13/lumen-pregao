package ao.lumen.pregao.common.config;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfig {

    /** Relógio do servidor (UTC): única referência temporal (CON-01). Substituído nos testes. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Executor de threads virtuais para a correcção assíncrona (CON-08). */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService gradingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
