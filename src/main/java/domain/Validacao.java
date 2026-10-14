package domain;

/** Regras de validação partilhadas pelo modelo. */
public final class Validacao {

    private Validacao() {
    }

    public static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor.trim();
    }

    /** NIF português: 9 dígitos. */
    public static String nif(String valor) {
        String limpo = textoObrigatorio(valor, "NIF").replace(" ", "");
        if (!limpo.matches("\\d{9}")) {
            throw new IllegalArgumentException("O NIF deve ter 9 dígitos.");
        }
        return limpo;
    }

    /**
     * Palavra-passe (US01): no máximo 8 caracteres, com pelo menos
     * uma letra, um número e um símbolo.
     */
    public static void palavraPasse(String valor) {
        if (valor == null || valor.isEmpty() || valor.length() > 8) {
            throw new IllegalArgumentException("A palavra-passe deve ter no máximo 8 caracteres.");
        }
        boolean letra = valor.chars().anyMatch(Character::isLetter);
        boolean numero = valor.chars().anyMatch(Character::isDigit);
        boolean simbolo = valor.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!(letra && numero && simbolo)) {
            throw new IllegalArgumentException(
                    "A palavra-passe deve incluir letras, números e símbolos.");
        }
    }
}
