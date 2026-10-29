package plataforma.model;

import java.time.LocalDate;

public class Contrato {

    private int id;
    private Contratante contratante;
    private Trabalhadora trabalhadora;
    private Reuniao reuniao;
    private LocalDate dataContratacao;
    private volatile boolean pago; // false = "pendente de pagamento", true = "confirmado"

    public Contrato(int id, Contratante contratante, Trabalhadora trabalhadora, Reuniao reuniao) {
        this.id = id;
        this.contratante = contratante;
        this.trabalhadora = trabalhadora;
        this.reuniao = reuniao;
        this.dataContratacao = LocalDate.now();
    }

    // usado ao carregar da base de dados
    public Contrato(int id, Contratante contratante, Trabalhadora trabalhadora, Reuniao reuniao,
                    LocalDate dataContratacao, boolean pago) {
        this(id, contratante, trabalhadora, reuniao);
        this.dataContratacao = dataContratacao;
        this.pago = pago;
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

    public boolean isPago() {
        return pago;
    }

    public void setPago(boolean pago) {
        this.pago = pago;
    }

    @Override
    public String toString() {
        return "Contrato " + id + ": " + contratante.getNome() + " contratou "
                + trabalhadora.getNome() + " em " + dataContratacao
                + (pago ? " [CONFIRMADO]" : " [PENDENTE DE PAGAMENTO]");
    }
}