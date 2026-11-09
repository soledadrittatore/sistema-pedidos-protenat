package com.protenat.pedidos.persistencia;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.excepciones.ArchivoException;
import com.protenat.pedidos.util.Formato;

/**
 * Exporta la bandeja de pedidos a un archivo CSV que se abre con Excel o
 * LibreOffice Calc, para que Administración comparta el listado.
 */
public class ExportadorCSV {

    private static final String SEPARADOR = ";";

    // Arreglo: las columnas son fijas y se conocen de antemano
    private static final String[] ENCABEZADO = {"N.° pedido", "Cliente", "CUIT/CUIL", "Fecha",
        "Total", "Estado", "Pago", "Disponibilidad", "Observaciones"};

    /** Escribe una línea por pedido y devuelve la cantidad exportada. */
    public int exportarPedidos(List<Pedido> pedidos, Path destino) throws ArchivoException {

        try (BufferedWriter out = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {

            out.write('﻿'); // marca BOM: Excel reconoce los acentos
            out.write(unir(ENCABEZADO));
            out.newLine();

            String[] fila = new String[ENCABEZADO.length];
            for (Pedido p : pedidos) {
                fila[0] = String.valueOf(p.getIdPedido());
                fila[1] = p.getCliente().getRazonSocial();
                fila[2] = p.getCliente().getCuitCuil();
                fila[3] = Formato.fechaHora(p.getFecha());
                fila[4] = p.getTotal().toPlainString();
                fila[5] = p.getEstado().name();
                fila[6] = p.getEstadoPago().name();
                fila[7] = p.getEstadoDisponibilidad().name();
                fila[8] = p.getObservaciones() == null ? "" : p.getObservaciones();
                out.write(unir(fila));
                out.newLine();
            }

        } catch (IOException e) {
            throw new ArchivoException("No se pudo escribir el archivo " + destino, e);
        }
        return pedidos.size();
    }

    // Une los campos con ";" y encierra entre comillas los que tienen el separador
    private static String unir(String[] campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            String campo = campos[i] == null ? "" : campos[i];
            if (campo.contains(SEPARADOR) || campo.contains("\"")) {
                campo = "\"" + campo.replace("\"", "\"\"") + "\"";
            }
            sb.append(campo);
        }
        return sb.toString();
    }
}
