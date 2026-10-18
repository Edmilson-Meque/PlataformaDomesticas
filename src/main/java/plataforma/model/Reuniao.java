package plataforma.model;

import java.time.LocalDateTime;

public class Reuniao {

    private int id;
    private Contratante contratante;
    private Trabalhadora trabalhadora;
    private LocalDateTime criadaEm;
    private EstadoReuniao estado;

    public Reuniao(int id, Contratante contratante, Trabalhadora trabalhadora) {
        this.id = id;
        this.contratante = contratante;
        this.trabalhadora = trabalhadora;
        this.criadaEm = LocalDateTime.now();
        this.estado = EstadoReuniao.PENDENTE;
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

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public EstadoReuniao getEstado() {
        return estado;
    }

    public void setEstado(EstadoReuniao estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Reunião " + id + ": " + contratante.getNome() + " com "
                + trabalhadora.getNome() + " [" + estado + "]";
    }
}