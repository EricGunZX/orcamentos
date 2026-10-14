package controller;

import domain.Administrador;
import domain.Utilizador;
import repository.Repositorio;

/** US01: autenticação e sessão. */
public class AutenticacaoController {

    private final Repositorio repositorio;
    private Utilizador sessao;

    public AutenticacaoController(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Utilizador entrar(String nomeUtilizador, String palavraPasse) {
        Utilizador u = repositorio.procurarUtilizador(nomeUtilizador).orElse(null);
        if (u == null || !u.isAtivo() || !u.verificarPalavraPasse(palavraPasse)) {
            throw new SecurityException("Credenciais inválidas.");
        }
        sessao = u;
        return u;
    }

    public void sair() {
        sessao = null;
    }

    public Utilizador getSessao() {
        return sessao;
    }

    public boolean temSessao() {
        return sessao != null;
    }

    public boolean isAdministrador() {
        return sessao instanceof Administrador;
    }

    /** Lança exceção se não houver sessão ativa. */
    public Utilizador exigirSessao() {
        if (sessao == null) {
            throw new SecurityException("É necessário autenticar-se.");
        }
        return sessao;
    }

    /** Lança exceção se o utilizador da sessão não for Administrador. */
    public void exigirAdministrador() {
        exigirSessao();
        if (!isAdministrador()) {
            throw new SecurityException("Apenas o Administrador pode executar esta ação.");
        }
    }
}
