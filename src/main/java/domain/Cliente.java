package domain;

import java.io.Serializable;

/** Cliente que recebe orçamentos. Subclasses: ClienteParticular e ClienteEmpresa. */
public abstract class Cliente implements Serializable {
    private static final long serialVersionUID = 1L;

    private String codigo;
    private String nome;
    private String nif;
    private String morada;
    private String telefone;
    private String email;

    protected Cliente(String nome, String nif, String morada, String telefone, String email) {
        setNome(nome);
        setNif(nif);
        setMorada(morada);
        this.telefone = telefone;
        this.email = email;
    }

    /** Texto do tipo de cliente. */
    public abstract String getTipo();

    /** Linha "A/C" quando aplicável; null caso contrário. */
    public String getContacto() {
        return null;
    }

    public String getCodigo() {
        return codigo;
    }

    /** Atribuído pelo repositório ao registar o cliente. */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validacao.textoObrigatorio(nome, "Nome");
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = Validacao.nif(nif);
    }

    public String getMorada() {
        return morada;
    }

    public void setMorada(String morada) {
        this.morada = Validacao.textoObrigatorio(morada, "Morada");
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return nome + " (NIF " + nif + ")";
    }
}
