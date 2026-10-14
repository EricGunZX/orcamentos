package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grupo de linhas com subtotal próprio (ex.: "Construção civil").
 * Um capítulo sem título representa orçamentos sem capítulos.
 */
public class Capitulo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String titulo;
    private final List<LinhaOrcamento> linhas = new ArrayList<>();

    public Capitulo(String titulo) {
        this.titulo = titulo;
    }

    public boolean temTitulo() {
        return titulo != null && !titulo.isBlank();
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public List<LinhaOrcamento> getLinhas() {
        return Collections.unmodifiableList(linhas);
    }

    public void adicionarLinha(LinhaOrcamento linha) {
        if (linha == null) {
            throw new IllegalArgumentException("A linha é obrigatória.");
        }
        linhas.add(linha);
    }

    public boolean removerLinha(LinhaOrcamento linha) {
        return linhas.remove(linha);
    }

    public BigDecimal getSubtotal() {
        BigDecimal soma = BigDecimal.ZERO;
        for (LinhaOrcamento l : linhas) {
            soma = soma.add(l.getTotal());
        }
        return Dinheiro.arredondar(soma);
    }
}
