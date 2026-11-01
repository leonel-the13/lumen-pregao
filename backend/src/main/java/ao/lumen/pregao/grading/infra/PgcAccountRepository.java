package ao.lumen.pregao.grading.infra;

import ao.lumen.pregao.grading.domain.PgcAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PgcAccountRepository extends JpaRepository<PgcAccount, UUID> {
    List<PgcAccount> findByVersionOrderByCodeAsc(String version);

    @Query("""
            select a from PgcAccount a
            where a.version = :version
              and (a.code like concat(cast(:q as string), '%')
                   or lower(a.name) like lower(concat('%', cast(:q as string), '%')))
            order by a.code
            """)
    List<PgcAccount> search(@Param("version") String version, @Param("q") String q, Pageable page);
}
