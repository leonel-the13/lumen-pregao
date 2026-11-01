package ao.lumen.pregao.exam.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.exam.api.ExamDtos.AnswerInput;
import ao.lumen.pregao.exam.api.ExamDtos.AnswerSaved;
import ao.lumen.pregao.exam.api.ExamDtos.ExamCard;
import ao.lumen.pregao.exam.api.ExamDtos.Report;
import ao.lumen.pregao.exam.api.ExamDtos.SessionStarted;
import ao.lumen.pregao.exam.api.ExamDtos.SessionView;
import ao.lumen.pregao.exam.app.ExamService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ExamController {
    private final ExamService exams;

    public ExamController(ExamService exams) {
        this.exams = exams;
    }

    @GetMapping("/exams")
    public List<ExamCard> list(@AuthenticationPrincipal SessionUser me) {
        return exams.list(me.id());
    }

    @PostMapping("/exams/{id}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionStarted start(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return exams.start(id, me.id());
    }

    @GetMapping("/exam-sessions/{id}")
    public SessionView session(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return exams.view(id, me.id());
    }

    @PutMapping("/exam-sessions/{id}/answers/{q}")
    public AnswerSaved answer(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @PathVariable int q,
                              @RequestBody AnswerInput in) {
        return exams.saveAnswer(id, me.id(), q, in.answer(), in.marked());
    }

    @PostMapping("/exam-sessions/{id}/submit")
    public Report submit(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return exams.submit(id, me.id());
    }

    @GetMapping("/exam-sessions/{id}/report")
    public Report report(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return exams.report(id, me.id());
    }
}
