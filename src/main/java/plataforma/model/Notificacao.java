package plataforma.model;

import java.time.LocalDateTime;

public class Notificacao {

    private int id;
    private Utilizador destinatario;
    private String mensagem;
    private LocalDateTime criadaEm;
    private boolean lida;

    public Notificacao(int id, Utilizador destinatario, String mensagem) {
        this.id = id;
        this.destinatario = destinatario;
        this.mensagem = mensagem;
        this.criadaEm = LocalDateTime.now();
        this.lida = false;
    }

    public int getId() {
        return id;
    }

    public Utilizador getDestinatario() {
        return destinatario;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public boolean isLida() {
        return lida;
    }

    public void marcarComoLida() {
        this.lida = true;
    }

    @Override
    public String toString() {
        return "Para " + destinatario.getNome() + ": " + mensagem;
    }
}