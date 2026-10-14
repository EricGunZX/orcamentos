package controller;

import domain.Capitulo;
import domain.Cliente;
import domain.EstadoOrcamento;
import domain.LinhaOrcamento;
import domain.Orcamento;
import domain.UnidadeMedida;
import repository.Repositorio;
import service.GeradorPdf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** US05, US06, US07, US09 e US10: criação, edição, estados, consulta e duplicação. */
public class OrcamentoController {

    private final Repositorio repositorio;
    private final AutenticacaoController auth;

    public OrcamentoController(Repositorio repositorio, AutenticacaoController auth) {
        this.repositorio = repositorio;
        this.auth = auth;
    }

    public Orcamento criar(Cliente cliente, String assunto) {
        String utilizador = auth.exigirSessao().getNomeUtilizador();
        Orcamento o = new Orcamento(repositorio.proximoNumeroOrcamento(), cliente,
                repositorio.getEmpresa(), utilizador);
        o.setAssunto(assunto);
        repositorio.adicionarOrcamento(o);
        repositorio.guardar();
        return o;
    }

    public Capitulo adicionarCapitulo(Orcamento o, String titulo) {
        auth.exigirSessao();
        Capitulo c = o.adicionarCapitulo(titulo);
        repositorio.guardar();
        return c;
    }

    public LinhaOrcamento adicionarLinha(Orcamento o, Capitulo capitulo, String descricao,
                                         BigDecimal quantidade, UnidadeMedida unidade,
                                         BigDecimal precoUnitario) {
        auth.exigirSessao();
        LinhaOrcamento l = new LinhaOrcamento(descricao, quantidade, unidade, precoUnitario);
        if (capitulo == null) {
            o.adicionarLinha(l);
        } else {
            o.adicionarLinha(capitulo, l);
        }
        repositorio.guardar();
        return l;
    }

    public void removerLinha(Orcamento o, LinhaOrcamento linha) {
        auth.exigirSessao();
        o.removerLinha(linha);
        repositorio.guardar();
    }

    public void definirCondicoes(Orcamento o, boolean ivaIncluido, LocalDate validoAte,
                                 String prazoExecucao, String condicoesPagamento,
                                 String exclusoes, String inclusoes) {
        auth.exigirSessao();
        o.setIvaIncluido(ivaIncluido);
        o.setValidoAte(validoAte);
        o.setPrazoExecucao(prazoExecucao);
        o.setCondicoesPagamento(condicoesPagamento);
        o.setExclusoes(exclusoes);
        o.setInclusoes(inclusoes);
        repositorio.guardar();
    }

    public void mudarEstado(Orcamento o, EstadoOrcamento novo) {
        String utilizador = auth.exigirSessao().getNomeUtilizador();
        o.mudarEstado(novo, utilizador);
        repositorio.guardar();
    }

    public Orcamento duplicar(Orcamento original) {
        String utilizador = auth.exigirSessao().getNomeUtilizador();
        Orcamento copia = original.duplicar(repositorio.proximoNumeroOrcamento(),
                repositorio.getEmpresa(), utilizador);
        repositorio.adicionarOrcamento(copia);
        repositorio.guardar();
        return copia;
    }

    /** Qualquer filtro a null é ignorado. */
    public List<Orcamento> pesquisar(String numero, String cliente,
                                     LocalDate de, LocalDate ate, EstadoOrcamento estado) {
        auth.exigirSessao();
        return repositorio.getOrcamentos().stream()
                .filter(o -> numero == null || o.getNumero().contains(numero))
                .filter(o -> cliente == null
                        || o.getCliente().getNome().toLowerCase().contains(cliente.toLowerCase())
                        || o.getCliente().getNif().contains(cliente))
                .filter(o -> de == null || !o.getData().isBefore(de))
                .filter(o -> ate == null || !o.getData().isAfter(ate))
                .filter(o -> estado == null || o.getEstado() == estado)
                .collect(Collectors.toList());
    }

    public List<Orcamento> listar() {
        return pesquisar(null, null, null, null, null);
    }

    // ---------- Edição de um rascunho (usada pela interface) ----------

    private void exigirRascunho(Orcamento o) {
        auth.exigirSessao();
        if (o.getEstado() != EstadoOrcamento.RASCUNHO) {
            throw new IllegalStateException("Só os rascunhos podem ser editados.");
        }
    }

    public void definirAssunto(Orcamento o, String assunto) {
        exigirRascunho(o);
        o.setAssunto(assunto);
        repositorio.guardar();
    }

    public void alterarCliente(Orcamento o, Cliente cliente) {
        exigirRascunho(o);
        o.setCliente(cliente);
        repositorio.guardar();
    }

    public void definirIva(Orcamento o, boolean incluido, BigDecimal taxa) {
        exigirRascunho(o);
        o.setTaxaIva(taxa);
        o.setIvaIncluido(incluido);
        repositorio.guardar();
    }

    public void renomearCapitulo(Orcamento o, Capitulo capitulo, String titulo) {
        exigirRascunho(o);
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("O título do capítulo é obrigatório.");
        }
        capitulo.setTitulo(titulo.trim());
        repositorio.guardar();
    }

    public void removerCapitulo(Orcamento o, Capitulo capitulo) {
        exigirRascunho(o);
        o.removerCapitulo(capitulo);
        repositorio.guardar();
    }

    /** Altera uma linha; valida tudo antes de mexer na linha original. */
    public void editarLinha(Orcamento o, LinhaOrcamento linha, String descricao,
                            BigDecimal quantidade, UnidadeMedida unidade, BigDecimal precoUnitario) {
        exigirRascunho(o);
        new LinhaOrcamento(descricao, quantidade, unidade, precoUnitario); // só valida
        linha.setDescricao(descricao);
        linha.setQuantidade(quantidade);
        linha.setUnidade(unidade);
        linha.setPrecoUnitario(precoUnitario);
        repositorio.guardar();
    }

    /** Gera o PDF do orçamento (US08). */
    public void gerarPdf(Orcamento o, Path destino) {
        auth.exigirSessao();
        try {
            new GeradorPdf().gerar(o, repositorio.getEmpresa(), destino);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o PDF: " + e.getMessage(), e);
        }
    }
}
