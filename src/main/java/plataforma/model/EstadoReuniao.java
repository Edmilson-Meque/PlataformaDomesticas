package plataforma.model;

public enum EstadoReuniao {
    PENDENTE,   // convite enviado, a trabalhadora ainda não respondeu
    ACEITE,
    RECUSADA,
    CANCELADA,  // a contratante cancelou, ou a trabalhadora foi contratada por outra pessoa
    EXPIRADA,   // ficou parada demasiados dias (Task de manutenção)
    CONCLUIDA   // deu origem a um contrato (já não pode ser cancelada nem expirar)
}
