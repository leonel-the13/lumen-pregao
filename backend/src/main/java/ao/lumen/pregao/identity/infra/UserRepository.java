package ao.lumen.pregao.identity.infra;

import ao.lumen.pregao.identity.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "course"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "course"})
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithRolesById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"course"})
    List<User> findByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = {"roles", "course"})
    Optional<User> findByPortfolioHandle(String handle);

    @Query("select u.id from User u join u.roles r where r.code in :codes and u.status = 'ACTIVE'")
    List<UUID> idsWithRoles(@Param("codes") Collection<String> codes);

    boolean existsByEmail(String email);

    boolean existsByPseudonym(String pseudonym);

    boolean existsByPortfolioHandle(String handle);

    @EntityGraph(attributePaths = {"roles", "course"})
    @Query("""
            select distinct u from User u left join u.roles r
            where (:q is null or lower(u.name) like lower(concat('%', cast(:q as string), '%'))
                   or u.email like lower(concat('%', cast(:q as string), '%')))
              and (:role is null or r.code = :role)
              and (:cursor is null or u.id < :cursor)
            order by u.id desc
            """)
    List<User> search(@Param("q") String q, @Param("role") String role, @Param("cursor") UUID cursor, Pageable page);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update User u set u.lastSeenAt = :now where u.id = :id and (u.lastSeenAt is null or u.lastSeenAt < :before)")
    int touchSeen(@Param("id") UUID id, @Param("now") java.time.Instant now, @Param("before") java.time.Instant before);

    /** Contas por confirmar com lembrete em falta (no máximo {@code max}, o último antes de {@code remindedBefore}). */
    @Query("""
            select u from User u
            where u.status = 'PENDING_VERIFICATION' and u.createdAt < :createdBefore and u.verifyReminders < :max
              and (u.lastVerifyReminderAt is null or u.lastVerifyReminderAt < :remindedBefore)
            order by u.createdAt
            """)
    List<User> dueVerificationReminders(@Param("createdBefore") java.time.Instant createdBefore, @Param("max") int max,
                                        @Param("remindedBefore") java.time.Instant remindedBefore);

    /** Estudantes activos, com emails ligados, sem actividade desde {@code inactiveSince}. */
    @Query("""
            select distinct u from User u join u.roles r
            where u.status = 'ACTIVE' and r.code = 'STUDENT' and u.emailOptIn = true
              and coalesce(u.lastSeenAt, u.lastLoginAt, u.createdAt) < :inactiveSince
              and (u.lastInactiveReminderAt is null or u.lastInactiveReminderAt < :remindedBefore)
            """)
    List<User> dueInactivityReminders(@Param("inactiveSince") java.time.Instant inactiveSince,
                                      @Param("remindedBefore") java.time.Instant remindedBefore);

    @Query("select count(u), min(u.createdAt) from User u where u.status = 'PENDING_VERIFICATION'")
    List<Object[]> pendingVerificationStats();

    /** Estudantes activos (base dos segmentos dos emails da administração; os filtros aplicam-se no serviço). */
    @EntityGraph(attributePaths = {"course"})
    @Query("select distinct u from User u join u.roles r where u.status = 'ACTIVE' and r.code = 'STUDENT' order by u.id")
    List<User> activeStudents();
}
