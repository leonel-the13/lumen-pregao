package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.common.error.DomainException;

/** Resposta com formato inválido: recusada antes de contar como tentativa. */
public class AnswerFormatException extends DomainException {
    public AnswerFormatException(String detail) {
        super(org.springframework.http.HttpStatus.BAD_REQUEST, "ANSWER_FORMAT_INVALID", "Resposta inválida", detail);
    }
}
