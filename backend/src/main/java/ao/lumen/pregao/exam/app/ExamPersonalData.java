package ao.lumen.pregao.exam.app;

import ao.lumen.pregao.common.privacy.PersonalDataContributor;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ExamPersonalData implements PersonalDataContributor {
    private final ExamService exams;

    ExamPersonalData(ExamService exams) {
        this.exams = exams;
    }

    @Override
    public String section() {
        return "exams";
    }

    @Override
    public Object export(UUID userId) {
        return exams.history(userId);
    }
}
