package plataforma.model;

import java.time.LocalDateTime;

import plataforma.model.estado.CandidaturaPendente;
import plataforma.model.estado.EstadoCandidaturaState;

// A candidatura DELEGA no objecto de estado (Padrão State). Já não existe setEstado():
// o estado só muda através de transições válidas.
public class Candidatura {

    private int id;
    private Vaga vaga;
    private Trabalhadora trabalhadora;
    private EstadoCandidaturaState estado;
    private LocalDateTime submetidaEm;

    public Candidatura(int id, Vaga vaga, Trabalhadora trabalhadora) {
        this.id = id;
        this.vaga = vaga;
        this.trabalhadora = trabalhadora;
        this.estado = new CandidaturaPendente();
        this.submetidaEm = LocalDateTime.now();
    }

    // usado ao carregar da base de dados: recria o objecto de estado a partir do texto guardado
    public Candidatura(int id, Vaga vaga, Trabalhadora trabalhadora, EstadoCandidatura estado, LocalDateTime submetidaEm) {
        this.id = id;
        this.vaga = vaga;
        this.trabalhadora = trabalhadora;
        this.estado = EstadoCandidaturaState.de(estado);
        this.submetidaEm = submetidaEm;
    }

    public synchronized void aceitar() {
        estado = estado.aceitar();
    }

    public synchronized void recusar() {
        estado = estado.recusar();
    }

    public synchronized void cancelar() {
        estado = estado.cancelar();
    }

    public void verificarNovaCandidatura() {
        estado.verificarNovaCandidatura(this);
    }

    public int getId() {
        return id;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public Trabalhadora getTrabalhadora() {
        return trabalhadora;
    }

    public EstadoCandidatura getEstado() {
        return estado.codigo();
    }

    public String getClasseEstado() {
        return estado.getClass().getSimpleName();
    }

    public LocalDateTime getSubmetidaEm() {
        return submetidaEm;
    }

    @Override
    public String toString() {
        return String.format("Candidatura #%d: %s -> Vaga '%s' (%s) [%s]",
                id, trabalhadora.getNome(), vaga.getTitulo(), vaga.getProvincia(), getEstado());
    }
}
