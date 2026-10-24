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
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += (limpo.charAt(i) - '0') * (9 - i);
        }
        int controlo = 11 - (soma % 11);
        if (controlo >= 10) {
            controlo = 0;
        }
        if (controlo != limpo.charAt(8) - '0') {
            throw new IllegalArgumentException("O NIF é inválido (o dígito de controlo não confere).");
        }
        return limpo;
    }

    /** E-mail opcional: se preenchido, tem de ter formato válido. */
    public static String emailOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        String v = valor.trim();
        if (!v.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}")) {
            throw new IllegalArgumentException("O e-mail é inválido.");
        }
        return v;
    }

    /** Telefone opcional: 9 a 15 dígitos, aceitando espaços e um + inicial. */
    public static String telefoneOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        String v = valor.trim();
        if (!v.replace(" ", "").matches("\\+?\\d{9,15}")) {
            throw new IllegalArgumentException("O telefone é inválido (9 a 15 dígitos).");
        }
        return v;
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
