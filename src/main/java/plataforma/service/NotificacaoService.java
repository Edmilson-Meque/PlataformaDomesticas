package plataforma.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import plataforma.model.Notificacao;
import plataforma.model.Utilizador;
import plataforma.util.Util;

// P04 - Gestão de notificações (Programação Funcional)
// As notificações NÃO são impressas no ecrã de quem fez a acção: ficam guardadas e só
// aparecem na aba "Ver notificações" do destinatário.
public class NotificacaoService {

    private final List<Notificacao> notificacoes;

    public NotificacaoService(List<Notificacao> notificacoes) {
        this.notificacoes = notificacoes;
    }

    // synchronized: vários consumidores (threads) enviam notificações ao mesmo tempo
    public synchronized void enviar(Utilizador destinatario, String mensagem) {
        notificacoes.add(new Notificacao(Util.proximoId(notificacoes, Notificacao::getId), destinatario, mensagem));
    }

    // Funcional: filter + sorted (mais recentes primeiro)
    public synchronized List<Notificacao> listarDe(Utilizador utilizador) {
        return notificacoes.stream()
                .filter(n -> n.getDestinatario() == utilizador)
                .sorted(Comparator.comparing(Notificacao::getCriadaEm).thenComparing(Notificacao::getId).reversed())
                .collect(Collectors.toList());
    }

    // Funcional: filter + count
    public synchronized long contarNaoLidas(Utilizador utilizador) {
        return notificacoes.stream()
                .filter(n -> n.getDestinatario() == utilizador)
                .filter(n -> !n.isLida())
                .count();
    }
}
