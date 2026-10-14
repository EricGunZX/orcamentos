package controller;

import domain.Administrador;
import domain.Empresa;
import domain.Operador;
import domain.Utilizador;
import repository.Repositorio;

import java.math.BigDecimal;

/** US02 e US03: configuração do sistema e gestão de utilizadores. */
public class AdministracaoController {

    private final Repositorio repositorio;
    private final AutenticacaoController auth;

    public AdministracaoController(Repositorio repositorio, AutenticacaoController auth) {
        this.repositorio = repositorio;
        this.auth = auth;
    }

    public Empresa getEmpresa() {
        return repositorio.getEmpresa();
    }

    public void configurarEmpresa(String designacao, String nif, String morada,
                                  String telefone, String email, String website) {
        auth.exigirAdministrador();
        Empresa e = repositorio.getEmpresa();
        e.setDesignacao(designacao);
        e.setNif(nif);
        e.setMorada(morada);
        e.setTelefone(telefone);
        e.setEmail(email);
        e.setWebsite(website);
        repositorio.guardar();
    }

    public void configurarParametros(BigDecimal taxaIva, String condicoesPagamento, int diasValidade) {
        auth.exigirAdministrador();
        Empresa e = repositorio.getEmpresa();
        e.setTaxaIva(taxaIva);
        e.setCondicoesPagamentoPorDefeito(condicoesPagamento);
        e.setDiasValidadePorDefeito(diasValidade);
        repositorio.guardar();
    }

    public Operador criarOperador(String nomeUtilizador, String nome, String palavraPasse) {
        auth.exigirAdministrador();
        Operador o = new Operador(nomeUtilizador, nome, palavraPasse);
        repositorio.adicionarUtilizador(o);
        repositorio.guardar();
        return o;
    }

    public Administrador criarAdministrador(String nomeUtilizador, String nome, String palavraPasse) {
        auth.exigirAdministrador();
        Administrador a = new Administrador(nomeUtilizador, nome, palavraPasse);
        repositorio.adicionarUtilizador(a);
        repositorio.guardar();
        return a;
    }

    public void desativarUtilizador(String nomeUtilizador) {
        auth.exigirAdministrador();
        Utilizador u = repositorio.procurarUtilizador(nomeUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        if (u instanceof Administrador && u.isAtivo() && repositorio.contarAdministradoresAtivos() <= 1) {
            throw new IllegalStateException("Não é possível desativar o último Administrador.");
        }
        u.setAtivo(false);
        repositorio.guardar();
    }

    public void definirLogotipo(String caminho) {
        auth.exigirAdministrador();
        repositorio.getEmpresa().setCaminhoLogotipo(caminho == null || caminho.isBlank() ? null : caminho);
        repositorio.guardar();
    }

    public void ativarUtilizador(String nomeUtilizador) {
        auth.exigirAdministrador();
        Utilizador u = repositorio.procurarUtilizador(nomeUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        u.setAtivo(true);
        repositorio.guardar();
    }

    /** Cria o primeiro Administrador quando ainda não existe nenhum utilizador. */
    public void criarAdministradorInicial(String nomeUtilizador, String nome, String palavraPasse) {
        if (!repositorio.getUtilizadores().isEmpty()) {
            throw new IllegalStateException("Já existem utilizadores registados.");
        }
        repositorio.adicionarUtilizador(new Administrador(nomeUtilizador, nome, palavraPasse));
        repositorio.guardar();
    }
}
