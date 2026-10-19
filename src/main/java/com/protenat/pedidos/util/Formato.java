package com.protenat.pedidos.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/* Formato de importes y fechas en pantalla. */
public final class Formato {

    private static final DecimalFormat MONEDA =
            new DecimalFormat("$ #,##0.00", DecimalFormatSymbols.getInstance(new Locale("es", "AR")));
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    public static String moneda(BigDecimal importe) {
        return MONEDA.format(importe);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha.format(FECHA_HORA);
    }

    /** Recorta o completa con espacios para armar columnas en la consola. */
    public static String columna(String texto, int ancho) {
        String t = texto == null ? "" : texto;
        if (t.length() > ancho) {
            return t.substring(0, ancho - 1) + "…";
        }
        return String.format("%-" + ancho + "s", t);
    }
}
