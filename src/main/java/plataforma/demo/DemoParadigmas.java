package plataforma.demo;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import plataforma.broker.Broker;
import plataforma.concorrencia.MonitorPagamento;
import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.event.consumidores.MotorLogicoConsumer;
import plataforma.logica.MotorRegras;
import plataforma.model.Contratante;
import plataforma.model.Reuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Trabalhadora;
import plataforma.service.AgendamentoService;
import plataforma.service.ModeracaoService;
import plataforma.service.NotificacaoService;
import plataforma.util.Util;

// Demonstrações curtas de cada paradigma, para apresentar na aula.
public class DemoParadigmas {

    public static void menu(Scanner scanner) {
        String opcao = "";
        while (!opcao.equals("0")) {
            System.out.println("\n--- DEMONSTRAÇÕES DOS PARADIGMAS ---");
            System.out.println("1 - Tradicional vs Eventos SÍNCRONOS vs Eventos ASSÍNCRONOS");
            System.out.println("2 - Threads: run() vs start() e controlar a ordem A/B");
            System.out.println("3 - Sequencial vs Concorrente vs Paralelo (núcleos)");
            System.out.println("4 - Race condition: 2 contratantes ao mesmo tempo (sem lock vs com lock)");
            System.out.println("5 - Atomicidade: contador partilhado (int vs AtomicInteger)");
            System.out.println("6 - synchronized + wait/notify: pagamento -> notificação");
            System.out.println("7 - Deadlock e como o evitar (tryLock)");
            System.out.println("8 - Broker: pedidos de pagamento numa fila (o serviço vem buscá-los)");
            System.out.println("9 - Memória: objectos que perdem a referência e o Garbage Collector");
            System.out.println("10 - Paradigma Lógico: denúncias de contratantes diferentes bloqueiam a contratação");
            System.out.println("0 - Voltar");
            System.out.print("Opção: ");
            opcao = scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
            try {
                switch (opcao) {
                    case "1" -> tradicionalVsEventos();
                    case "2" -> runVsStart();
                    case "3" -> sequencialConcorrenteParalelo();
                    case "4" -> raceCondition();
                    case "5" -> atomicidade();
                    case "6" -> waitNotify();
                    case "7" -> deadlock();
                    case "8" -> broker();
                    case "9" -> memoria();
                    case "10" -> logico();
                    default -> {
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 1 ------------------------------------------------------------------------------------
    static void tradicionalVsEventos() {
        System.out.println("\nTRADICIONAL: o serviço chama e-mail, SMS e ficheiro, um a seguir ao outro, na thread main.");
        long inicio = System.currentTimeMillis();
        enviarEmail();
        enviarSms();
        guardarFicheiro();
        System.out.println("   O utilizador esperou " + (System.currentTimeMillis() - inicio) + " ms (gargalo!)");

        System.out.println("\nEVENTOS SÍNCRONOS (subscrever): os consumidores correm na MESMA thread do produtor.");
        EventBus busSincrono = new EventBus();
        busSincrono.subscrever(TipoEvento.CANDIDATURA_ACEITE, e -> enviarEmail());
        busSincrono.subscrever(TipoEvento.CANDIDATURA_ACEITE, e -> enviarSms());
        busSincrono.subscrever(TipoEvento.CANDIDATURA_ACEITE, e -> guardarFicheiro());
        inicio = System.currentTimeMillis();
        busSincrono.publicar(TipoEvento.CANDIDATURA_ACEITE, "candidatura #1", "Mariana");
        System.out.println("   O utilizador esperou " + (System.currentTimeMillis() - inicio)
                + " ms (já há desacoplamento, mas ainda tudo na thread main)");
        busSincrono.encerrar();

        System.out.println("\nEVENTOS ASSÍNCRONOS (subscreverAssincrono): os consumidores correm noutras threads.");
        EventBus bus = new EventBus();
        bus.subscreverAssincrono(TipoEvento.CANDIDATURA_ACEITE, e -> enviarEmail());
        bus.subscreverAssincrono(TipoEvento.CANDIDATURA_ACEITE, e -> enviarSms());
        bus.subscreverAssincrono(TipoEvento.CANDIDATURA_ACEITE, e -> guardarFicheiro());
        inicio = System.currentTimeMillis();
        bus.publicar(TipoEvento.CANDIDATURA_ACEITE, "candidatura #1", "Mariana");
        System.out.println("   O utilizador esperou " + (System.currentTimeMillis() - inicio) + " ms");
        Util.dormir(600);
        bus.encerrar();
    }

    private static void enviarEmail() {
        Util.dormir(300);
        Util.log("e-mail enviado");
    }

    private static void enviarSms() {
        Util.dormir(300);
        Util.log("SMS enviado");
    }

    private static void guardarFicheiro() {
        Util.dormir(300);
        Util.log("comprovativo guardado no File Server");
    }

    // 2 ------------------------------------------------------------------------------------
    static void runVsStart() throws InterruptedException {
        Thread t1 = new Thread(() -> Util.log("A"), "Thread-1");
        Thread t2 = new Thread(() -> Util.log("B"), "Thread-2");

        System.out.println("\nt1.run() -> NÃO cria thread, corre na própria main:");
        t1.run();
        System.out.println("t1.start() -> cria uma NOVA thread:");
        t1.start();
        t1.join();
        System.out.println("t2 sem start(): estado = " + t2.getState() + " -> o 'B' nunca aparece");

        System.out.println("\nControlar a ordem sem mudar as linhas de lugar:");
        Thread a = new Thread(() -> Util.log("A"), "Thread-1");
        Thread b = new Thread(() -> Util.log("B"), "Thread-2");
        System.out.println("Ordem A depois B (join espera a thread terminar):");
        a.start();
        a.join();
        b.start();
        b.join();
        Thread a2 = new Thread(() -> Util.log("A"), "Thread-1");
        Thread b2 = new Thread(() -> Util.log("B"), "Thread-2");
        System.out.println("Ordem B depois A:");
        b2.start();
        b2.join();
        a2.start();
        a2.join();
    }

    // 3 ------------------------------------------------------------------------------------
    static void sequencialConcorrenteParalelo() throws InterruptedException {
        int nucleos = Runtime.getRuntime().availableProcessors();
        System.out.println("\nEsta máquina tem " + nucleos + " núcleos (CPUs).");

        System.out.println("SEQUENCIAL (há dependência de dados): criar perfil -> validar -> candidatar, um a seguir ao outro.");

        System.out.println("CONCORRENTE: 3 tarefas de rede no MESMO intervalo de tempo (ExecutorService com 3 threads)");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        long inicio = System.currentTimeMillis();
        executor.submit(DemoParadigmas::enviarEmail);
        executor.submit(DemoParadigmas::enviarSms);
        executor.submit(DemoParadigmas::guardarFicheiro);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("   3 x 300 ms terminaram em " + (System.currentTimeMillis() - inicio) + " ms (e não 900 ms)");

        System.out.println("PARALELO: validar 8 documentos (trabalho de CPU) repartido pelos núcleos");
        inicio = System.currentTimeMillis();
        IntStream.range(0, 8).forEach(i -> trabalhoPesado());
        long sequencial = System.currentTimeMillis() - inicio;
        inicio = System.currentTimeMillis();
        IntStream.range(0, 8).parallel().forEach(i -> trabalhoPesado());
        long paralelo = System.currentTimeMillis() - inicio;
        System.out.println("   1 núcleo: " + sequencial + " ms | vários núcleos (parallel): " + paralelo + " ms");
    }

    private static void trabalhoPesado() {
        double x = 0;
        for (int i = 0; i < 20_000_000; i++) {
            x += Math.sqrt(i);
        }
        if (x < 0) {
            System.out.println(x);
        }
    }

    // 4 ------------------------------------------------------------------------------------
    static void raceCondition() throws InterruptedException {
        System.out.println("\nSEM LOCK: Mariana e Joana clicam 'Agendar' na Ana ao mesmo tempo");
        Trabalhadora ana = new Trabalhadora(1, "Ana", "84", "Maputo");
        Thread p1 = new Thread(() -> reservarSemLock(ana, "Mariana"), "pedido-Mariana");
        Thread p2 = new Thread(() -> reservarSemLock(ana, "Joana"), "pedido-Joana");
        p1.start();
        p2.start();
        p1.join();
        p2.join();
        System.out.println("   -> AS DUAS reservaram a mesma trabalhadora (erro!)");

        System.out.println("\nCOM LOCK (AgendamentoService):");
        AgendamentoService agendamento = new AgendamentoService(new java.util.concurrent.CopyOnWriteArrayList<>(),
                new NotificacaoService(new java.util.concurrent.CopyOnWriteArrayList<>()), new EventBus());
        Trabalhadora beatriz = new Trabalhadora(2, "Beatriz", "84", "Matola");
        Contratante mariana = new Contratante(1, "Mariana", "82", "Polana");
        Contratante joana = new Contratante(2, "Joana", "82", "Sommerschield");
        Thread p3 = new Thread(() -> agendamento.agendar(mariana, beatriz), "pedido-Mariana");
        Thread p4 = new Thread(() -> agendamento.agendar(joana, beatriz), "pedido-Joana");
        p3.start();
        p4.start();
        p3.join();
        p4.join();
        System.out.println("   -> Só uma reservou; a outra ficou na fila. Estado: " + beatriz.getEstado());
    }

    private static void reservarSemLock(Trabalhadora t, String contratante) {
        if (t.getEstado() == EstadoTrabalhadora.DISPONIVEL) {   // 1. ler
            Util.dormir(50);                                     //    (tempo da base de dados)
            t.setEstado(EstadoTrabalhadora.EM_REUNIAO);          // 2. escrever
            Util.log(contratante + " reservou a " + t.getNome());
        }
    }

    // 5 ------------------------------------------------------------------------------------
    private static int contadorNormal = 0;

    static void atomicidade() throws InterruptedException {
        contadorNormal = 0;
        AtomicInteger contadorAtomico = new AtomicInteger();
        Thread[] threads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100_000; j++) {
                    contadorNormal++;                 // ler + somar + escrever: NÃO é atómico
                    contadorAtomico.incrementAndGet(); // atómico
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("\n4 threads contaram 100.000 candidaturas cada. Esperado: 400000");
        System.out.println("   int normal:    " + contadorNormal + "   <- perderam-se contagens (race condition)");
        System.out.println("   AtomicInteger: " + contadorAtomico.get());
    }

    // 6 ------------------------------------------------------------------------------------
    static void waitNotify() throws InterruptedException {
        MonitorPagamento monitor = new MonitorPagamento();
        Thread notificacoes = new Thread(() -> {
            try {
                Util.log("à espera (wait) que o pagamento termine...");
                monitor.esperarPagamento();
                Util.log("fui acordada! A enviar a notificação: contrato confirmado.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "thread-notificacoes");
        Thread pagamentos = new Thread(() -> {
            Util.log("a processar o pagamento M-Pesa...");
            Util.dormir(1000);
            Util.log("pagamento confirmado -> notifyAll()");
            monitor.confirmarPagamento();
        }, "thread-pagamentos");
        System.out.println();
        notificacoes.start();
        pagamentos.start();
        notificacoes.join();
        pagamentos.join();
    }

    // 7 ------------------------------------------------------------------------------------
    static void deadlock() throws InterruptedException {
        ReentrantLock trabalhadora = new ReentrantLock();
        ReentrantLock vaga = new ReentrantLock();
        System.out.println("\nThread A tranca a TRABALHADORA e quer a VAGA; Thread B tranca a VAGA e quer a TRABALHADORA.");
        System.out.println("Com lock() ficariam presas para sempre (DEADLOCK). Com tryLock(tempo) desistem e libertam:");
        Thread a = new Thread(() -> duasTrancas(trabalhadora, vaga, "trabalhadora", "vaga"), "Thread-A");
        Thread b = new Thread(() -> duasTrancas(vaga, trabalhadora, "vaga", "trabalhadora"), "Thread-B");
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Solução definitiva: todas as threads trancam pela MESMA ORDEM (primeiro trabalhadora, depois vaga).");
    }

    private static void duasTrancas(ReentrantLock primeiro, ReentrantLock segundo, String n1, String n2) {
        primeiro.lock();
        try {
            Util.log("tranquei " + n1 + ", quero " + n2);
            Util.dormir(100);
            if (segundo.tryLock(500, TimeUnit.MILLISECONDS)) {
                try {
                    Util.log("consegui as duas");
                } finally {
                    segundo.unlock();
                }
            } else {
                Util.log("não consegui " + n2 + " em 500 ms -> desisto e liberto " + n1 + " (evita deadlock)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            primeiro.unlock();
        }
    }

    // 8 ------------------------------------------------------------------------------------
    static void broker() throws InterruptedException {
        Broker broker = new Broker();
        System.out.println("\nO serviço de pagamentos está DESLIGADO. A plataforma deixa 3 pedidos na fila e continua:");
        broker.publicar("pagamentos", new Broker.Mensagem("application/json", "{\"contrato\": 1, \"valor\": 250}"));
        broker.publicar("pagamentos", new Broker.Mensagem("application/json", "{\"contrato\": 2, \"valor\": 250}"));
        broker.publicar("pagamentos", new Broker.Mensagem("application/xml", "<contrato>3</contrato>"));
        System.out.println("   Pedidos à espera na fila: " + broker.pendentes("pagamentos"));
        System.out.println("O serviço de pagamentos LIGA e VEM BUSCAR os pedidos (nada se perdeu):");
        Thread servico = new Thread(() -> {
            try {
                Broker.Mensagem m;
                while ((m = broker.receber("pagamentos", 300)) != null) {
                    if (m.contentType().equals("application/json")) {
                        Util.log("pagamento processado: " + m.corpo());
                    } else {
                        Util.log("rejeitado: formato " + m.contentType() + " (o combinado era application/json)");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "pagamentos");
        servico.start();
        servico.join();
    }

    // 9 ------------------------------------------------------------------------------------
    static void memoria() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        System.out.println("\nMemória Heap usada no início: " + usadaMb(rt) + " MB");
        byte[] relatorio = new byte[40 * 1024 * 1024];   // objecto 1: relatório em PDF
        byte[] fotografia = new byte[30 * 1024 * 1024];  // objecto 2: fotografia
        byte[] documentoBI = new byte[30 * 1024 * 1024]; // objecto 3: BI digitalizado
        System.out.println("Depois de criar 3 objectos pesados: " + usadaMb(rt) + " MB  (veja no VisualVM)");
        System.out.println("   tamanhos: " + (relatorio.length + fotografia.length + documentoBI.length) / (1024 * 1024) + " MB");
        relatorio = null;   // os objectos PERDEM A REFERÊNCIA ("morrem")
        fotografia = null;
        documentoBI = null;
        System.gc();        // pedido ao Garbage Collector
        Util.dormir(300);
        System.out.println("Depois de perderem a referência + GC: " + usadaMb(rt) + " MB");
        System.out.println("Algoritmo do GC: java -XX:+UseG1GC (padrão) | -XX:+UseParallelGC | -XX:+UseSerialGC | -XX:+UseZGC");
    }

    // 10 -----------------------------------------------------------------------------------
    // Dados em memória (sem base de dados): 4 contratantes diferentes, cada uma com reunião com a
    // Rosa (a fila de espera vai promovendo uma de cada vez), denunciam-na.
    static void logico() {
        java.util.List<Trabalhadora> ts = new java.util.concurrent.CopyOnWriteArrayList<>();
        java.util.List<Reuniao> rs = new java.util.concurrent.CopyOnWriteArrayList<>();
        EventBus bus = new EventBus();
        MotorRegras motor = new MotorRegras();
        AgendamentoService agendamento = new AgendamentoService(rs,
                new NotificacaoService(new java.util.concurrent.CopyOnWriteArrayList<>()), bus);
        ModeracaoService moderacao = new ModeracaoService(ts, rs, bus, motor);
        bus.subscrever(TipoEvento.DENUNCIA_REGISTADA, new MotorLogicoConsumer(motor));

        Trabalhadora rosa = new Trabalhadora(1, "Rosa", "84", "Gaza");
        rosa.setDocumentacaoValidada(true);
        ts.add(rosa);
        System.out.println("\nRegras: " + "R2 = SE denúncias de mais de 3 contratantes DIFERENTES ENTÃO bloquearContratacao");
        System.out.println("4 contratantes marcam reunião com a Rosa (1 reserva, 3 ficam na fila; a Rosa recusa e a fila avança):");
        java.util.List<Contratante> cs = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Contratante c = new Contratante(i, "Contratante" + i, "82", "Maputo");
            cs.add(c);
            agendamento.agendar(c, rosa);
        }
        for (int i = 0; i < 3; i++) {
            Reuniao pendente = rs.stream().filter(Reuniao::isActiva).findFirst().orElseThrow();
            agendamento.responderConvite(pendente, false);
        }
        System.out.println("\nA Contratante1 tenta denunciar 2 vezes (só a 1ª conta):");
        moderacao.denunciar(cs.get(0), rosa, "atraso");
        moderacao.denunciar(cs.get(0), rosa, "outra vez");
        System.out.println("   Contratantes diferentes que denunciaram: " + rosa.contarDenunciantesDistintos()
                + " -> bloqueada? " + motor.deduz(MotorRegras.BLOQUEAR_CONTRATACAO, rosa));
        System.out.println("\nAs outras 3 contratantes também denunciam:");
        for (int i = 1; i < 4; i++) {
            moderacao.denunciar(cs.get(i), rosa, "motivo " + i);
        }
        System.out.println("   Bloqueada? " + motor.deduz(MotorRegras.BLOQUEAR_CONTRATACAO, rosa)
                + " | aparece nas pesquisas? " + moderacao.perfilValido(rosa));
        bus.aguardar();
        bus.encerrar();
    }

    private static long usadaMb(Runtime rt) {
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }
}
