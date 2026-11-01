package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.text.MarkdownSanitizer;
import ao.lumen.pregao.learning.domain.RoomRating;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.RoomRatingRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avaliações de salas: só quem já tentou pelo menos uma tarefa da sala pode avaliar. */
@Service
public class RoomRatingService {
    private final RoomRatingRepository ratings;
    private final TaskRepository tasks;
    private final AttemptRepository attempts;

    public RoomRatingService(RoomRatingRepository ratings, TaskRepository tasks, AttemptRepository attempts) {
        this.ratings = ratings;
        this.tasks = tasks;
        this.attempts = attempts;
    }

    public record RatingSummary(BigDecimal average, long count, Integer myStars, String myComment) {}

    @Transactional
    public RatingSummary rate(UUID user, UUID roomId, int stars, String comment) {
        if (stars < 1 || stars > 5) throw DomainException.badRequest("RATING_INVALID", "A avaliação vai de 1 a 5 estrelas.");
        List<UUID> taskIds = tasks.findByRoomIds(List.of(roomId)).stream().map(Task::getId).toList();
        if (taskIds.isEmpty() || attempts.findForUserTasks(user, taskIds).isEmpty()) {
            throw DomainException.rule("RATING_NOT_ALLOWED", "Resolve pelo menos uma tarefa desta sala antes de a avaliar.");
        }
        RoomRating r = ratings.findByUserIdAndRoomId(user, roomId).orElseGet(() -> new RoomRating(user, roomId));
        String clean = comment == null || comment.isBlank() ? null : MarkdownSanitizer.clean(comment.strip());
        r.rate(stars, clean);
        ratings.save(r);
        return summary(user, roomId);
    }

    @Transactional(readOnly = true)
    public RatingSummary summary(UUID user, UUID roomId) {
        Object[] row = ratings.summary(roomId).getFirst();
        var mine = ratings.findByUserIdAndRoomId(user, roomId);
        BigDecimal avg = row[0] == null ? BigDecimal.ZERO : new BigDecimal(row[0].toString()).setScale(1, RoundingMode.HALF_UP);
        return new RatingSummary(avg, ((Number) row[1]).longValue(), mine.map(RoomRating::getStars).orElse(null),
                mine.map(RoomRating::getComment).orElse(null));
    }
}
