package br.edu.registrofuncionarios.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Regras de validação e normalização reutilizadas pelas classes do modelo.
 */
public final class Validador {

    private static final Pattern MATRICULA = Pattern.compile("\\d{6}");
    private static final Pattern CPF = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}");
    private static final Pattern CEP = Pattern.compile("\\d{5}-\\d{3}|\\d{8}");
    private static final Pattern NOME = Pattern.compile("\\p{L}[\\p{L} .'-]*");

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB",
            "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private Validador() {
    }

    /** Remove espaços nas pontas, colapsa espaços/quebras de linha repetidos e trata null como "". */
    public static String normalizarTexto(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
    }

    /** Matrícula: exatamente 6 dígitos numéricos. */
    public static boolean matriculaValida(String matricula) {
        return matricula != null && MATRICULA.matcher(matricula.strip()).matches();
    }

    /** Nome: começa com letra e contém apenas letras, espaços, ponto, apóstrofo e hífen. */
    public static boolean nomeComCaracteresValidos(String nomeNormalizado) {
        return NOME.matcher(nomeNormalizado).matches();
    }

    /** CPF: formato 000.000.000-00 ou 11 dígitos, rejeita sequências repetidas e confere os 2 dígitos verificadores. */
    public static boolean cpfValido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String texto = cpf.strip();
        if (!CPF.matcher(texto).matches()) {
            return false;
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }
        return calcularDigito(digitos, 9) == digitos.charAt(9) - '0'
                && calcularDigito(digitos, 10) == digitos.charAt(10) - '0';
    }

    private static int calcularDigito(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Converte um CPF válido para o formato 000.000.000-00. */
    public static String normalizarCpf(String cpf) {
        String d = cpf.replaceAll("\\D", "");
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** CEP: formato 00000-000 ou 8 dígitos. */
    public static boolean cepValido(String cep) {
        return cep != null && CEP.matcher(cep.strip()).matches();
    }

    /** Converte um CEP válido para o formato 00000-000. */
    public static String normalizarCep(String cep) {
        String d = cep.replaceAll("\\D", "");
        return d.substring(0, 5) + "-" + d.substring(5);
    }

    /** UF: sigla de um dos 27 estados brasileiros (maiúsculas ou minúsculas). */
    public static boolean ufValida(String uf) {
        return uf != null && UFS.contains(uf.strip().toUpperCase(Locale.ROOT));
    }

    /** Idade completa, em anos, na data de referência. */
    public static int idade(LocalDate nascimento, LocalDate referencia) {
        return Period.between(nascimento, referencia).getYears();
    }
}
