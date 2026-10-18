package plataforma.model;

public class Denuncia {

    private int id;
    private Contratante autor;
    private Trabalhadora alvo;
    private String motivo;
    private boolean activa;

    public Denuncia(int id, Contratante autor, Trabalhadora alvo, String motivo) {
        this.id = id;
        this.autor = autor;
        this.alvo = alvo;
        this.motivo = motivo;
        this.activa = true;
    }

    public int getId() {
        return id;
    }

    public Contratante getAutor() {
        return autor;
    }

    public Trabalhadora getAlvo() {
        return alvo;
    }

    public String getMotivo() {
        return motivo;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}