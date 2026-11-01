package ao.lumen.pregao.grading.api;

import ao.lumen.pregao.grading.app.PgcAccountService;
import ao.lumen.pregao.grading.app.PgcAccountService.AccountDto;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/pgc")
public class PgcController {
    private final PgcAccountService accounts;

    public PgcController(PgcAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/accounts")
    public List<AccountDto> search(@RequestParam(required = false) @Size(max = 60) String q,
                                   @RequestParam(required = false) @Size(max = 32) String version) {
        return accounts.search(version, q, 12);
    }
}
