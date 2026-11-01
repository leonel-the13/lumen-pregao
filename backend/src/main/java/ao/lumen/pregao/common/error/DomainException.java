package ao.lumen.pregao.common.error;

import org.springframework.http.HttpStatus;

/** Excepção de domínio com código estável, convertida em application/problem+json. */
public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String title;

    public DomainException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String title() { return title; }

    public static DomainException notFound(String code, String detail) {
        return new DomainException(HttpStatus.NOT_FOUND, code, "Não encontrado", detail);
    }

    public static DomainException forbidden(String code, String detail) {
        return new DomainException(HttpStatus.FORBIDDEN, code, "Sem permissão", detail);
    }

    public static DomainException conflict(String code, String detail) {
        return new DomainException(HttpStatus.CONFLICT, code, "Conflito", detail);
    }

    public static DomainException rule(String code, String detail) {
        return new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, code, "Regra de negócio", detail);
    }

    public static DomainException badRequest(String code, String detail) {
        return new DomainException(HttpStatus.BAD_REQUEST, code, "Pedido inválido", detail);
    }

    public static DomainException tooMany(String detail) {
        return new DomainException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Demasiados pedidos", detail);
    }
}
