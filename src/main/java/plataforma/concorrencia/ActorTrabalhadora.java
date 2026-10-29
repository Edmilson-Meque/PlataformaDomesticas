package plataforma.concorrencia;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.service.AgendamentoService;
import plataforma.util.Util;

// MODELO DE ACTORES (Problema G2: Negociação das condições)
// Cada trabalhadora é um ACTOR com a sua CAIXA DE CORREIO (fila de mensagens).
// As respostas às reuniões são MENSAGENS. O actor processa UMA mensagem de cada vez,
// por isso o estado das reuniões dela nunca é alterado por duas threads ao mesmo tempo.
public class ActorTrabalhadora {

    private record Mensagem(Reuniao reuniao, boolean aceitar) {
    }

    private final Trabalhadora trabalhadora;
    private final AgendamentoService agendamentoService;
    private final BlockingQueue<Mensagem> caixaDeCorreio = new LinkedBlockingQueue<>();

    public ActorTrabalhadora(Trabalhadora trabalhadora, AgendamentoService agendamentoService) {
        this.trabalhadora = trabalhadora;
        this.agendamentoService = agendamentoService;
        Thread t = new Thread(this::processarMensagens, "actor-" + trabalhadora.getNome());
        t.setDaemon(true);
        t.start();
    }

    // Qualquer thread pode deixar uma mensagem na caixa (não mexe no estado directamente)
    public void enviar(Reuniao reuniao, boolean aceitar) {
        caixaDeCorreio.add(new Mensagem(reuniao, aceitar));
    }

    private void processarMensagens() {
        while (true) {
            try {
                Mensagem m = caixaDeCorreio.take(); // espera (sem gastar CPU) até chegar uma mensagem
                Util.log("[Actor " + trabalhadora.getNome() + "] a processar a resposta à reunião "
                        + m.reuniao().getId() + " (uma mensagem de cada vez)");
                agendamentoService.responderConvite(m.reuniao(), m.aceitar());
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
