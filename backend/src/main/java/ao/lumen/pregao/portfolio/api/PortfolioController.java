package ao.lumen.pregao.portfolio.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.portfolio.app.PortfolioService;
import ao.lumen.pregao.portfolio.app.PortfolioService.Dashboard;
import ao.lumen.pregao.portfolio.app.PortfolioService.Portfolio;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PortfolioController {
    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping("/me/summary")
    public PortfolioService.Summary summary(@AuthenticationPrincipal SessionUser me) {
        return service.summary(me.id());
    }

    @GetMapping("/me/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal SessionUser me) {
        return service.dashboard(me.id());
    }

    @GetMapping("/me/portfolio")
    public Portfolio mine(@AuthenticationPrincipal SessionUser me) {
        return service.mine(me.id());
    }

    /** Portefólio público (opt-in). Sem email nem dados de contacto. */
    @GetMapping("/public/portfolios/{handle}")
    public Portfolio publicPortfolio(@PathVariable String handle) {
        return service.publicPortfolio(handle);
    }
}
