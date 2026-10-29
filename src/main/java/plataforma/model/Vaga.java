package plataforma.model;

import java.time.LocalDateTime;

public class Vaga {

    private int id;
    private Contratante contratante;
    private String titulo;
    private String provincia;
    private String competenciaRequerida;
    private double salarioOferecido;
    private boolean aberta;
    private LocalDateTime criadaEm;

    public Vaga(int id, Contratante contratante, String titulo, String provincia, String competenciaRequerida, double salarioOferecido) {
        this.id = id;
        this.contratante = contratante;
        this.titulo = titulo;
        this.provincia = provincia;
        this.competenciaRequerida = competenciaRequerida;
        this.salarioOferecido = salarioOferecido;
        this.aberta = true;
        this.criadaEm = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public Contratante getContratante() {
        return contratante;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getCompetenciaRequerida() {
        return competenciaRequerida;
    }

    public double getSalarioOferecido() {
        return salarioOferecido;
    }

    public boolean isAberta() {
        return aberta;
    }

    public void setAberta(boolean aberta) {
        this.aberta = aberta;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    @Override
    public String toString() {
        return String.format("Vaga #%d: %s (%s) | Req: %s | Salário: %.2f MT | Contratante: %s [%s]",
                id, titulo, provincia, competenciaRequerida, salarioOferecido,
                contratante.getNome(), aberta ? "ABERTA" : "FECHADA");
    }
}