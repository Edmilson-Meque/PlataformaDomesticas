package plataforma.logica;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import plataforma.model.Trabalhadora;

// PARADIGMA LÓGICO - motor de inferência simples (lógica de predicados)
// Em vez de "if's" espalhados, declaramos REGRAS: SE premissas ENTÃO conclusão.
// O motor DEDUZ a conclusão quando as premissas de alguma regra são verdadeiras.
public class MotorRegras {

    public static final String BLOQUEAR_CONTRATACAO = "bloquearContratacao";
    public static final String OCULTAR_DAS_PESQUISAS = "ocultarDasPesquisas";

    public record Regra(String nome, String texto, Predicate<Trabalhadora> premissas, String conclusao) {
    }

    private final List<Regra> regras = new ArrayList<>();

    public MotorRegras() {
        // R1 = regra do documento do Grupo 2
        // Conta-se o número de contratantes DIFERENTES que denunciaram (uma só não bloqueia ninguém)
        regras.add(new Regra("R1", "SE denúncias de mais de 3 contratantes diferentes E perfil não verificado",
                t -> t.contarDenunciantesDistintos() > 3 && !t.isDocumentacaoValidada(), BLOQUEAR_CONTRATACAO));
        regras.add(new Regra("R2", "SE denúncias de mais de 3 contratantes diferentes (o administrador ainda não analisou)",
                t -> t.contarDenunciantesDistintos() > 3, BLOQUEAR_CONTRATACAO));
        regras.add(new Regra("R3", "SE perfil não verificado",
                t -> !t.isDocumentacaoValidada(), OCULTAR_DAS_PESQUISAS));
        regras.add(new Regra("R4", "SE bloquearContratacao(X)",
                t -> deduz(BLOQUEAR_CONTRATACAO, t), OCULTAR_DAS_PESQUISAS));
    }

    // A conclusão é VERDADEIRA se ALGUMA regra com essa conclusão tiver as premissas verdadeiras
    public boolean deduz(String conclusao, Trabalhadora t) {
        return regras.stream().anyMatch(r -> r.conclusao().equals(conclusao) && r.premissas().test(t));
    }

    // Mostra que regras provaram a conclusão
    public String explicar(String conclusao, Trabalhadora t) {
        StringBuilder sb = new StringBuilder();
        for (Regra r : regras) {
            if (r.conclusao().equals(conclusao) && r.premissas().test(t)) {
                sb.append("      ").append(r.nome()).append(": ").append(r.texto()).append(" ENTÃO ")
                        .append(conclusao).append("(").append(t.getNome()).append(")\n");
            }
        }
        return sb.length() == 0 ? "      nenhuma regra se aplica\n" : sb.toString();
    }
}
