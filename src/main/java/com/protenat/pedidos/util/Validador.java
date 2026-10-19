package com.protenat.pedidos.util;

import java.util.regex.Pattern;

/**
 * Validaciones de formato de los datos ingresados (sección 14.5.3).
 */
public final class Validador {

    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern CUIT = Pattern.compile("^\\d{2}-\\d{8}-\\d$");

    private Validador() {
        // clase de utilidades: no se instancia
    }

    public static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static boolean esEmailValido(String email) {
        return !esVacio(email) && EMAIL.matcher(email.trim()).matches();
    }

    /**
     * CUIT/CUIL con formato 30-71234567-8 y dígito verificador correcto
     * (algoritmo de módulo 11 de AFIP).
     */
    public static boolean esCuitValido(String cuit) {
        if (esVacio(cuit) || !CUIT.matcher(cuit.trim()).matches()) {
            return false;
        }
        String digitos = cuit.replace("-", "");
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < pesos.length; i++) {
            suma += Character.getNumericValue(digitos.charAt(i)) * pesos[i];
        }
        int resto = 11 - (suma % 11);
        int verificador;
        if (resto == 11) {
            verificador = 0;
        } else if (resto == 10) {
            verificador = 9;
        } else {
            verificador = resto;
        }
        return verificador == Character.getNumericValue(digitos.charAt(10));
    }
}
