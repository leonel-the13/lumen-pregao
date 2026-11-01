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
}
