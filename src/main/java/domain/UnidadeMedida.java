package domain;

/** Unidades de medida usadas nas linhas de orçamento. */
public enum UnidadeMedida {
    METRO_QUADRADO("m²"),
    METRO("m"),
    UNIDADE("un"),
    VERBA_GLOBAL("vg"),
    CONJUNTO("cj."),
    HORA("h");

    private final String simbolo;

    UnidadeMedida(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getSimbolo() {
        return simbolo;
    }

    @Override
    public String toString() {
        return simbolo;
    }
}
