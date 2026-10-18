package plataforma.service;

import java.util.ArrayList;
import java.util.List;

import plataforma.model.Contratante;
import plataforma.model.Denuncia;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Trabalhadora;

// P03 - Validação e moderação de perfis
public class ModeracaoService {

    private static final int MAX_DENUNCIAS_ACTIVAS = 3;

    private List<Trabalhadora> trabalhadoras;
    private int proximoIdDenuncia = 1;

    public ModeracaoService(List<Trabalhadora> trabalhadoras) {
        this.trabalhadoras = trabalhadoras;
    }

    // Regra R01: o perfil só é válido com documentação validada
    // e com no máximo 3 denúncias activas.
    public boolean perfilValido(Trabalhadora t) {
        return t.isDocumentacaoValidada()
                && t.contarDenunciasActivas() <= MAX_DENUNCIAS_ACTIVAS;
    }

    // Perfis que os contratantes podem ver na pesquisa:
    // perfil válido e trabalhadora disponível.
    public List<Trabalhadora> listarVisiveis() {
        List<Trabalhadora> visiveis = new ArrayList<>();
        for (Trabalhadora t : trabalhadoras) {
            if (perfilValido(t) && t.getEstado() == EstadoTrabalhadora.DISPONIVEL) {
                visiveis.add(t);
            }
        }
        return visiveis;
    }

    // Perfis ocultos por falha de validação (para o administrador rever).
    public List<Trabalhadora> listarOcultos() {
        List<Trabalhadora> ocultos = new ArrayList<>();
        for (Trabalhadora t : trabalhadoras) {
            if (!perfilValido(t)) {
                ocultos.add(t);
            }
        }
        return ocultos;
    }

    // O administrador valida a documentação de uma trabalhadora.
    public void validarDocumentacao(Trabalhadora t) {
        t.setDocumentacaoValidada(true);
    }

    // Um contratante denuncia uma trabalhadora.
    public Denuncia denunciar(Contratante autor, Trabalhadora alvo, String motivo) {
        Denuncia d = new Denuncia(proximoIdDenuncia++, autor, alvo, motivo);
        alvo.adicionarDenuncia(d);
        return d;
    }

    // O administrador revê e arquiva uma denúncia (deixa de estar activa).
    public void arquivarDenuncia(Denuncia d) {
        d.setActiva(false);
    }
}