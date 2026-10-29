package plataforma.event;

// EVENTO = um FACTO que JÁ ACONTECEU (verbo no passado).
//   COMANDO (intenção)        ->  EVENTO (facto)           ->  ESTADO (condição actual)
//   "Aceitar candidatura"     ->  CANDIDATURA_ACEITE       ->  Candidatura: ACEITE
//   "Confirmar pagamento"     ->  PAGAMENTO_CONFIRMADO     ->  Contrato: CONFIRMADO
// Cada evento é de BUSINESS (negócio) ou de SYSTEM (técnico).
public enum TipoEvento {
    CANDIDATURA_SUBMETIDA(Categoria.BUSINESS),
    CANDIDATURA_ACEITE(Categoria.BUSINESS),
    CANDIDATURA_RECUSADA(Categoria.BUSINESS),
    CANDIDATURA_CANCELADA(Categoria.BUSINESS),
    REUNIAO_AGENDADA(Categoria.BUSINESS),
    REUNIAO_ACEITE(Categoria.BUSINESS),
    REUNIAO_RECUSADA(Categoria.BUSINESS),
    REUNIAO_CANCELADA(Categoria.BUSINESS),
    REUNIAO_EXPIRADA(Categoria.BUSINESS),
    CONTRATO_CRIADO(Categoria.BUSINESS),
    PAGAMENTO_CONFIRMADO(Categoria.BUSINESS),
    PERFIL_VALIDADO(Categoria.BUSINESS),
    DENUNCIA_REGISTADA(Categoria.BUSINESS),
    REDE_RESTABELECIDA(Categoria.SYSTEM);   // o telemóvel voltou a ter internet

    public enum Categoria { BUSINESS, SYSTEM }

    private final Categoria categoria;

    TipoEvento(Categoria categoria) {
        this.categoria = categoria;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
