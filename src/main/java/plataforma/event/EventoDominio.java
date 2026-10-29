package plataforma.event;

import java.time.LocalDateTime;

// O OBJECTO DE EVENTO é um POJO: só atributos, construtor e getters.
// Boa prática: NÃO tem métodos que alterem o negócio (isso fica nos consumidores).
public class EventoDominio<T> {

    private final TipoEvento tipo;
    private final T dados;              // o PAYLOAD (ex.: a Candidatura)
    private final String utilizador;    // quem fez a acção (para a auditoria)
    private final LocalDateTime ocorridoEm;

    public EventoDominio(TipoEvento tipo, T dados, String utilizador) {
        this.tipo = tipo;
        this.dados = dados;
        this.utilizador = utilizador;
        this.ocorridoEm = LocalDateTime.now();
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public T getDados() {
        return dados;
    }

    public String getUtilizador() {
        return utilizador;
    }

    public LocalDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
