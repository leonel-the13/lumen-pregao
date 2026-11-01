package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.common.privacy.PersonalDataContributor;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ContestPersonalData implements PersonalDataContributor {
    private final ContestService contests;

    ContestPersonalData(ContestService contests) {
        this.contests = contests;
    }

    @Override
    public String section() {
        return "contests";
    }

    @Override
    public Object export(UUID userId) {
        return contests.history(userId);
    }
}
