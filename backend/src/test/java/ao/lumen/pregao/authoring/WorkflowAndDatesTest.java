package ao.lumen.pregao.authoring;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.lumen.pregao.authoring.domain.EditorialStatus;
import ao.lumen.pregao.authoring.domain.ReviewWorkflow;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.contest.domain.Contest;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkflowAndDatesTest {
    static ReviewWorkflow wf() throws Exception {
        Constructor<ReviewWorkflow> c = ReviewWorkflow.class.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    @Test
    void editorialFlowCannotSkipSteps() throws Exception {
        ReviewWorkflow w = wf();
        assertThatThrownBy(() -> w.moveTo(EditorialStatus.PUBLISHED, UUID.randomUUID(), null, Instant.now()))
                .isInstanceOf(DomainException.class);
        w.moveTo(EditorialStatus.IN_REVIEW, UUID.randomUUID(), null, Instant.now());
        w.moveTo(EditorialStatus.APPROVED, UUID.randomUUID(), null, Instant.now());
        assertThatCode(() -> w.moveTo(EditorialStatus.PUBLISHED, UUID.randomUUID(), null, Instant.now())).doesNotThrowAnyException();
    }

    @Test
    void legalAlertUnpublishes() throws Exception {
        ReviewWorkflow w = wf();
        w.moveTo(EditorialStatus.IN_REVIEW, null, null, Instant.now());
        w.moveTo(EditorialStatus.APPROVED, null, null, Instant.now());
        w.moveTo(EditorialStatus.PUBLISHED, null, null, Instant.now());
        w.legalAlert(Instant.now(), true);
        org.assertj.core.api.Assertions.assertThat(w.getStatus()).isEqualTo(EditorialStatus.NEEDS_REVIEW);
    }

    @Test
    void contestDatesMustBeMonotonic() {
        Instant t = Instant.parse("2026-11-10T13:00:00Z");
        assertThatCode(() -> Contest.validateDates(t, t, t.plusSeconds(60), t.plusSeconds(120), t.plusSeconds(180)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> Contest.validateDates(t, t, t.plusSeconds(60), t.plusSeconds(60), t.plusSeconds(180)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Contest.validateDates(t.plusSeconds(10), t, t.plusSeconds(60), t.plusSeconds(90), t.plusSeconds(180)))
                .isInstanceOf(DomainException.class);
    }
}
