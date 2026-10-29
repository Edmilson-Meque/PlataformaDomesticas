package plataforma.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import plataforma.util.Util;

// EVENT BUS: o caminho central por onde passam todos os eventos (padrão Publish/Subscribe).
// É o ÚNICO que conhece os consumidores. Os produtores só chamam publicar(...).
//  - consumidor SÍNCRONO : corre na mesma thread do produtor (o produtor espera)
//  - consumidor ASSÍNCRONO: corre numa thread do pool (o produtor NÃO espera)
public class EventBus {

    private record Subscricao(EventListener<?> consumidor, boolean assincrono) {
    }

    private final Map<TipoEvento, List<Subscricao>> subscricoes = new ConcurrentHashMap<>();
    private final AtomicInteger numeroThread = new AtomicInteger();
    private final ExecutorService pool = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r);
        t.setName("evento-" + numeroThread.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    public <T> void subscrever(TipoEvento tipo, EventListener<T> consumidor) {
        subscricoes.computeIfAbsent(tipo, k -> new CopyOnWriteArrayList<>()).add(new Subscricao(consumidor, false));
    }

    public <T> void subscreverAssincrono(TipoEvento tipo, EventListener<T> consumidor) {
        subscricoes.computeIfAbsent(tipo, k -> new CopyOnWriteArrayList<>()).add(new Subscricao(consumidor, true));
    }

    @SuppressWarnings("unchecked")
    public <T> void publicar(TipoEvento tipo, T dados, String utilizador) {
        EventoDominio<T> evento = new EventoDominio<>(tipo, dados, utilizador);
        for (Subscricao s : new ArrayList<>(subscricoes.getOrDefault(tipo, List.of()))) {
            EventListener<T> consumidor = (EventListener<T>) s.consumidor();
            if (s.assincrono()) {
                pool.execute(() -> entregar(consumidor, evento));
            } else {
                entregar(consumidor, evento);
            }
        }
    }

    // Fiabilidade simples: se o consumidor falhar, tenta mais 2 vezes
    private <T> void entregar(EventListener<T> consumidor, EventoDominio<T> evento) {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            try {
                consumidor.onEvent(evento);
                return;
            } catch (RuntimeException e) {
                System.out.println("   [EventBus] " + consumidor.getClass().getSimpleName() + " falhou ("
                        + e.getMessage() + ") - tentativa " + tentativa + "/3");
                Util.dormir(200);
            }
        }
    }

    // Espera um pouco para os consumidores assíncronos terminarem de imprimir
    public void aguardar() {
        Util.dormir(400);
    }

    public void encerrar() {
        pool.shutdown();
        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
