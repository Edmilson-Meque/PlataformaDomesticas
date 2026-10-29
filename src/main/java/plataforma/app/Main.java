package plataforma.app;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import plataforma.broker.Broker;
import plataforma.broker.ServicoPagamentos;
import plataforma.concorrencia.ActorTrabalhadora;
import plataforma.concorrencia.TarefaManutencao;
import plataforma.dados.BaseDados;
import plataforma.dados.DadosPlataforma;
import plataforma.demo.DemoParadigmas;
import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.event.consumidores.AgendamentoAutomaticoConsumer;
import plataforma.event.consumidores.AuditoriaConsumer;
import plataforma.event.consumidores.EncerramentoContratoConsumer;
import plataforma.event.consumidores.MotorLogicoConsumer;
import plataforma.event.consumidores.NotificacaoConsumer;
import plataforma.event.consumidores.PedidoPagamentoConsumer;
import plataforma.event.consumidores.SincronizacaoConsumer;
import plataforma.event.consumidores.SmsConsumer;
import plataforma.logica.MotorRegras;
import plataforma.model.Administrador;
import plataforma.model.Candidatura;
import plataforma.model.Contratante;
import plataforma.model.Contrato;
import plataforma.model.Denuncia;
import plataforma.model.EstadoReuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Notificacao;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.model.Utilizador;
import plataforma.model.Vaga;
import plataforma.offline.Telemovel;
import plataforma.service.AgendamentoService;
import plataforma.service.ContratacaoService;
import plataforma.service.ModeracaoService;
import plataforma.service.NotificacaoService;
import plataforma.service.VagaCandidaturaService;
import plataforma.util.Util;

// NOTA: a ideia deste trabalho é só mostrar uma IMPLEMENTAÇÃO BÁSICA dos paradigmas
// (Orientado a Eventos, Concorrente/Paralelo, Actores, Lógico, State e Offline-First),
// sem ir muito a fundo, para se ver cada paradigma a funcionar dentro da plataforma.
public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    // Dados em memória (listas THREAD-SAFE), guardados no MySQL depois de cada operação
    private static final DadosPlataforma dados = new DadosPlataforma();
    private static final List<Trabalhadora> trabalhadoras = dados.trabalhadoras;
    private static final List<Contratante> contratantes = dados.contratantes;
    private static final List<Reuniao> reunioes = dados.reunioes;
    private static final List<Contrato> contratos = dados.contratos;
    private static final List<Notificacao> notificacoes = dados.notificacoes;
    private static final List<Vaga> vagas = dados.vagas;
    private static final List<Candidatura> candidaturas = dados.candidaturas;
    private static final Administrador admin = dados.admin;

    // Barramento de Eventos e Serviços
    private static EventBus eventBus;
    private static NotificacaoService notificacaoService;
    private static ModeracaoService moderacaoService;
    private static AgendamentoService agendamentoService;
    private static ContratacaoService contratacaoService;
    private static VagaCandidaturaService vagaCandidaturaService;

    // Novos componentes dos paradigmas
    private static MotorRegras motorRegras;
    private static Broker broker;
    private static ServicoPagamentos servicoPagamentos;
    private static BaseDados bd;
    private static AuditoriaConsumer auditoria;
    private static TarefaManutencao tarefas;
    private static final Map<Trabalhadora, ActorTrabalhadora> actores = new ConcurrentHashMap<>();
    private static final Map<Trabalhadora, Telemovel> telemoveis = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // ===== 1. PRODUTORES: os serviços (só fazem a sua acção e PUBLICAM eventos no EventBus) =====
        eventBus = new EventBus();
        motorRegras = new MotorRegras();
        notificacaoService = new NotificacaoService(notificacoes);
        moderacaoService = new ModeracaoService(trabalhadoras, reunioes, eventBus, motorRegras);
        agendamentoService = new AgendamentoService(reunioes, notificacaoService, eventBus);
        contratacaoService = new ContratacaoService(contratos, eventBus, agendamentoService, motorRegras);
        vagaCandidaturaService = new VagaCandidaturaService(vagas, candidaturas, eventBus, motorRegras);
        broker = new Broker();

        // BASE DE DADOS (MySQL via JDBC): carrega tudo o que já estava guardado
        bd = new BaseDados();
        bd.carregarTudo(dados, agendamentoService);

        // ===== 2. CONSUMIDORES: registados no EventBus (só o EventBus os conhece) =====
        // SÍNCRONOS (mesma thread: há dependência de dados)
        eventBus.subscrever(TipoEvento.CANDIDATURA_ACEITE, new AgendamentoAutomaticoConsumer(agendamentoService));
        eventBus.subscrever(TipoEvento.DENUNCIA_REGISTADA, new MotorLogicoConsumer(motorRegras));
        eventBus.subscrever(TipoEvento.CONTRATO_CRIADO,
                new EncerramentoContratoConsumer(agendamentoService, vagaCandidaturaService));
        // ASSÍNCRONOS (threads do pool: o utilizador não fica à espera)
        NotificacaoConsumer notificacao = new NotificacaoConsumer(notificacaoService);
        for (TipoEvento tipo : TipoEvento.values()) {
            eventBus.subscreverAssincrono(tipo, notificacao);
        }
        eventBus.subscreverAssincrono(TipoEvento.CANDIDATURA_ACEITE, new SmsConsumer());
        eventBus.subscreverAssincrono(TipoEvento.REUNIAO_AGENDADA, new SmsConsumer());
        eventBus.subscreverAssincrono(TipoEvento.CONTRATO_CRIADO, new PedidoPagamentoConsumer(broker));
        eventBus.subscreverAssincrono(TipoEvento.REDE_RESTABELECIDA, new SincronizacaoConsumer());

        // ===== 3. AUDITORIA: acrescentada SÓ AQUI, sem mudar nenhuma regra de negócio =====
        auditoria = new AuditoriaConsumer(bd);
        for (TipoEvento tipo : TipoEvento.values()) {
            eventBus.subscreverAssincrono(tipo, auditoria);
        }

        // ===== 4. THREADS DE FUNDO: serviço de pagamentos (lê do broker) e Task agendada =====
        servicoPagamentos = new ServicoPagamentos(broker, contratos, eventBus, bd);
        servicoPagamentos.ligar();
        tarefas = new TarefaManutencao(bd, trabalhadoras, agendamentoService);
        tarefas.agendar(LocalTime.of(2, 0));

        menuPrincipal();

        // ===== 5. ENCERRAR: guardar tudo, parar as threads; os objectos perdem a referência e o GC limpa-os =====
        persistir();
        eventBus.encerrar();
        System.out.println("Até à próxima!");
    }

    // Espera pelos consumidores assíncronos e grava tudo na base de dados (numa transacção)
    private static void persistir() {
        eventBus.aguardar();
        bd.guardarTudo(dados, agendamentoService);
    }

    // Cada trabalhadora tem um ACTOR e um TELEMÓVEL (criados na primeira vez)
    private static ActorTrabalhadora actorDe(Trabalhadora t) {
        return actores.computeIfAbsent(t, k -> new ActorTrabalhadora(k, agendamentoService));
    }

    private static Telemovel telemovelDe(Trabalhadora t) {
        return telemoveis.computeIfAbsent(t, k -> new Telemovel(k, actorDe(k), notificacaoService, eventBus));
    }

    // MENU PRINCIPAL
    private static void menuPrincipal() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n=========================================");
            System.out.println("  PLATAFORMA DE CONTRATAÇÃO DOMÉSTICA");
            System.out.println("=========================================");
            System.out.println("1 - Entrar como Administrador");
            System.out.println("2 - Entrar como Contratante");
            System.out.println("3 - Entrar como Trabalhadora");
            System.out.println("4 - Criar conta de Contratante");
            System.out.println("5 - Criar conta de Trabalhadora");
            System.out.println("6 - Demonstrações dos paradigmas (threads, locks, broker, memória...)");
            System.out.println("0 - Sair");
            opcao = lerInt("Opção: ");

            switch (opcao) {
                case 1:
                    menuAdministrador();
                    break;
                case 2:
                    Contratante c = contratantes.isEmpty()
                            ? (oferecerCriarConta("contratante") ? criarContratante() : null)
                            : escolherContratante();
                    if (c != null) {
                        menuContratante(c);
                    }
                    break;
                case 3:
                    Trabalhadora t = trabalhadoras.isEmpty()
                            ? (oferecerCriarConta("trabalhadora") ? criarTrabalhadora() : null)
                            : escolherTrabalhadora("Entrar como:", trabalhadoras);
                    if (t != null) {
                        menuTrabalhadora(t);
                    }
                    break;
                case 4:
                    criarContratante();
                    break;
                case 5:
                    criarTrabalhadora();
                    break;
                case 6:
                    DemoParadigmas.menu(scanner);
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
            persistir();
        }
    }

    // MENU DO ADMINISTRADOR
    private static void menuAdministrador() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- ADMINISTRADOR: " + admin.getNome() + " ---");
            System.out.println("1 - Ver todas as trabalhadoras");
            System.out.println("2 - Ver perfis ocultos");
            System.out.println("3 - Validar documentação de uma trabalhadora");
            System.out.println("4 - Rever denúncias activas (arquivar)");
            System.out.println("5 - Ver contratos realizados");
            System.out.println("6 - Ver registo de auditoria (quem fez o quê e quando)");
            System.out.println("7 - Manutenção: normalizar números de BI e expirar reuniões paradas");
            System.out.println("8 - Pagamentos: ligar / desligar o serviço que processa os pagamentos");
            System.out.println("0 - Voltar");
            opcao = lerInt("Opção: ");

            switch (opcao) {
                case 1:
                    verTodasTrabalhadoras();
                    break;
                case 2:
                    verPerfisOcultos();
                    break;
                case 3:
                    validarDocumentacao();
                    break;
                case 4:
                    reverDenuncias();
                    break;
                case 5:
                    verContratos();
                    break;
                case 6:
                    verAuditoria();
                    break;
                case 7:
                    System.out.println("\nA manutenção corre sozinha todos os dias às 02:00 (Task agendada). Aqui pode corrê-la já:");
                    System.out.println("  Task 1: tira espaços/traços dos números de BI directamente na base de dados (SQL).");
                    System.out.println("  Task 2: reuniões pendentes/aceites há demasiados dias sem contrato passam a EXPIRADA,");
                    System.out.println("          e a trabalhadora fica livre (ou a próxima da fila recebe o convite).");
                    String dias = lerTexto("Expirar reuniões com mais de quantos dias? (Enter = "
                            + TarefaManutencao.DIAS_PARA_EXPIRAR + ", 0 = todas as activas): ");
                    try {
                        tarefas.executar(dias.isEmpty() ? TarefaManutencao.DIAS_PARA_EXPIRAR : Integer.parseInt(dias));
                    } catch (NumberFormatException e) {
                        System.out.println("Número de dias inválido.");
                    }
                    break;
                case 8:
                    System.out.println("\nQuando há um contrato, o pedido de pagamento fica numa FILA (Broker).");
                    System.out.println("O serviço de pagamentos vai buscar os pedidos à fila e confirma-os.");
                    System.out.println("Se o desligar, os pedidos ficam à espera na fila (não se perdem) até o voltar a ligar.");
                    System.out.println("Estado: " + (servicoPagamentos.isActivo() ? "LIGADO" : "DESLIGADO")
                            + " | pedidos à espera na fila: " + broker.pendentes(ServicoPagamentos.TOPICO));
                    int op = lerInt("1 - Desligar | 2 - Ligar | 0 - Voltar: ");
                    if (op == 1) {
                        servicoPagamentos.desligar();
                    } else if (op == 2) {
                        servicoPagamentos.ligar();
                    }
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
            persistir();
        }
    }

    private static void verTodasTrabalhadoras() {
        System.out.println("\nTodas as trabalhadoras (ordenadas por nome):");
        if (trabalhadoras.isEmpty()) {
            System.out.println("  (nenhuma)");
            return;
        }
        List<Trabalhadora> visiveis = moderacaoService.listarVisiveis();
        // Funcional: sorted + map + forEach
        trabalhadoras.stream()
                .sorted(Comparator.comparing(Trabalhadora::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(t -> "  - " + t.getNome() + " | " + t.getEstado()
                        + " | documentação " + (t.isDocumentacaoValidada() ? "validada" : "por validar")
                        + " | BI " + t.getBi()
                        + " | denúncias activas: " + t.contarDenunciasActivas()
                        + " | " + (visiveis.contains(t) ? "visível" : "não aparece na pesquisa"))
                .forEach(System.out::println);
    }

    private static void verPerfisOcultos() {
        List<Trabalhadora> ocultos = moderacaoService.listarOcultos();
        System.out.println("\nPerfis ocultos (a rever):");
        if (ocultos.isEmpty()) {
            System.out.println("  (nenhum)");
            return;
        }
        ocultos.stream()
                .map(t -> "  - " + t.getNome() + ": " + (!t.isDocumentacaoValidada()
                        ? "documentação por validar"
                        : "denúncias de " + t.contarDenunciantesDistintos() + " contratantes diferentes (aguarda revisão)"))
                .forEach(System.out::println);
    }

    private static void validarDocumentacao() {
        // Funcional: filter
        List<Trabalhadora> porValidar = trabalhadoras.stream()
                .filter(t -> !t.isDocumentacaoValidada())
                .collect(Collectors.toList());

        if (porValidar.isEmpty()) {
            System.out.println("Não há documentação por validar.");
            return;
        }
        Trabalhadora t = escolherTrabalhadora("Validar documentação de:", porValidar);
        if (t != null) {
            moderacaoService.validarDocumentacao(t);
            System.out.println("Documentação de " + t.getNome() + " validada.");
        }
    }

    private static void reverDenuncias() {
        // Funcional: flatMap + filter
        List<Denuncia> activas = trabalhadoras.stream()
                .flatMap(t -> t.getDenuncias().stream())
                .filter(Denuncia::isActiva)
                .collect(Collectors.toList());

        if (activas.isEmpty()) {
            System.out.println("Não há denúncias activas.");
            return;
        }

        System.out.println("\nDenúncias activas:");
        for (int i = 0; i < activas.size(); i++) {
            Denuncia d = activas.get(i);
            System.out.println("  " + (i + 1) + ") " + d.getAutor().getNome() + " contra "
                    + d.getAlvo().getNome() + ": " + d.getMotivo());
        }
        int escolha = lerInt("Número da denúncia a arquivar (0 para voltar): ");
        if (escolha >= 1 && escolha <= activas.size()) {
            Denuncia d = activas.get(escolha - 1);
            moderacaoService.arquivarDenuncia(d);
            System.out.println("Denúncia arquivada. " + d.getAlvo().getNome()
                    + " tem agora " + d.getAlvo().contarDenunciasActivas() + " denúncias activas.");
        }
    }

    // Os contratos são lidos da BASE DE DADOS (prova de que ficaram guardados)
    private static void verContratos() {
        persistir();
        List<String[]> linhas = bd.consultar("SELECT c.id, ct.nome, t.nome, c.data_contratacao,"
                + " CASE WHEN c.pago THEN 'PAGO' ELSE 'PENDENTE DE PAGAMENTO' END"
                + " FROM contrato c JOIN contratante ct ON ct.id = c.contratante_id"
                + " JOIN trabalhadora t ON t.id = c.trabalhadora_id ORDER BY c.id");
        System.out.println("\nContratos realizados:");
        if (linhas.isEmpty()) {
            System.out.println("  (ainda não há contratos)");
            return;
        }
        tabela(new String[]{"Nº", "Contratante", "Trabalhadora", "Data", "Pagamento"}, linhas);
    }

    private static void verAuditoria() {
        persistir();
        List<String[]> linhas = bd.consultar("SELECT hora, utilizador, accao, categoria, ip FROM auditoria"
                + " ORDER BY id DESC LIMIT 20");
        System.out.println("\nRegisto de auditoria (últimas 20 acções, mais recentes primeiro):");
        if (linhas.isEmpty()) {
            System.out.println("  (vazio)");
            return;
        }
        linhas.forEach(l -> l[0] = l[0].length() > 19 ? l[0].substring(0, 19) : l[0]);
        tabela(new String[]{"Data e hora", "Utilizador", "Acção", "Tipo", "IP"}, linhas);
    }

    // Imprime uma tabela simples na consola
    private static void tabela(String[] cabecalho, List<String[]> linhas) {
        int[] largura = new int[cabecalho.length];
        for (int i = 0; i < cabecalho.length; i++) {
            largura[i] = cabecalho[i].length();
            for (String[] l : linhas) {
                largura[i] = Math.max(largura[i], String.valueOf(l[i]).length());
            }
        }
        StringBuilder formato = new StringBuilder("  ");
        for (int w : largura) {
            formato.append("%-").append(w + 2).append("s");
        }
        System.out.println(String.format(formato.toString(), (Object[]) cabecalho));
        for (String[] l : linhas) {
            System.out.println(String.format(formato.toString(), (Object[]) l));
        }
    }

    // MENU DO CONTRATANTE
    private static void menuContratante(Contratante contratante) {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- CONTRATANTE: " + contratante.getNome() + " ---");
            System.out.println("1 - Pesquisar trabalhadoras disponíveis (com filtro/ordenação)");
            System.out.println("2 - Agendar reunião directa");
            System.out.println("3 - As minhas reuniões");
            System.out.println("4 - Confirmar contratação");
            System.out.println("5 - Denunciar uma trabalhadora");
            System.out.println("6 - Ver notificações" + novas(contratante));
            System.out.println("7 - Publicar nova vaga de serviço doméstico");
            System.out.println("8 - Avaliar candidaturas às minhas vagas (Dispara CANDIDATURA_ACEITE)");
            System.out.println("9 - Cancelar uma reunião");
            System.out.println("0 - Voltar");
            opcao = lerInt("Opção: ");

            switch (opcao) {
                case 1:
                    pesquisarTrabalhadoras();
                    break;
                case 2:
                    agendarReuniao(contratante);
                    break;
                case 3:
                    verReunioesDoContratante(contratante);
                    break;
                case 4:
                    confirmarContratacao(contratante);
                    break;
                case 5:
                    denunciar(contratante);
                    break;
                case 6:
                    verNotificacoes(contratante);
                    break;
                case 7:
                    publicarVaga(contratante);
                    break;
                case 8:
                    gerirCandidaturasContratante(contratante);
                    break;
                case 9:
                    cancelarReuniao(contratante);
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
            persistir();
        }
    }

    private static void pesquisarTrabalhadoras() {
        System.out.print("Filtrar por província/competência (Enter para ver todas): ");
        String termo = scanner.nextLine().trim();

        List<Trabalhadora> resultado = termo.isEmpty()
                ? moderacaoService.listarVisiveis()
                : moderacaoService.pesquisarVisiveisPorFiltro(termo);

        System.out.println("\nTrabalhadoras disponíveis:");
        if (resultado.isEmpty()) {
            System.out.println("  (nenhuma encontrada)");
            return;
        }
        // Funcional: map + forEach
        resultado.stream()
                .map(t -> "  - " + descrever(t))
                .forEach(System.out::println);
    }

    private static void publicarVaga(Contratante contratante) {
        System.out.print("Título da vaga (ex: Cuidadora Infantil / Limpeza Geral): ");
        String titulo = scanner.nextLine().trim();
        System.out.print("Província: ");
        String provincia = scanner.nextLine().trim();
        System.out.print("Competência requerida: ");
        String competencia = scanner.nextLine().trim();
        double salario = lerDouble("Salário mensal oferecido (MT): ");

        if (titulo.isEmpty()) {
            System.out.println("Publicação cancelada (título vazio).");
            return;
        }
        vagaCandidaturaService.publicarVaga(contratante, titulo, provincia, competencia, salario);
    }

    private static void gerirCandidaturasContratante(Contratante contratante) {
        List<Candidatura> pendentes = vagaCandidaturaService.listarCandidaturasPendentesDoContratante(contratante);
        if (pendentes.isEmpty()) {
            System.out.println("Não tem candidaturas pendentes nas suas vagas.");
            return;
        }
        System.out.println("\nCandidaturas pendentes:");
        for (int i = 0; i < pendentes.size(); i++) {
            Candidatura c = pendentes.get(i);
            System.out.println("  " + (i + 1) + ") " + c.getTrabalhadora().getNome()
                    + " (" + c.getTrabalhadora().getCompetencias() + ") -> Vaga: "
                    + c.getVaga().getTitulo() + " [" + c.getVaga().getSalarioOferecido() + " MT]");
        }
        int escolha = lerInt("Escolha a candidatura a avaliar (0 para voltar): ");
        if (escolha >= 1 && escolha <= pendentes.size()) {
            Candidatura selecionada = pendentes.get(escolha - 1);
            int acao = lerInt("1 - Aceitar Candidatura (emite CANDIDATURA_ACEITE) | 2 - Recusar | 0 - Cancelar: ");
            if (acao == 1) {
                vagaCandidaturaService.avaliarCandidatura(selecionada, true);
            } else if (acao == 2) {
                vagaCandidaturaService.avaliarCandidatura(selecionada, false);
            }
        }
    }

    private static void agendarReuniao(Contratante contratante) {
        // Mostra também as reservadas: quem escolher uma reservada fica na FILA DE ESPERA
        List<Trabalhadora> visiveis = trabalhadoras.stream()
                .filter(moderacaoService::perfilValido)
                .filter(t -> t.getEstado() != EstadoTrabalhadora.CONTRATADA)
                .collect(Collectors.toList());
        visiveis.forEach(t -> System.out.println("   " + t.getNome() + " -> " + t.getEstado()));
        if (visiveis.isEmpty()) {
            System.out.println("Não há trabalhadoras disponíveis de momento.");
            return;
        }
        Trabalhadora t = escolherTrabalhadora("Agendar reunião com:", visiveis);
        if (t != null) {
            agendamentoService.agendar(contratante, t);
        }
    }

    private static void verReunioesDoContratante(Contratante contratante) {
        System.out.println("\nAs minhas reuniões:");
        List<Reuniao> minhas = reunioes.stream()
                .filter(r -> r.getContratante() == contratante)
                .sorted(Comparator.comparing(Reuniao::getCriadaEm).reversed())
                .collect(Collectors.toList());

        if (minhas.isEmpty()) {
            System.out.println("  (nenhuma)");
            return;
        }
        minhas.forEach(r -> System.out.println("  - " + r));
    }

    private static void confirmarContratacao(Contratante contratante) {
        // Funcional: filter
        List<Reuniao> aceites = reunioes.stream()
                .filter(r -> r.getContratante() == contratante)
                .filter(r -> r.getEstado() == EstadoReuniao.ACEITE)
                .filter(r -> r.getTrabalhadora().getEstado() != EstadoTrabalhadora.CONTRATADA)
                .collect(Collectors.toList());

        if (aceites.isEmpty()) {
            System.out.println("Não tem reuniões aceites para confirmar.");
            return;
        }
        Reuniao r = escolherReuniao("Contratar após a reunião:", aceites);
        if (r != null) {
            contratacaoService.contratar(r);
            eventBus.aguardar(); // dá tempo às threads de pagamento e notificação
            persistir();
        }
    }

    private static void cancelarReuniao(Contratante contratante) {
        List<Reuniao> activas = reunioes.stream()
                .filter(r -> r.getContratante() == contratante && r.isActiva())
                .collect(Collectors.toList());
        if (activas.isEmpty()) {
            System.out.println("Não tem reuniões activas.");
            return;
        }
        Reuniao r = escolherReuniao("Cancelar a reunião:", activas);
        if (r != null && agendamentoService.cancelarReuniao(r)) {
            System.out.println("Reunião " + r.getId() + " cancelada.");
        }
    }

    private static void denunciar(Contratante contratante) {
        // Só aparecem as trabalhadoras que se podem denunciar: perfil público, já houve reunião
        // com esta contratante e ela ainda não as denunciou
        List<Trabalhadora> possiveis = trabalhadoras.stream()
                .filter(t -> moderacaoService.podeDenunciar(contratante, t))
                .collect(Collectors.toList());
        if (possiveis.isEmpty()) {
            System.out.println("Só pode denunciar trabalhadoras com perfil público com quem já teve reunião ou contrato"
                    + " (e cada uma só uma vez).");
            return;
        }
        Trabalhadora t = escolherTrabalhadora("Denunciar:", possiveis);
        if (t == null) {
            return;
        }
        System.out.print("Motivo da denúncia: ");
        String motivo = scanner.nextLine().trim();
        if (motivo.isEmpty()) {
            System.out.println("Denúncia cancelada (motivo vazio).");
            return;
        }
        if (moderacaoService.denunciar(contratante, t, motivo) != null) {
            System.out.println("Denúncia registada. O administrador irá rever.");
        }
    }

    // MENU DA TRABALHADORA
    private static void menuTrabalhadora(Trabalhadora trabalhadora) {
        if (telemovelDe(trabalhadora).isOnline()) {
            telemovelDe(trabalhadora).sincronizar(); // com internet: guarda as notificações no telemóvel
        }
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- TRABALHADORA: " + trabalhadora.getNome() + " ---");
            System.out.println("1 - Ver o meu perfil");
            System.out.println("2 - Actualizar as minhas competências");
            System.out.println("3 - Ver convites pendentes e responder");
            System.out.println("4 - Ver notificações" + novas(trabalhadora));
            System.out.println("5 - Explorar vagas abertas (filter, map, sort) e candidatar-me");
            System.out.println("6 - Ver estado das minhas candidaturas");
            System.out.println("7 - Internet do telemóvel: ligar / desligar (" + (telemovelDe(trabalhadora).isOnline() ? "ONLINE" : "OFFLINE") + ")");
            System.out.println("0 - Voltar");
            opcao = lerInt("Opção: ");

            switch (opcao) {
                case 1:
                    verPerfil(trabalhadora);
                    break;
                case 2:
                    System.out.print("Novas competências: ");
                    trabalhadora.setCompetencias(scanner.nextLine().trim());
                    System.out.println("Competências actualizadas.");
                    break;
                case 3:
                    responderConvites(trabalhadora);
                    break;
                case 4:
                    System.out.println("\nNotificações:");
                    List<String> lista = telemovelDe(trabalhadora).verNotificacoes();
                    if (lista.isEmpty()) {
                        System.out.println("  (nenhuma)");
                    }
                    lista.forEach(n -> System.out.println("  " + n));
                    break;
                case 5:
                    explorarECandidatarVagas(trabalhadora);
                    break;
                case 6:
                    verMinhasCandidaturas(trabalhadora);
                    break;
                case 7:
                    Telemovel tel = telemovelDe(trabalhadora);
                    if (tel.isOnline()) {
                        tel.desligarInternet();
                        System.out.println("Internet DESLIGADA. As acções ficam guardadas no telemóvel.");
                    } else {
                        tel.ligarInternet(); // publica o evento de SYSTEM REDE_RESTABELECIDA
                    }
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
            persistir();
        }
    }

    private static void explorarECandidatarVagas(Trabalhadora trabalhadora) {
        System.out.print("Filtrar vagas por província ou competência (Enter para todas por maior salário): ");
        String termo = scanner.nextLine().trim().toLowerCase();

        List<Vaga> vagasDisponiveis = vagaCandidaturaService.filtrarVagas(
                v -> termo.isEmpty()
                        || v.getProvincia().toLowerCase().contains(termo)
                        || v.getCompetenciaRequerida().toLowerCase().contains(termo)
                        || v.getTitulo().toLowerCase().contains(termo),
                Comparator.comparingDouble(Vaga::getSalarioOferecido).reversed()
        );

        if (vagasDisponiveis.isEmpty()) {
            System.out.println("Não há vagas abertas para esse critério.");
            return;
        }

        System.out.println("\nVagas disponíveis (ordenadas por salário decrescente):");
        for (int i = 0; i < vagasDisponiveis.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + vagasDisponiveis.get(i));
        }

        int escolha = lerInt("Número da vaga para se candidatar (0 para voltar): ");
        if (escolha >= 1 && escolha <= vagasDisponiveis.size()) {
            Vaga escolhida = vagasDisponiveis.get(escolha - 1);
            vagaCandidaturaService.candidatar(trabalhadora, escolhida);
        }
    }

    private static void verMinhasCandidaturas(Trabalhadora trabalhadora) {
        List<Candidatura> minhas = vagaCandidaturaService.listarCandidaturasDaTrabalhadora(trabalhadora);
        System.out.println("\nAs minhas candidaturas:");
        if (minhas.isEmpty()) {
            System.out.println("  (nenhuma)");
            return;
        }
        minhas.stream()
                .map(c -> "  - " + c + "  <- objecto de estado: " + c.getClasseEstado())
                .forEach(System.out::println);
    }

    private static void verPerfil(Trabalhadora t) {
        System.out.println("\nNome: " + t.getNome());
        System.out.println("Província: " + t.getProvincia());
        System.out.println("Competências: " + (t.getCompetencias().isEmpty() ? "(por preencher)" : t.getCompetencias()));
        System.out.println("BI: " + t.getBi());
        System.out.println("Estado: " + t.getEstado());
        System.out.println("Documentação: " + (t.isDocumentacaoValidada() ? "validada" : "por validar"));
        System.out.println("Denúncias activas: " + t.contarDenunciasActivas());
        System.out.println("Perfil: " + (moderacaoService.perfilValido(t) ? "válido" : "oculto das pesquisas"));
    }

    private static void responderConvites(Trabalhadora trabalhadora) {
        // Funcional: filter
        List<Reuniao> pendentes = reunioes.stream()
                .filter(r -> r.getTrabalhadora() == trabalhadora && r.getEstado() == EstadoReuniao.PENDENTE)
                .collect(Collectors.toList());

        if (pendentes.isEmpty()) {
            System.out.println("Não tem convites pendentes.");
            return;
        }
        Reuniao r = escolherReuniao("Convite a responder:", pendentes);
        if (r == null) {
            return;
        }
        int resposta = lerInt("1 - Aceitar | 2 - Recusar | 0 - Cancelar: ");
        // OFFLINE-FIRST: a resposta é guardada no telemóvel; se houver internet é enviada ao
        // ACTOR da trabalhadora, que a processa na sua própria thread (uma mensagem de cada vez)
        if (resposta == 1 || resposta == 2) {
            telemovelDe(trabalhadora).responderConvite(r, resposta == 1);
        }
    }

    // CRIAÇÃO DE CONTAS
    private static Contratante criarContratante() {
        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();
        System.out.print("Contacto: ");
        String contacto = scanner.nextLine().trim();
        System.out.print("Morada/Bairro: ");
        String morada = scanner.nextLine().trim();
        if (nome.isEmpty()) {
            System.out.println("Conta não criada (nome vazio).");
            return null;
        }
        Contratante c = new Contratante(Util.proximoId(contratantes, Contratante::getId), nome, contacto, morada);
        contratantes.add(c);
        System.out.println("Conta criada: " + c);
        return c;
    }

    private static Trabalhadora criarTrabalhadora() {
        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();
        System.out.print("Contacto: ");
        String contacto = scanner.nextLine().trim();
        System.out.print("Província de origem: ");
        String provincia = scanner.nextLine().trim();
        System.out.print("Competências: ");
        String competencias = scanner.nextLine().trim();
        String bi;
        while (true) {
            System.out.print("Número do BI (12 algarismos e 1 letra, ex.: 110100123456A): ");
            bi = scanner.nextLine().trim();
            if (bi.replaceAll("[ -]", "").matches("[0-9]{12}[A-Za-z]")) {
                break;
            }
            System.out.println("BI inválido. Deve ter 12 algarismos seguidos de 1 letra.");
        }
        if (nome.isEmpty()) {
            System.out.println("Conta não criada (nome vazio).");
            return null;
        }
        String biFinal = bi;
        if (trabalhadoras.stream().anyMatch(x -> x.getBi().replaceAll("[ -]", "")
                .equalsIgnoreCase(biFinal.replaceAll("[ -]", "")))) {
            System.out.println("Já existe uma trabalhadora com esse BI. Conta não criada.");
            return null;
        }
        Trabalhadora t = new Trabalhadora(Util.proximoId(trabalhadoras, Trabalhadora::getId), nome, contacto, provincia);
        t.setCompetencias(competencias);
        t.setBi(bi);
        trabalhadoras.add(t);
        System.out.println("Conta criada: " + t);
        System.out.println("O perfil só aparece nas pesquisas depois de o administrador validar a documentação.");
        return t;
    }

    // NOTIFICAÇÕES
    private static String novas(Utilizador u) {
        long n = notificacaoService.contarNaoLidas(u);
        return n == 0 ? "" : " (" + n + " nova" + (n > 1 ? "s" : "") + ")";
    }

    private static void verNotificacoes(Utilizador utilizador) {
        List<Notificacao> lista = notificacaoService.listarDe(utilizador);
        System.out.println("\nNotificações:");
        if (lista.isEmpty()) {
            System.out.println("  (nenhuma)");
            return;
        }
        lista.forEach(n -> {
            System.out.println("  " + (n.isLida() ? "   " : "[NOVA] ") + n.getMensagem());
            n.marcarComoLida();
        });
    }

    // FUNÇÕES DE APOIO
    private static boolean oferecerCriarConta(String tipo) {
        System.out.println("\nAinda não existe nenhuma conta de " + tipo + ". É preciso criar uma primeiro.");
        return lerInt("Criar conta agora? (1 - Sim | 0 - Voltar ao menu): ") == 1;
    }

    private static String descrever(Trabalhadora t) {
        String competencias = t.getCompetencias().isEmpty() ? "sem competências indicadas" : t.getCompetencias();
        return t.getNome() + " | " + t.getProvincia() + " | " + competencias;
    }

    private static Contratante escolherContratante() {
        if (contratantes.isEmpty()) {
            System.out.println("Não há contratantes. Crie uma conta primeiro.");
            return null;
        }
        System.out.println("\nEntrar como:");
        for (int i = 0; i < contratantes.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + contratantes.get(i).getNome());
        }
        int escolha = lerInt("Número (0 para voltar): ");
        if (escolha >= 1 && escolha <= contratantes.size()) {
            return contratantes.get(escolha - 1);
        }
        return null;
    }

    private static Trabalhadora escolherTrabalhadora(String titulo, List<Trabalhadora> lista) {
        if (lista.isEmpty()) {
            System.out.println("Não há trabalhadoras registadas.");
            return null;
        }
        System.out.println("\n" + titulo);
        for (int i = 0; i < lista.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + lista.get(i).getNome()
                    + " (" + lista.get(i).getProvincia() + ")");
        }
        int escolha = lerInt("Número (0 para voltar): ");
        if (escolha >= 1 && escolha <= lista.size()) {
            return lista.get(escolha - 1);
        }
        return null;
    }

    private static Reuniao escolherReuniao(String titulo, List<Reuniao> lista) {
        System.out.println("\n" + titulo);
        for (int i = 0; i < lista.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + lista.get(i));
        }
        int escolha = lerInt("Número (0 para voltar): ");
        if (escolha >= 1 && escolha <= lista.size()) {
            return lista.get(escolha - 1);
        }
        return null;
    }

    private static String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    private static int lerInt(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            if (!scanner.hasNextLine()) {
                return 0;
            }
            String texto = scanner.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escreva um número válido.");
            }
        }
    }

    private static double lerDouble(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String texto = scanner.nextLine().trim();
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escreva um valor numérico válido.");
            }
        }
    }
}