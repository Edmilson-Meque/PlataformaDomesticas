package plataforma.model;

import java.util.ArrayList;
import java.util.List;

public class Trabalhadora extends Utilizador {

    private String provincia;
    private String competencias;
    private boolean documentacaoValidada;
    // volatile = VISIBILIDADE: quando uma thread muda o estado, as outras vêem logo o valor novo
    private volatile EstadoTrabalhadora estado;
    private String bi = ""; // número do Bilhete de Identidade
    private List<Denuncia> denuncias;

    public Trabalhadora(int id, String nome, String contacto, String provincia) {
        super(id, nome, contacto);
        this.provincia = provincia;
        this.competencias = "";
        this.documentacaoValidada = false;
        this.estado = EstadoTrabalhadora.DISPONIVEL;
        this.denuncias = new ArrayList<>();
    }

    public String getProvincia() {
        return provincia;
    }

    public String getCompetencias() {
        return competencias;
    }

    public void setCompetencias(String competencias) {
        this.competencias = competencias;
    }

    public boolean isDocumentacaoValidada() {
        return documentacaoValidada;
    }

    public void setDocumentacaoValidada(boolean documentacaoValidada) {
        this.documentacaoValidada = documentacaoValidada;
    }

    public EstadoTrabalhadora getEstado() {
        return estado;
    }

    public void setEstado(EstadoTrabalhadora estado) {
        this.estado = estado;
    }

    public String getBi() {
        return bi;
    }

    public void setBi(String bi) {
        this.bi = bi;
    }

    public List<Denuncia> getDenuncias() {
        return denuncias;
    }

    public void adicionarDenuncia(Denuncia denuncia) {
        denuncias.add(denuncia);
    }

    // Operação funcional: filter + count
    public int contarDenunciasActivas() {
        return (int) denuncias.stream()
                .filter(Denuncia::isActiva)
                .count();
    }

    // Quantas contratantes DIFERENTES têm denúncias activas contra ela
    // (uma só contratante não consegue bloquear ninguém sozinha)
    public int contarDenunciantesDistintos() {
        return (int) denuncias.stream()
                .filter(Denuncia::isActiva)
                .map(d -> d.getAutor().getId())
                .distinct()
                .count();
    }

    @Override
    public String toString() {
        return getNome() + " [" + estado + "]";
    }
}