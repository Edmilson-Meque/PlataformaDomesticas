package plataforma.model;

import java.time.LocalDate;

public class Contrato {

    private int id;
    private Contratante contratante;
    private Trabalhadora trabalhadora;
    private Reuniao reuniao;
    private LocalDate dataContratacao;

    public Contrato(int id, Contratante contratante, Trabalhadora trabalhadora, Reuniao reuniao) {
        this.id = id;
        this.contratante = contratante;
        this.trabalhadora = trabalhadora;
        this.reuniao = reuniao;
        this.dataContratacao = LocalDate.now();
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

    public Reuniao getReuniao() {
        return reuniao;
    }

    public LocalDate getDataContratacao() {
        return dataContratacao;
    }

    @Override
    public String toString() {
        return "Contrato " + id + ": " + contratante.getNome() + " contratou "
                + trabalhadora.getNome() + " em " + dataContratacao;
    }
}