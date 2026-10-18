package plataforma.model;

public class Contratante extends Utilizador {

    private String morada;

    public Contratante(int id, String nome, String contacto, String morada) {
        super(id, nome, contacto);
        this.morada = morada;
    }

    public String getMorada() {
        return morada;
    }
}