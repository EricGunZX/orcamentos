package domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Utilitário para valores monetários: arredondamento e formato português. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Ex.: 3243.51 -> "3243,51 €". */
    public static String formatar(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-PT"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        nf.setGroupingUsed(false);
        return nf.format(valor) + " €";
    }
}
