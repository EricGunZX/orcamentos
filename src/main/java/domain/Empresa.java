package domain;

import java.io.Serializable;
import java.math.BigDecimal;

/** Empresa emissora e parâmetros do sistema (US02). */
public class Empresa implements Serializable {
    private static final long serialVersionUID = 1L;

    private String designacao;
    private String nif;
    private String morada;
    private String telefone;
    private String email;
    private String website;
    private String caminhoLogotipo;
    private BigDecimal taxaIva = new BigDecimal("23");
    private String condicoesPagamentoPorDefeito = "40% na adjudicação, restante no fim da obra.";
    private int diasValidadePorDefeito = 30;

    public Empresa(String designacao, String nif, String morada,
                   String telefone, String email, String website) {
        this.designacao = Validacao.textoObrigatorio(designacao, "Designação");
        this.nif = Validacao.nif(nif);
        this.morada = Validacao.textoObrigatorio(morada, "Morada");
        this.telefone = telefone;
        this.email = email;
        this.website = website;
    }

    /** Dados da Números Profícuos, tal como aparecem nos orçamentos. */
    public static Empresa numerosProficuos() {
        return new Empresa("Números Profícuos", "517870444",
                "Rua Joaquim Pinto, 210 – 1.º\n4815-434 Vizela - Portugal",
                "933658520", "numerosproficuos@gmail.com", "numerosproficuos.pt");
    }

    public String getDesignacao() { return designacao; }
    public String getNif() { return nif; }
    public String getMorada() { return morada; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public String getCaminhoLogotipo() { return caminhoLogotipo; }
    public BigDecimal getTaxaIva() { return taxaIva; }
    public String getCondicoesPagamentoPorDefeito() { return condicoesPagamentoPorDefeito; }
    public int getDiasValidadePorDefeito() { return diasValidadePorDefeito; }

    public void setDesignacao(String v) { this.designacao = Validacao.textoObrigatorio(v, "Designação"); }
    public void setNif(String v) { this.nif = Validacao.nif(v); }
    public void setMorada(String v) { this.morada = Validacao.textoObrigatorio(v, "Morada"); }
    public void setTelefone(String v) { this.telefone = Validacao.telefoneOpcional(v); }
    public void setEmail(String v) { this.email = Validacao.emailOpcional(v); }
    public void setWebsite(String v) { this.website = v; }
    public void setCaminhoLogotipo(String v) { this.caminhoLogotipo = v; }

    public void setTaxaIva(BigDecimal taxa) {
        if (taxa == null || taxa.signum() < 0 || taxa.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("A taxa de IVA deve estar entre 0 e 100.");
        }
        this.taxaIva = taxa;
    }

    public void setCondicoesPagamentoPorDefeito(String v) { this.condicoesPagamentoPorDefeito = v; }

    public void setDiasValidadePorDefeito(int dias) {
        if (dias <= 0) {
            throw new IllegalArgumentException("A validade deve ser positiva.");
        }
        this.diasValidadePorDefeito = dias;
    }
}
