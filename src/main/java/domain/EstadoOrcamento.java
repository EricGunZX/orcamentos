package domain;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida de um orçamento (US07). */
public enum EstadoOrcamento {
    RASCUNHO("Rascunho"),
    EMITIDO("Emitido"),
    ADJUDICADO("Adjudicado"),
    RECUSADO("Recusado"),
    ANULADO("Anulado");

    private final String descricao;

    EstadoOrcamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Estados para os quais é permitido transitar a partir deste. */
    public Set<EstadoOrcamento> transicoesPermitidas() {
        switch (this) {
            case RASCUNHO:
                return EnumSet.of(EMITIDO, ANULADO);
            case EMITIDO:
                return EnumSet.of(ADJUDICADO, RECUSADO, ANULADO);
            case ADJUDICADO:
                return EnumSet.of(ANULADO);
            default:
                return EnumSet.noneOf(EstadoOrcamento.class);
        }
    }

    public boolean podeTransitarPara(EstadoOrcamento novo) {
        return transicoesPermitidas().contains(novo);
    }

    @Override
    public String toString() {
        return descricao;
    }
}
