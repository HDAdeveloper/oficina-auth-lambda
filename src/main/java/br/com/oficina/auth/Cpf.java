package br.com.oficina.auth;

/** Normalização e validação de CPF (dígitos verificadores). */
public final class Cpf {

    private Cpf() {
    }

    /** Só os dígitos; devolve {@code null} se vier nulo. */
    public static String normalizar(String texto) {
        return texto == null ? null : texto.replaceAll("\\D", "");
    }

    /** Verdadeiro se o texto (com ou sem pontuação) é um CPF válido. */
    public static boolean valido(String texto) {
        String cpf = normalizar(texto);
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digito(cpf, 9) == cpf.charAt(9) - '0' && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digito(String cpf, int quantos) {
        int soma = 0;
        for (int i = 0; i < quantos; i++) {
            soma += (cpf.charAt(i) - '0') * (quantos + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
