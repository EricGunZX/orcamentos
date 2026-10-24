package domain;

public class Operador extends Utilizador {
    private static final long serialVersionUID = 1L;

    public Operador(String nomeUtilizador, String nome, String palavraPasse) {
        super(nomeUtilizador, nome, palavraPasse);
    }

    @Override
    public String getPerfil() {
        return "Operador";
    }
}
