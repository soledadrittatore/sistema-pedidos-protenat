package com.protenat.pedidos.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
 * Registra los errores técnicos en el archivo protenat.log. Al usuario solo se
 * le muestra un mensaje general.
 */
public final class Registro {

    private static final Logger LOG = Logger.getLogger("protenat");

    static {
        try {
            FileHandler archivo = new FileHandler("protenat.log", true);
            archivo.setFormatter(new SimpleFormatter());
            LOG.addHandler(archivo);
            LOG.setUseParentHandlers(false); // no mostrar el error técnico en la consola
        } catch (IOException e) {
            System.err.println("No se pudo crear el archivo de log: " + e.getMessage());
        }
    }

    private Registro() {
    }

    public static void error(String mensaje, Throwable causa) {
        LOG.log(Level.SEVERE, mensaje, causa);
    }
}
