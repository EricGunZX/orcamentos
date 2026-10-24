package domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Utilitário para valores monetários: arredondamento e formato português. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Ex.: 3243.51 -> "3243,51 €". Símbolos definidos à mão: não depende dos dados de idioma do Java. */
    public static String formatar(BigDecimal valor) {
        java.text.DecimalFormatSymbols sim = new java.text.DecimalFormatSymbols(Locale.ROOT);
        sim.setDecimalSeparator(',');
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.00", sim);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(valor) + " €";
    }
}
