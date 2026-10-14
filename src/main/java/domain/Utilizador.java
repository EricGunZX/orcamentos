package domain;

import java.io.Serializable;

/** Utilizador da aplicação. Subclasses: Administrador e Operador. */
public abstract class Utilizador implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nomeUtilizador;
    private String nome;
    private String palavraPasse;
    private boolean ativo = true;

    protected Utilizador(String nomeUtilizador, String nome, String palavraPasse) {
        this.nomeUtilizador = Validacao.textoObrigatorio(nomeUtilizador, "Nome de utilizador");
        setNome(nome);
        alterarPalavraPasse(palavraPasse);
    }

    /** Nome do perfil apresentado na interface. */
    public abstract String getPerfil();

    public String getNomeUtilizador() {
        return nomeUtilizador;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validacao.textoObrigatorio(nome, "Nome");
    }

    public void alterarPalavraPasse(String nova) {
        Validacao.palavraPasse(nova);
        this.palavraPasse = nova;
    }

    public boolean verificarPalavraPasse(String tentativa) {
        return palavraPasse.equals(tentativa);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return nome + " (" + getPerfil() + ")";
    }
}
