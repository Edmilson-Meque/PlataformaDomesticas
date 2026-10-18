package plataforma.model;

import java.util.ArrayList;
import java.util.List;

public class Trabalhadora extends Utilizador {

    private String provincia;
    private String competencias;                 // descrição livre das competências
    private boolean documentacaoValidada;
    private EstadoTrabalhadora estado;
    private List<Denuncia> denuncias;

    public Trabalhadora(int id, String nome, String contacto, String provincia) {
        super(id, nome, contacto);
        this.provincia = provincia;
        this.competencias = "";                  // a trabalhadora preenche depois
        this.documentacaoValidada = false;       // só passa a true depois de validada
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

    public List<Denuncia> getDenuncias() {
        return denuncias;
    }

    public void adicionarDenuncia(Denuncia denuncia) {
        denuncias.add(denuncia);
    }

    // Conta só as denúncias que ainda estão activas.
    public int contarDenunciasActivas() {
        int total = 0;
        for (Denuncia d : denuncias) {
            if (d.isActiva()) {
                total++;
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return getNome() + " [" + estado + "]";
    }
}