package br.edu.registrofuncionarios.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;


public final class Formatos {

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Formatos() {
    }

    public static String formatarData(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

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

    public static String formatarMoeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    public static String formatarNumero(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }


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
