package plataforma.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.model.Candidatura;
import plataforma.model.Contratante;
import plataforma.model.EstadoReuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.util.Util;

// P01 - Agendamento de reuniões  (Problema G2 nº1: duas contratantes, a mesma trabalhadora)
// PARADIGMA CONCORRENTE com LOCK:
//  - a 1ª contratante que chega obtém o lock da trabalhadora e reserva-a (EM_REUNIAO);
//  - a 2ª espera pelo lock, vê que já está reservada e vai para a FILA DE ESPERA (UNDER_OFFER);
//  - se a reunião da 1ª for recusada, cancelada ou expirar, a próxima da fila recebe
//    AUTOMATICAMENTE um convite de reunião.
public class AgendamentoService {

    // Um pedido na fila de espera (pode vir de uma candidatura aceite)
    public record PedidoEspera(Contratante contratante, Candidatura candidatura) {
    }

    // Eventos que acontecem dentro do lock e que só são publicados DEPOIS do unlock
    private static class Efeitos {
        final List<Reuniao> promovidas = new ArrayList<>();
        final List<Candidatura> canceladas = new ArrayList<>();
    }

    private final List<Reuniao> reunioes;
    private final NotificacaoService notificacaoService;
    private final EventBus eventBus;
    private final Map<Trabalhadora, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<Trabalhadora, Queue<PedidoEspera>> filaEspera = new ConcurrentHashMap<>();

    public AgendamentoService(List<Reuniao> reunioes, NotificacaoService notificacaoService, EventBus eventBus) {
        this.reunioes = reunioes;
        this.notificacaoService = notificacaoService;
        this.eventBus = eventBus;
    }

    // Um lock por trabalhadora. "true" = lock justo: quem chega primeiro entra primeiro (evita starvation)
    public ReentrantLock lockDe(Trabalhadora t) {
        return locks.computeIfAbsent(t, k -> new ReentrantLock(true));
    }

    public Reuniao agendar(Contratante contratante, Trabalhadora trabalhadora) {
        return agendar(contratante, trabalhadora, null);
    }

    // Agendamento directo (menu) ou automático (candidatura aceite): é o MESMO processo,
    // por isso as duas formas passam pelo mesmo lock e pela mesma fila.
    public Reuniao agendar(Contratante contratante, Trabalhadora trabalhadora, Candidatura candidatura) {
        Reuniao reuniao;
        ReentrantLock lock = lockDe(trabalhadora);
        lock.lock();
        try {
            // ---------------- SECÇÃO CRÍTICA: só uma thread de cada vez ----------------
            Util.dormir(20); // simula a leitura na base de dados
            EstadoTrabalhadora estado = trabalhadora.getEstado();
            if (estado == EstadoTrabalhadora.CONTRATADA) {
                Util.log("[Rejeitado] " + trabalhadora.getNome() + " já está contratada.");
                return null;
            }
            if (temReuniaoActiva(contratante, trabalhadora)) {
                Util.log("[Aviso] " + contratante.getNome() + " já tem uma reunião activa com " + trabalhadora.getNome() + ".");
                return null;
            }
            if (estado == EstadoTrabalhadora.EM_REUNIAO || estado == EstadoTrabalhadora.UNDER_OFFER) {
                Queue<PedidoEspera> fila = filaEspera.computeIfAbsent(trabalhadora, k -> new LinkedList<>());
                if (fila.stream().anyMatch(p -> p.contratante() == contratante)) {
                    Util.log("[Aviso] " + contratante.getNome() + " já está na fila de espera de " + trabalhadora.getNome() + ".");
                    return null;
                }
                fila.add(new PedidoEspera(contratante, candidatura));
                trabalhadora.setEstado(EstadoTrabalhadora.UNDER_OFFER);
                Util.log("[Fila de espera / UNDER OFFER] " + trabalhadora.getNome() + " já está reservada. "
                        + contratante.getNome() + " ficou na posição " + fila.size() + ".");
                notificacaoService.enviar(contratante, trabalhadora.getNome()
                        + " está reservada. Ficou na fila de espera (posição " + fila.size()
                        + ") e receberá o convite automaticamente.");
                return null;
            }
            reuniao = criarReuniao(contratante, trabalhadora, candidatura);
            trabalhadora.setEstado(EstadoTrabalhadora.EM_REUNIAO);
            Util.log("[Reservado com LOCK] " + reuniao);
        } finally {
            lock.unlock(); // liberta SEMPRE, mesmo que haja erro
        }
        // O evento é publicado FORA do lock (boa prática para evitar deadlocks)
        eventBus.publicar(TipoEvento.REUNIAO_AGENDADA, reuniao, contratante.getNome());
        return reuniao;
    }

    // A trabalhadora responde ao convite (chamado pelo ACTOR dela)
    public void responderConvite(Reuniao reuniao, boolean aceite) {
        Trabalhadora trabalhadora = reuniao.getTrabalhadora();
        Efeitos efeitos = new Efeitos();
        ReentrantLock lock = lockDe(trabalhadora);
        lock.lock();
        try {
            if (reuniao.getEstado() != EstadoReuniao.PENDENTE) {
                Util.log("[Aviso] A reunião " + reuniao.getId() + " já não está pendente (" + reuniao.getEstado() + ").");
                return;
            }
            if (aceite) {
                reuniao.setEstado(EstadoReuniao.ACEITE);
            } else {
                reuniao.setEstado(EstadoReuniao.RECUSADA);
                libertar(reuniao, efeitos);
            }
        } finally {
            lock.unlock();
        }
        eventBus.publicar(aceite ? TipoEvento.REUNIAO_ACEITE : TipoEvento.REUNIAO_RECUSADA, reuniao, trabalhadora.getNome());
        publicarEfeitos(efeitos);
    }

    // A contratante cancela uma reunião pendente ou aceite (que ainda não deu contrato)
    public boolean cancelarReuniao(Reuniao reuniao) {
        Trabalhadora trabalhadora = reuniao.getTrabalhadora();
        Efeitos efeitos = new Efeitos();
        ReentrantLock lock = lockDe(trabalhadora);
        lock.lock();
        try {
            if (!reuniao.isActiva()) {
                Util.log("[Aviso] A reunião " + reuniao.getId() + " já não está activa.");
                return false;
            }
            reuniao.setEstado(EstadoReuniao.CANCELADA);
            libertar(reuniao, efeitos);
        } finally {
            lock.unlock();
        }
        eventBus.publicar(TipoEvento.REUNIAO_CANCELADA, reuniao, reuniao.getContratante().getNome());
        publicarEfeitos(efeitos);
        return true;
    }

    // Chamado pela Task de manutenção: reuniões paradas há mais de "dias" dias EXPIRAM,
    // para a trabalhadora não ficar presa em EM_REUNIAO para sempre.
    public int expirarReunioes(int dias) {
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        int expiradas = 0;
        for (Reuniao r : new ArrayList<>(reunioes)) {
            if (!r.isActiva() || r.getCriadaEm().isAfter(limite)) {
                continue;
            }
            Efeitos efeitos = new Efeitos();
            ReentrantLock lock = lockDe(r.getTrabalhadora());
            lock.lock();
            try {
                if (!r.isActiva()) {
                    continue;
                }
                r.setEstado(EstadoReuniao.EXPIRADA);
                libertar(r, efeitos);
                expiradas++;
            } finally {
                lock.unlock();
            }
            eventBus.publicar(TipoEvento.REUNIAO_EXPIRADA, r, "Sistema");
            publicarEfeitos(efeitos);
        }
        return expiradas;
    }

    // Depois de um contrato: as outras reuniões da trabalhadora são canceladas e a fila acaba.
    public List<Reuniao> encerrarPorContratacao(Trabalhadora trabalhadora, Reuniao reuniaoDoContrato) {
        List<Reuniao> canceladas = new ArrayList<>();
        Efeitos efeitos = new Efeitos();
        ReentrantLock lock = lockDe(trabalhadora);
        lock.lock();
        try {
            for (Reuniao r : reunioes) {
                if (r != reuniaoDoContrato && r.getTrabalhadora() == trabalhadora && r.isActiva()) {
                    r.setEstado(EstadoReuniao.CANCELADA);
                    reabrirVagaDaCandidatura(r, efeitos);
                    canceladas.add(r);
                }
            }
            Queue<PedidoEspera> fila = filaEspera.remove(trabalhadora);
            if (fila != null) {
                for (PedidoEspera p : fila) {
                    notificacaoService.enviar(p.contratante(), trabalhadora.getNome()
                            + " foi contratada por outra pessoa. Saiu da fila de espera.");
                }
            }
        } finally {
            lock.unlock();
        }
        for (Reuniao r : canceladas) {
            notificacaoService.enviar(r.getContratante(), "A reunião com " + trabalhadora.getNome()
                    + " foi cancelada: ela foi contratada por outra pessoa.");
        }
        publicarEfeitos(efeitos);
        return canceladas;
    }

    // ---- chamado JÁ DENTRO do lock: a reunião caiu, o que acontece à trabalhadora? ----
    private void libertar(Reuniao reuniao, Efeitos efeitos) {
        reabrirVagaDaCandidatura(reuniao, efeitos);
        Trabalhadora trabalhadora = reuniao.getTrabalhadora();
        if (trabalhadora.getEstado() == EstadoTrabalhadora.CONTRATADA) {
            return; // uma trabalhadora contratada NUNCA volta a ficar disponível
        }
        if (reunioes.stream().anyMatch(r -> r.getTrabalhadora() == trabalhadora && r.isActiva())) {
            return; // ainda tem outra reunião activa
        }
        Queue<PedidoEspera> fila = filaEspera.get(trabalhadora);
        if (fila == null || fila.isEmpty()) {
            filaEspera.remove(trabalhadora);
            trabalhadora.setEstado(EstadoTrabalhadora.DISPONIVEL);
            return;
        }
        PedidoEspera proximo = fila.poll();
        Reuniao nova = criarReuniao(proximo.contratante(), trabalhadora, proximo.candidatura());
        trabalhadora.setEstado(fila.isEmpty() ? EstadoTrabalhadora.EM_REUNIAO : EstadoTrabalhadora.UNDER_OFFER);
        if (fila.isEmpty()) {
            filaEspera.remove(trabalhadora);
        }
        Util.log("[Fila de espera] " + proximo.contratante().getNome()
                + " recebeu AUTOMATICAMENTE o convite: " + nova);
        efeitos.promovidas.add(nova);
    }

    // Se a reunião vinha de uma candidatura aceite, a candidatura é cancelada e a vaga volta a abrir
    private void reabrirVagaDaCandidatura(Reuniao reuniao, Efeitos efeitos) {
        Candidatura c = reuniao.getCandidatura();
        if (c == null || c.getVaga().isAberta()) {
            return;
        }
        try {
            c.cancelar(); // Padrão State: ACEITE -> CANCELADA
        } catch (IllegalStateException e) {
            return;
        }
        c.getVaga().setAberta(true);
        notificacaoService.enviar(c.getVaga().getContratante(), "A reunião com " + c.getTrabalhadora().getNome()
                + " não avançou. A vaga '" + c.getVaga().getTitulo() + "' voltou a estar ABERTA.");
        efeitos.canceladas.add(c);
    }

    private void publicarEfeitos(Efeitos efeitos) {
        for (Candidatura c : efeitos.canceladas) {
            eventBus.publicar(TipoEvento.CANDIDATURA_CANCELADA, c, "Sistema");
        }
        for (Reuniao r : efeitos.promovidas) {
            eventBus.publicar(TipoEvento.REUNIAO_AGENDADA, r, r.getContratante().getNome());
        }
    }

    // ATOMICIDADE: calcular o próximo id e guardar a reunião é UMA só operação (synchronized),
    // senão duas trabalhadoras diferentes (locks diferentes) podiam receber o mesmo id.
    private Reuniao criarReuniao(Contratante contratante, Trabalhadora trabalhadora, Candidatura candidatura) {
        synchronized (reunioes) {
            Reuniao r = new Reuniao(Util.proximoId(reunioes, Reuniao::getId), contratante, trabalhadora, candidatura);
            reunioes.add(r);
            return r;
        }
    }

    private boolean temReuniaoActiva(Contratante contratante, Trabalhadora trabalhadora) {
        return reunioes.stream().anyMatch(r -> r.getContratante() == contratante
                && r.getTrabalhadora() == trabalhadora && r.isActiva());
    }

    // ---- usados pela base de dados (guardar / carregar a fila de espera) ----
    public Map<Trabalhadora, Queue<PedidoEspera>> getFilaEspera() {
        return filaEspera;
    }

    public void restaurarNaFila(Trabalhadora t, Contratante c, Candidatura candidatura) {
        filaEspera.computeIfAbsent(t, k -> new LinkedList<>()).add(new PedidoEspera(c, candidatura));
    }
}
