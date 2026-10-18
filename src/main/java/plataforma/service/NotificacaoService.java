package plataforma.service;

import java.util.ArrayList;
import java.util.List;

import plataforma.model.Notificacao;
import plataforma.model.Utilizador;

// P04 - Gestão de notificações
public class NotificacaoService {

    private List<Notificacao> notificacoes;
    private int proximoId = 1;

    public NotificacaoService(List<Notificacao> notificacoes) {
        this.notificacoes = notificacoes;
    }

    // Cria a notificação, guarda-a na lista e mostra-a na consola.
    public void enviar(Utilizador destinatario, String mensagem) {
        Notificacao n = new Notificacao(proximoId++, destinatario, mensagem);
        notificacoes.add(n);
        System.out.println("   [Notificação] " + n);
    }

    // Todas as notificações de um utilizador.
    public List<Notificacao> listarDe(Utilizador utilizador) {
        List<Notificacao> resultado = new ArrayList<>();
        for (Notificacao n : notificacoes) {
            if (n.getDestinatario() == utilizador) {
                resultado.add(n);
            }
        }
        return resultado;
    }
}