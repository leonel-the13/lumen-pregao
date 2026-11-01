package ao.lumen.pregao.exam.app;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.exam.infra.ExamSessionRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** No fim do tempo, a submissão é automática (spec §4.6). Idempotente. */
@Component
public class ExamScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExamScheduler.class);
    private final ExamSessionRepository sessions;
    private final ExamService exams;
    private final AppProperties props;
    private final Clock clock;

    public ExamScheduler(ExamSessionRepository sessions, ExamService exams, AppProperties props, Clock clock) {
        this.sessions = sessions;
        this.exams = exams;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void expire() {
        var before = clock.instant().minus(props.exam().networkGrace());
        var ids = new java.util.ArrayList<>(sessions.findExpired(before));
        ids.addAll(sessions.findUngraded());
        for (var id : ids) {
            try {
                exams.autoSubmit(id);
            } catch (OptimisticLockingFailureException e) {
                // o estudante submeteu ao mesmo tempo; a próxima volta confirma o estado
            } catch (RuntimeException e) {
                log.error("Falha na submissão automática do exame {}", id, e);
            }
        }
    }
}
