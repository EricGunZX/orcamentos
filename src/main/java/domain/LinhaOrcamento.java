package domain;

import java.io.Serializable;
import java.math.BigDecimal;

/** Item de um orçamento (US06). */
public class LinhaOrcamento implements Serializable {
    private static final long serialVersionUID = 1L;

    private String descricao;
    private BigDecimal quantidade;
    private UnidadeMedida unidade;
    private BigDecimal precoUnitario;

    public LinhaOrcamento(String descricao, BigDecimal quantidade,
                          UnidadeMedida unidade, BigDecimal precoUnitario) {
        setDescricao(descricao);
        setQuantidade(quantidade);
        setUnidade(unidade);
        setPrecoUnitario(precoUnitario);
    }

    /** Total da linha = quantidade x preço unitário, arredondado a 2 casas. */
    public BigDecimal getTotal() {
        return Dinheiro.arredondar(quantidade.multiply(precoUnitario));
    }

    public String getDescricao() { return descricao; }
    public BigDecimal getQuantidade() { return quantidade; }
    public UnidadeMedida getUnidade() { return unidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }

    public void setDescricao(String descricao) {
        this.descricao = Validacao.textoObrigatorio(descricao, "Descrição");
    }

    public void setQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser positiva.");
        }
        this.quantidade = quantidade;
    }

    public void setUnidade(UnidadeMedida unidade) {
        if (unidade == null) {
            throw new IllegalArgumentException("A unidade é obrigatória.");
        }
        this.unidade = unidade;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        if (precoUnitario == null || precoUnitario.signum() <= 0) {
            throw new IllegalArgumentException("O preço unitário deve ser positivo.");
        }
        this.precoUnitario = precoUnitario;
    }
}
