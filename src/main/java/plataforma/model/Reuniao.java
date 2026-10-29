package plataforma.model;

import java.time.LocalDateTime;

public class Reuniao {

    private int id;
    private Contratante contratante;
    private Trabalhadora trabalhadora;
    private Candidatura candidatura; // a candidatura que deu origem à reunião (pode ser null)
    private LocalDateTime criadaEm;
    private volatile EstadoReuniao estado;

    public Reuniao(int id, Contratante contratante, Trabalhadora trabalhadora, Candidatura candidatura) {
        this(id, contratante, trabalhadora, candidatura, EstadoReuniao.PENDENTE, LocalDateTime.now());
    }

    // usado ao carregar da base de dados
    public Reuniao(int id, Contratante contratante, Trabalhadora trabalhadora, Candidatura candidatura,
                   EstadoReuniao estado, LocalDateTime criadaEm) {
        this.id = id;
        this.contratante = contratante;
        this.trabalhadora = trabalhadora;
        this.candidatura = candidatura;
        this.estado = estado;
        this.criadaEm = criadaEm;
    }

    public int getId() {
        return id;
    }

    public Contratante getContratante() {
        return contratante;
    }

    public Trabalhadora getTrabalhadora() {
        return trabalhadora;
    }

    public Candidatura getCandidatura() {
        return candidatura;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public EstadoReuniao getEstado() {
        return estado;
    }

    public void setEstado(EstadoReuniao estado) {
        this.estado = estado;
    }

    public boolean isActiva() {
        return estado == EstadoReuniao.PENDENTE || estado == EstadoReuniao.ACEITE;
    }

    @Override
    public String toString() {
        return "Reunião " + id + ": " + contratante.getNome() + " com "
                + trabalhadora.getNome() + " [" + estado + "]"
                + (candidatura != null ? " (vaga '" + candidatura.getVaga().getTitulo() + "')" : "");
    }
}
