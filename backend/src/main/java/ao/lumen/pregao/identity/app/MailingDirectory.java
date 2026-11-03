package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.UserRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API pública do módulo identity para o envio de emails (campanhas e notificações por email).
 * Só devolve contas activas; o email nunca sai daqui para respostas da API.
 */
@Service
@Transactional(readOnly = true)
public class MailingDirectory {
    private final UserRepository users;

    public MailingDirectory(UserRepository users) {
        this.users = users;
    }

    public record Recipient(UUID id, String email, String name, UUID courseId, String courseName, Integer year,
                            Instant lastActivity, boolean emailOptIn) {}

    /** Estudantes activos (com e sem emails ligados: quem chama decide). */
    public List<Recipient> activeStudents() {
        return users.activeStudents().stream().map(MailingDirectory::toRecipient).toList();
    }

    /** Conta activa (qualquer perfil). */
    public Optional<Recipient> active(UUID id) {
        return users.findById(id).filter(u -> u.getStatus() == UserStatus.ACTIVE).map(MailingDirectory::toRecipient);
    }

    public List<Recipient> active(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return users.findByIdIn(ids).stream().filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .map(MailingDirectory::toRecipient).toList();
    }

    static Recipient toRecipient(User u) {
        var c = u.getCourse();
        return new Recipient(u.getId(), u.getEmail(), u.getName(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                u.getYear(), u.lastActivity(), u.isEmailOptIn());
    }
}
