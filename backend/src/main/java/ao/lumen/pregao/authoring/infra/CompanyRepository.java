package ao.lumen.pregao.authoring.infra;

import ao.lumen.pregao.authoring.domain.Company;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findAllByOrderByNameAsc();
}
