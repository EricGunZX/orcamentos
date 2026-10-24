package domain;

public class ClienteParticular extends Cliente {
    private static final long serialVersionUID = 1L;

    /** Cliente em branco, para preencher diretamente no orçamento. */
    public ClienteParticular() {
        super();
    }

    public ClienteParticular(String nome, String nif, String morada, String telefone, String email) {
        super(nome, nif, morada, telefone, email);
    }

    @Override
    public String getTipo() {
        return "Particular";
    }
}
