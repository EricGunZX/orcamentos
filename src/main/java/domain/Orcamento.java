package domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Orçamento: cliente, capítulos/linhas, cálculos e estado (US05, US06, US07). */
public class Orcamento implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Registo de uma mudança de estado. */
    public static class MudancaEstado implements Serializable {
        private static final long serialVersionUID = 1L;
        private final EstadoOrcamento estado;
        private final LocalDateTime data;
        private final String utilizador;

        MudancaEstado(EstadoOrcamento estado, String utilizador) {
            this.estado = estado;
            this.data = LocalDateTime.now();
            this.utilizador = utilizador;
        }

        public EstadoOrcamento getEstado() { return estado; }
        public LocalDateTime getData() { return data; }
        public String getUtilizador() { return utilizador; }
    }

    private final String numero;
    private final LocalDate data;
    private final String criadoPor;
    private Cliente cliente;
    private String assunto;
    private LocalDate validoAte;
    private String prazoExecucao;
    private String condicoesPagamento;
    private String exclusoes;
    private String inclusoes;
    private boolean ivaIncluido = true;
    private BigDecimal taxaIva;
    private EstadoOrcamento estado = EstadoOrcamento.RASCUNHO;
    private final List<Capitulo> capitulos = new ArrayList<>();
    private final List<MudancaEstado> historico = new ArrayList<>();

    /** Cria um rascunho com os valores por defeito da empresa. */
    public Orcamento(String numero, Cliente cliente, Empresa empresa, String criadoPor) {
        this.numero = Validacao.textoObrigatorio(numero, "Número");
        this.data = LocalDate.now();
        this.criadoPor = criadoPor;
        setCliente(cliente);
        this.taxaIva = empresa.getTaxaIva();
        this.condicoesPagamento = empresa.getCondicoesPagamentoPorDefeito();
        this.validoAte = data.plusDays(empresa.getDiasValidadePorDefeito());
        historico.add(new MudancaEstado(EstadoOrcamento.RASCUNHO, criadoPor));
    }

    // ---------- Edição (só em rascunho) ----------

    private void garantirRascunho() {
        if (estado != EstadoOrcamento.RASCUNHO) {
            throw new IllegalStateException("Só os rascunhos podem ser editados.");
        }
    }

    public void setCliente(Cliente cliente) {
        garantirRascunho();
        if (cliente == null) {
            throw new IllegalArgumentException("O cliente é obrigatório.");
        }
        this.cliente = cliente;
    }

    public void setAssunto(String assunto) { garantirRascunho(); this.assunto = assunto; }
    public void setPrazoExecucao(String v) { garantirRascunho(); this.prazoExecucao = v; }
    public void setCondicoesPagamento(String v) { garantirRascunho(); this.condicoesPagamento = v; }
    public void setExclusoes(String v) { garantirRascunho(); this.exclusoes = v; }
    public void setInclusoes(String v) { garantirRascunho(); this.inclusoes = v; }
    public void setIvaIncluido(boolean v) { garantirRascunho(); this.ivaIncluido = v; }

    public void setValidoAte(LocalDate validoAte) {
        garantirRascunho();
        if (validoAte == null || validoAte.isBefore(data)) {
            throw new IllegalArgumentException("A validade não pode ser anterior à data do orçamento.");
        }
        this.validoAte = validoAte;
    }

    public void setTaxaIva(BigDecimal taxa) {
        garantirRascunho();
        if (taxa == null || taxa.signum() < 0) {
            throw new IllegalArgumentException("A taxa de IVA é inválida.");
        }
        this.taxaIva = taxa;
    }

    // ---------- Capítulos e linhas ----------

    public Capitulo adicionarCapitulo(String titulo) {
        garantirRascunho();
        Capitulo c = new Capitulo(titulo);
        capitulos.add(c);
        return c;
    }

    /** Adiciona uma linha a um capítulo existente. */
    public void adicionarLinha(Capitulo capitulo, LinhaOrcamento linha) {
        garantirRascunho();
        if (!capitulos.contains(capitulo)) {
            throw new IllegalArgumentException("O capítulo não pertence a este orçamento.");
        }
        capitulo.adicionarLinha(linha);
    }

    /** Adiciona uma linha ao capítulo sem título (orçamentos sem capítulos). */
    public void adicionarLinha(LinhaOrcamento linha) {
        garantirRascunho();
        Capitulo destino = null;
        for (Capitulo c : capitulos) {
            if (!c.temTitulo()) {
                destino = c;
                break;
            }
        }
        if (destino == null) {
            destino = adicionarCapitulo(null);
        }
        destino.adicionarLinha(linha);
    }

    public boolean removerLinha(LinhaOrcamento linha) {
        garantirRascunho();
        for (Capitulo c : capitulos) {
            if (c.removerLinha(linha)) {
                return true;
            }
        }
        return false;
    }

    public boolean removerCapitulo(Capitulo capitulo) {
        garantirRascunho();
        return capitulos.remove(capitulo);
    }

    // ---------- Cálculos ----------

    public BigDecimal getSubtotal() {
        BigDecimal soma = BigDecimal.ZERO;
        for (Capitulo c : capitulos) {
            soma = soma.add(c.getSubtotal());
        }
        return Dinheiro.arredondar(soma);
    }

    /** Valor do IVA; zero quando o orçamento indica "IVA não incluído". */
    public BigDecimal getValorIva() {
        if (!ivaIncluido) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
        return Dinheiro.arredondar(
                getSubtotal().multiply(taxaIva).divide(new BigDecimal("100")));
    }

    public BigDecimal getTotal() {
        return Dinheiro.arredondar(getSubtotal().add(getValorIva()));
    }

    // ---------- Estado ----------

    /** Emitido e com a validade ultrapassada (ainda pode ser adjudicado ou recusado). */
    public boolean isExpirado() {
        return estado == EstadoOrcamento.EMITIDO && validoAte != null && validoAte.isBefore(LocalDate.now());
    }

    /** Texto do estado para mostrar ao utilizador (junta "expirado" quando aplicável). */
    public String getEstadoTexto() {
        return isExpirado() ? "Emitido (expirado)" : estado.getDescricao();
    }

    /** Só um rascunho nunca emitido pode ser eliminado. */
    public boolean podeEliminar() {
        return estado == EstadoOrcamento.RASCUNHO;
    }

    /** Muda de estado se a transição for válida e regista data e utilizador. */
    public void mudarEstado(EstadoOrcamento novo, String utilizador) {
        if (!estado.podeTransitarPara(novo)) {
            throw new IllegalStateException(
                    "Transição inválida: " + estado + " -> " + novo + ".");
        }
        if (novo == EstadoOrcamento.EMITIDO) {
            if (getSubtotal().signum() == 0) {
                throw new IllegalStateException("Não é possível emitir um orçamento sem linhas com valor.");
            }
            if (!cliente.isCompleto()) {
                throw new IllegalStateException(
                        "Preencha o nome, a morada e o NIF do cliente para emitir o orçamento.");
            }
            if (validoAte == null || validoAte.isBefore(LocalDate.now())) {
                throw new IllegalStateException(
                        "A validade já passou. Defina uma nova data de validade antes de emitir.");
            }
        }
        estado = novo;
        historico.add(new MudancaEstado(novo, utilizador));
    }

    /** Cria um novo rascunho com as mesmas linhas, capítulos e textos (US10). */
    public Orcamento duplicar(String novoNumero, Empresa empresa, String utilizador) {
        Orcamento copia = new Orcamento(novoNumero, cliente, empresa, utilizador);
        copia.assunto = assunto;
        copia.prazoExecucao = prazoExecucao;
        copia.condicoesPagamento = condicoesPagamento;
        copia.exclusoes = exclusoes;
        copia.inclusoes = inclusoes;
        copia.ivaIncluido = ivaIncluido;
        copia.taxaIva = taxaIva;
        for (Capitulo c : capitulos) {
            Capitulo novo = copia.adicionarCapitulo(c.getTitulo());
            for (LinhaOrcamento l : c.getLinhas()) {
                novo.adicionarLinha(new LinhaOrcamento(l.getDescricao(), l.getQuantidade(),
                        l.getUnidade(), l.getPrecoUnitario()));
            }
        }
        return copia;
    }

    // ---------- Consulta ----------

    public String getNumero() { return numero; }
    public LocalDate getData() { return data; }
    public String getCriadoPor() { return criadoPor; }
    public Cliente getCliente() { return cliente; }
    public String getAssunto() { return assunto; }
    public LocalDate getValidoAte() { return validoAte; }
    public String getPrazoExecucao() { return prazoExecucao; }
    public String getCondicoesPagamento() { return condicoesPagamento; }
    public String getExclusoes() { return exclusoes; }
    public String getInclusoes() { return inclusoes; }
    public boolean isIvaIncluido() { return ivaIncluido; }
    public BigDecimal getTaxaIva() { return taxaIva; }
    public EstadoOrcamento getEstado() { return estado; }
    public boolean temCapitulos() {
        return capitulos.stream().anyMatch(Capitulo::temTitulo);
    }
    public List<Capitulo> getCapitulos() { return Collections.unmodifiableList(capitulos); }
    public List<MudancaEstado> getHistorico() { return Collections.unmodifiableList(historico); }

    @Override
    public String toString() {
        return numero + " - " + cliente.getNome() + " - " + Dinheiro.formatar(getTotal())
                + " [" + estado + "]";
    }
}
