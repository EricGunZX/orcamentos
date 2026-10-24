package domain;

public class ClienteEmpresa extends Cliente {
    private static final long serialVersionUID = 1L;

    /** Pessoa de contacto (A/C). */
    private String contacto;

    public ClienteEmpresa(String designacaoSocial, String nif, String morada,
                          String telefone, String email, String contacto) {
        super(designacaoSocial, nif, morada, telefone, email);
        this.contacto = contacto;
    }

    @Override
    public String getTipo() {
        return "Empresa";
    }

    @Override
    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }
}
