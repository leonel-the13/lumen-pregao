package ao.lumen.pregao.identity.domain;

/**
 * INVITED: conta criada por um administrador; só entra depois de definir a palavra-passe pelo link do email.
 * DELETED: conta anonimizada depois de um pedido de eliminação (os registos imutáveis ficam sem dados pessoais).
 */
public enum UserStatus { PENDING_VERIFICATION, INVITED, ACTIVE, SUSPENDED, DELETION_REQUESTED, DELETED }
