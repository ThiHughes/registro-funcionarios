package br.edu.registrofuncionarios.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Conversões e formatações (datas dd/MM/aaaa e valores monetários em pt-BR).
 */
public final class Formatos {

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Formatos() {
    }

    /** Formata como dd/MM/aaaa; retorna "" para null. */
    public static String formatarData(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

    /** Lê uma data no formato dd/MM/aaaa; retorna null se vazia ou inválida. */
    public static LocalDate parseData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.strip(), DATA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Formata como moeda brasileira, por exemplo "R$ 3.500,00". */
    public static String formatarMoeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    /** Formata apenas o número com 2 casas e vírgula decimal, por exemplo "3500,50". */
    public static String formatarNumero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Lê um valor monetário digitado pelo usuário. Aceita "3500", "3500.50", "3500,50", "3.500,50" e "R$ 3.500,50".
     *
     * @throws NumberFormatException se o texto não for um número não negativo válido
     */
    public static BigDecimal parseMoeda(String texto) {
        if (texto == null) {
            throw new NumberFormatException("Valor vazio.");
        }
        String t = texto.replace("R$", "").replace("\u00A0", "").replaceAll("\\s", "");
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        } else if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
            t = t.replace(".", "");
        }
        if (!t.matches("\\d+(\\.\\d+)?")) {
            throw new NumberFormatException("Valor inválido: " + texto);
        }
        return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
    }
}
