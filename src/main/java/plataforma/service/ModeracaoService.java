package plataforma.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.logica.MotorRegras;
import plataforma.model.Contratante;
import plataforma.model.Denuncia;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;

// P03 - Validação e moderação de perfis (Programação Funcional + Eventos + Paradigma Lógico)
public class ModeracaoService {

    private List<Trabalhadora> trabalhadoras;
    private List<Reuniao> reunioes;
    private EventBus eventBus;
    private MotorRegras motor;

    public ModeracaoService(List<Trabalhadora> trabalhadoras, List<Reuniao> reunioes, EventBus eventBus, MotorRegras motor) {
        this.trabalhadoras = trabalhadoras;
        this.reunioes = reunioes;
        this.eventBus = eventBus;
        this.motor = motor;
    }

    // A decisão é DEDUZIDA pelo motor de regras (Paradigma Lógico)
    public boolean perfilValido(Trabalhadora t) {
        return !motor.deduz(MotorRegras.OCULTAR_DAS_PESQUISAS, t);
    }

    // Funcional: filter + sorted por nome
    public List<Trabalhadora> listarVisiveis() {
        return trabalhadoras.stream()
                .filter(this::perfilValido)
                .filter(t -> t.getEstado() == EstadoTrabalhadora.DISPONIVEL)
                .sorted(Comparator.comparing(Trabalhadora::getNome, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    // Funcional: filter por província ou competência + sorted
    public List<Trabalhadora> pesquisarVisiveisPorFiltro(String termo) {
        String filtro = termo.toLowerCase();
        return listarVisiveis().stream()
                .filter(t -> t.getProvincia().toLowerCase().contains(filtro)
                        || t.getCompetencias().toLowerCase().contains(filtro)
                        || t.getNome().toLowerCase().contains(filtro))
                .sorted(Comparator.comparing(Trabalhadora::getProvincia)
                        .thenComparing(Trabalhadora::getNome))
                .collect(Collectors.toList());
    }

    // Funcional: filter + sorted por número de denúncias activas (decrescente)
    public List<Trabalhadora> listarOcultos() {
        return trabalhadoras.stream()
                .filter(t -> !perfilValido(t))
                .sorted(Comparator.comparingInt(Trabalhadora::contarDenunciasActivas).reversed())
                .collect(Collectors.toList());
    }

    public void validarDocumentacao(Trabalhadora t) {
        t.setDocumentacaoValidada(true);
        if (perfilValido(t)) {
            eventBus.publicar(TipoEvento.PERFIL_VALIDADO, t, "Admin");
        }
    }

    // Só pode denunciar quem já TEVE CONTACTO com a trabalhadora (reunião ou contrato),
    // uma única vez, e só perfis já públicos (validados pelo administrador).
    public boolean podeDenunciar(Contratante autor, Trabalhadora alvo) {
        return alvo.isDocumentacaoValidada()
                && reunioes.stream().anyMatch(r -> r.getContratante() == autor && r.getTrabalhadora() == alvo)
                && alvo.getDenuncias().stream().noneMatch(d -> d.isActiva() && d.getAutor() == autor);
    }

    public synchronized Denuncia denunciar(Contratante autor, Trabalhadora alvo, String motivo) {
        if (!alvo.isDocumentacaoValidada()) {
            System.out.println("   [Rejeitado] O perfil de " + alvo.getNome()
                    + " ainda não é público (aguarda validação). Não pode ser denunciado.");
            return null;
        }
        if (reunioes.stream().noneMatch(r -> r.getContratante() == autor && r.getTrabalhadora() == alvo)) {
            System.out.println("   [Rejeitado] Só pode denunciar uma trabalhadora com quem já teve reunião ou contrato.");
            return null;
        }
        if (alvo.getDenuncias().stream().anyMatch(d -> d.isActiva() && d.getAutor() == autor)) {
            System.out.println("   [Rejeitado] Já tem uma denúncia activa contra " + alvo.getNome() + ".");
            return null;
        }
        int id = trabalhadoras.stream().flatMap(t -> t.getDenuncias().stream())
                .mapToInt(Denuncia::getId).max().orElse(0) + 1;
        Denuncia d = new Denuncia(id, autor, alvo, motivo);
        alvo.adicionarDenuncia(d);
        eventBus.publicar(TipoEvento.DENUNCIA_REGISTADA, d, autor.getNome());
        return d;
    }

    public void arquivarDenuncia(Denuncia d) {
        d.setActiva(false);
    }
}
