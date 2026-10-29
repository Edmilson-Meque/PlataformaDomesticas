package plataforma.offline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import plataforma.concorrencia.ActorTrabalhadora;
import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.service.NotificacaoService;
import plataforma.util.Util;

// OFFLINE-FIRST + COMUNICAÇÃO ASSÍNCRONA (Problemas G2 nº2 e nº3)
// - A acção da trabalhadora é GRAVADA LOCALMENTE no telemóvel, mesmo sem internet.
// - As notificações ficam numa CACHE local: sem dados móveis ela continua a vê-las.
// - Quando a rede volta, é publicado o evento de SYSTEM "REDE_RESTABELECIDA" e as acções
//   pendentes são enviadas em BACKGROUND (outra thread), sem bloquear o ecrã.
public class Telemovel {

    private record AccaoPendente(Reuniao reuniao, boolean aceitar) {
    }

    private final Trabalhadora dona;
    private final ActorTrabalhadora actor;
    private final NotificacaoService notificacaoService;
    private final EventBus eventBus;
    private final List<AccaoPendente> filaLocal = new CopyOnWriteArrayList<>();
    private final List<String> cacheNotificacoes = new CopyOnWriteArrayList<>();
    private volatile boolean online = true;

    public Telemovel(Trabalhadora dona, ActorTrabalhadora actor, NotificacaoService notificacaoService, EventBus eventBus) {
        this.dona = dona;
        this.actor = actor;
        this.notificacaoService = notificacaoService;
        this.eventBus = eventBus;
    }

    public void responderConvite(Reuniao reuniao, boolean aceitar) {
        filaLocal.add(new AccaoPendente(reuniao, aceitar));
        Util.log("[Telemóvel " + dona.getNome() + "] Resposta GUARDADA LOCALMENTE (" + filaLocal.size() + " pendente(s))");
        if (online) {
            sincronizar();
        } else {
            Util.log("[Telemóvel " + dona.getNome() + "] Sem internet: será enviada quando a rede voltar.");
        }
    }

    // Envia as acções pendentes ao servidor (que as entrega ao actor da trabalhadora)
    public synchronized void sincronizar() {
        for (AccaoPendente a : new ArrayList<>(filaLocal)) {
            Util.log("[Telemóvel " + dona.getNome() + "] Enviada ao servidor: resposta à reunião " + a.reuniao().getId());
            filaLocal.remove(a);
            actor.enviar(a.reuniao(), a.aceitar());
        }
        actualizarCache();
    }

    public List<String> verNotificacoes() {
        if (online) {
            actualizarCache();
            notificacaoService.listarDe(dona).forEach(n -> n.marcarComoLida()); // já foram vistas
        } else {
            Util.log("[Telemóvel " + dona.getNome() + "] SEM INTERNET: a mostrar as notificações guardadas no telemóvel");
        }
        return cacheNotificacoes;
    }

    // Copia as notificações do servidor para o telemóvel (as não lidas ficam marcadas [NOVA])
    private void actualizarCache() {
        cacheNotificacoes.clear();
        notificacaoService.listarDe(dona).forEach(n -> cacheNotificacoes.add((n.isLida() ? "       " : "[NOVA] ") + n.getMensagem()));
    }

    public void desligarInternet() {
        online = false;
    }

    public void ligarInternet() {
        online = true;
        eventBus.publicar(TipoEvento.REDE_RESTABELECIDA, this, dona.getNome()); // evento de SYSTEM
    }

    public boolean isOnline() {
        return online;
    }

    public int pendentes() {
        return filaLocal.size();
    }

    public Trabalhadora getDona() {
        return dona;
    }
}
