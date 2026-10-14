package domain;

public class Administrador extends Utilizador {
    private static final long serialVersionUID = 1L;

    public Administrador(String nomeUtilizador, String nome, String palavraPasse) {
        super(nomeUtilizador, nome, palavraPasse);
    }

    @Override
    public String getPerfil() {
        return "Administrador";
    }
}
