package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.grading.domain.PgcAccount;
import ao.lumen.pregao.grading.infra.PgcAccountRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PgcAccountService {
    public static final String DEFAULT_VERSION = "PGC-a-confirmar";
    private final PgcAccountRepository repo;
    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public PgcAccountService(PgcAccountRepository repo) {
        this.repo = repo;
    }

    public record AccountDto(String code, String name, String classCode, String className, boolean placeholder) {}

    @Transactional(readOnly = true)
    public Set<String> codes(String version) {
        String v = version == null ? DEFAULT_VERSION : version;
        return cache.computeIfAbsent(v, k -> repo.findByVersionOrderByCodeAsc(k).stream()
                .map(PgcAccount::getCode).collect(Collectors.toUnmodifiableSet()));
    }

    @Transactional(readOnly = true)
    public List<AccountDto> search(String version, String q, int limit) {
        String v = version == null || version.isBlank() ? DEFAULT_VERSION : version;
        List<PgcAccount> rows = q == null || q.isBlank()
                ? repo.findByVersionOrderByCodeAsc(v).stream().limit(limit).toList()
                : repo.search(v, q.strip(), PageRequest.of(0, limit));
        return rows.stream().map(a -> new AccountDto(a.getCode(), a.getName(), a.getClassCode(), a.getClassName(),
                a.isPlaceholder())).toList();
    }
}
