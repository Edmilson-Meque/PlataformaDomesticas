package plataforma.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import plataforma.model.Administrador;
import plataforma.model.Contratante;
import plataforma.model.Contrato;
import plataforma.model.Denuncia;
import plataforma.model.EstadoReuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Notificacao;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.service.AgendamentoService;
import plataforma.service.ContratacaoService;
import plataforma.service.ModeracaoService;
import plataforma.service.NotificacaoService;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    // Base de dados em memória usando ArrayList
    private static final List<Trabalhadora> trabalhadoras = new ArrayList<>();
    private static final List<Contratante> contratantes = new ArrayList<>();
    private static final List<Reuniao> reunioes = new ArrayList<>();
    private static final List<Contrato> contratos = new ArrayList<>();
    private static final List<Notificacao> notificacoes = new ArrayList<>();

    private static Administrador admin;

    //Serviços
    private static NotificacaoService notificacaoService;
    private static ModeracaoService moderacaoService;
    private static AgendamentoService agendamentoService;
    private static ContratacaoService contratacaoService;

    public static void main(String[] args) {
        iniciar();
        menuPrincipal();
        System.out.println("Até à próxima!");
    }


    //  INICIALIZAÇÃO
    private static void iniciar() {
        notificacaoService = new NotificacaoService(notificacoes);
        moderacaoService = new ModeracaoService(trabalhadoras);
        agendamentoService = new AgendamentoService(reunioes, notificacaoService);
        contratacaoService = new ContratacaoService(contratos, notificacaoService);

        // Não há criação de contas de admin
        admin = new Administrador(1, "Admin", "84 111 1111");
    }


    //  MENU PRINCIPAL
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
            System.out.println("0 - Sair");
            opcao = lerInt("Opção: ");

            switch (opcao) {
                case 1:
                    menuAdministrador();
                    break;
                case 2:
                    Contratante c = null;
                    if (contratantes.isEmpty()) {
                        // Ainda não há contas: propõe criar já, sem voltar a mostrar o menu grande.
                        if (oferecerCriarConta("contratante")) {
                            c = criarContratante();
                        }
                    } else {
                        c = escolherContratante();
                    }
                    if (c != null) {
                        menuContratante(c);
                    }
                    break;
                case 3:
                    Trabalhadora t = null;
                    if (trabalhadoras.isEmpty()) {
                        if (oferecerCriarConta("trabalhadora")) {
                            t = criarTrabalhadora();
                        }
                    } else {
                        t = escolherTrabalhadora("Entrar como:", trabalhadoras);
                    }
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
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    //  MENU DO ADMINISTRADOR  (P03 - moderação)

    private static void menuAdministrador() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- ADMINISTRADOR: " + admin.getNome() + " ---");
            System.out.println("1 - Ver todas as trabalhadoras");
            System.out.println("2 - Ver perfis ocultos");
            System.out.println("3 - Validar documentação de uma trabalhadora");
            System.out.println("4 - Rever denúncias activas (arquivar)");
            System.out.println("5 - Ver contratos");
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
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static void verTodasTrabalhadoras() {
        System.out.println("\nTodas as trabalhadoras:");
        if (trabalhadoras.isEmpty()) {
            System.out.println("  (nenhuma)");
        }
        List<Trabalhadora> visiveis = moderacaoService.listarVisiveis();
        for (Trabalhadora t : trabalhadoras) {
            String visibilidade = visiveis.contains(t) ? "visível" : "não aparece na pesquisa";
            System.out.println("  - " + t.getNome() + " | " + t.getEstado()
                    + " | documentação " + (t.isDocumentacaoValidada() ? "validada" : "por validar")
                    + " | denúncias activas: " + t.contarDenunciasActivas()
                    + " | " + visibilidade);
        }
    }

    private static void verPerfisOcultos() {
        List<Trabalhadora> ocultos = moderacaoService.listarOcultos();
        System.out.println("\nPerfis ocultos (a rever):");
        if (ocultos.isEmpty()) {
            System.out.println("  (nenhum)");
        }
        for (Trabalhadora t : ocultos) {
            String motivo = !t.isDocumentacaoValidada()
                    ? "documentação por validar"
                    : "mais de 3 denúncias activas (" + t.contarDenunciasActivas() + ")";
            System.out.println("  - " + t.getNome() + ": " + motivo);
        }
    }

    private static void validarDocumentacao() {
        List<Trabalhadora> porValidar = new ArrayList<>();
        for (Trabalhadora t : trabalhadoras) {
            if (!t.isDocumentacaoValidada()) {
                porValidar.add(t);
            }
        }
        if (porValidar.isEmpty()) {
            System.out.println("Não há documentação por validar.");
            return;
        }
        Trabalhadora t = escolherTrabalhadora("Validar documentação de:", porValidar);
        if (t != null) {
            moderacaoService.validarDocumentacao(t);
            System.out.println("Documentação de " + t.getNome() + " validada.");
            if (moderacaoService.perfilValido(t)) {
                notificacaoService.enviar(t, "O teu perfil foi validado e já aparece nas pesquisas.");
            }
        }
    }

    private static void reverDenuncias() {
        List<Denuncia> activas = new ArrayList<>();
        for (Trabalhadora t : trabalhadoras) {
            for (Denuncia d : t.getDenuncias()) {
                if (d.isActiva()) {
                    activas.add(d);
                }
            }
        }
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

    private static void verContratos() {
        System.out.println("\nContratos:");
        if (contratos.isEmpty()) {
            System.out.println("  (nenhum)");
        }
        for (Contrato c : contratos) {
            System.out.println("  - " + c);
        }
    }

    //  MENU DO CONTRATANTE  (P01 agendamento, P02 contratação)

    private static void menuContratante(Contratante contratante) {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- CONTRATANTE: " + contratante.getNome() + " ---");
            System.out.println("1 - Pesquisar trabalhadoras disponíveis");
            System.out.println("2 - Agendar reunião");
            System.out.println("3 - As minhas reuniões");
            System.out.println("4 - Confirmar contratação");
            System.out.println("5 - Denunciar uma trabalhadora");
            System.out.println("6 - Ver notificações");
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
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static void pesquisarTrabalhadoras() {
        List<Trabalhadora> visiveis = moderacaoService.listarVisiveis();
        System.out.println("\nTrabalhadoras disponíveis:");
        if (visiveis.isEmpty()) {
            System.out.println("  (nenhuma de momento)");
        }
        for (Trabalhadora t : visiveis) {
            System.out.println("  - " + descrever(t));
        }
    }

    private static void agendarReuniao(Contratante contratante) {
        List<Trabalhadora> visiveis = moderacaoService.listarVisiveis();
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
        boolean encontrou = false;
        for (Reuniao r : reunioes) {
            if (r.getContratante() == contratante) {
                System.out.println("  - " + r);
                encontrou = true;
            }
        }
        if (!encontrou) {
            System.out.println("  (nenhuma)");
        }
    }

    private static void confirmarContratacao(Contratante contratante) {
        List<Reuniao> aceites = new ArrayList<>();
        for (Reuniao r : reunioes) {
            if (r.getContratante() == contratante
                    && r.getEstado() == EstadoReuniao.ACEITE
                    && r.getTrabalhadora().getEstado() == EstadoTrabalhadora.EM_REUNIAO) {
                aceites.add(r);
            }
        }
        if (aceites.isEmpty()) {
            System.out.println("Não tem reuniões aceites para confirmar.");
            return;
        }
        Reuniao r = escolherReuniao("Contratar após a reunião:", aceites);
        if (r != null) {
            contratacaoService.contratar(r);
        }
    }

    private static void denunciar(Contratante contratante) {
        Trabalhadora t = escolherTrabalhadora("Denunciar:", trabalhadoras);
        if (t == null) {
            return;
        }
        System.out.print("Motivo da denúncia: ");
        String motivo = scanner.nextLine().trim();
        if (motivo.isEmpty()) {
            System.out.println("Denúncia cancelada (motivo vazio).");
            return;
        }
        moderacaoService.denunciar(contratante, t, motivo);
        System.out.println("Denúncia registada. O administrador irá rever.");
    }


    //  MENU DA TRABALHADORA  (P04 - respostas e notificações)

    private static void menuTrabalhadora(Trabalhadora trabalhadora) {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- TRABALHADORA: " + trabalhadora.getNome() + " ---");
            System.out.println("1 - Ver o meu perfil");
            System.out.println("2 - Actualizar as minhas competências");
            System.out.println("3 - Ver convites pendentes e responder");
            System.out.println("4 - Ver notificações");
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
                    verNotificacoes(trabalhadora);
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static void verPerfil(Trabalhadora t) {
        System.out.println("\nNome: " + t.getNome());
        System.out.println("Província: " + t.getProvincia());
        System.out.println("Competências: " + (t.getCompetencias().isEmpty() ? "(por preencher)" : t.getCompetencias()));
        System.out.println("Estado: " + t.getEstado());
        System.out.println("Documentação: " + (t.isDocumentacaoValidada() ? "validada" : "por validar"));
        System.out.println("Denúncias activas: " + t.contarDenunciasActivas());
        System.out.println("Perfil: " + (moderacaoService.perfilValido(t) ? "válido" : "oculto das pesquisas"));
    }

    private static void responderConvites(Trabalhadora trabalhadora) {
        List<Reuniao> pendentes = new ArrayList<>();
        for (Reuniao r : reunioes) {
            if (r.getTrabalhadora() == trabalhadora && r.getEstado() == EstadoReuniao.PENDENTE) {
                pendentes.add(r);
            }
        }
        if (pendentes.isEmpty()) {
            System.out.println("Não tem convites pendentes.");
            return;
        }
        Reuniao r = escolherReuniao("Convite a responder:", pendentes);
        if (r == null) {
            return;
        }
        int resposta = lerInt("1 - Aceitar | 2 - Recusar | 0 - Cancelar: ");
        if (resposta == 1) {
            agendamentoService.responderConvite(r, true);
        } else if (resposta == 2) {
            agendamentoService.responderConvite(r, false);
        }
    }


    //  CRIAÇÃO DE CONTAS
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
        Contratante c = new Contratante(contratantes.size() + 1, nome, contacto, morada);
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
        if (nome.isEmpty()) {
            System.out.println("Conta não criada (nome vazio).");
            return null;
        }
        Trabalhadora t = new Trabalhadora(trabalhadoras.size() + 1, nome, contacto, provincia);
        t.setCompetencias(competencias);
        trabalhadoras.add(t);
        System.out.println("Conta criada: " + t);
        System.out.println("O perfil só aparece nas pesquisas depois de o administrador validar a documentação.");
        return t;
    }

    //  NOTIFICAÇÕES

    private static void verNotificacoes(plataforma.model.Utilizador utilizador) {
        List<Notificacao> lista = notificacaoService.listarDe(utilizador);
        System.out.println("\nNotificações:");
        if (lista.isEmpty()) {
            System.out.println("  (nenhuma)");
        }
        for (Notificacao n : lista) {
            System.out.println("  " + (n.isLida() ? "   " : "[NOVA] ") + n.getMensagem());
            n.marcarComoLida();
        }
    }


    //  FUNÇÕES DE APOIO
    // Avisa que ainda não há contas e pergunta se quer criar uma agora.
    // Devolve true se a pessoa quiser criar a conta.
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

    // Lê um número inteiro; repete a pergunta se o texto não for um número.
    private static int lerInt(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String texto = scanner.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escreva um número válido.");
            }
        }
    }
}