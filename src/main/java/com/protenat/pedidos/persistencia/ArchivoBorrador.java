package com.protenat.pedidos.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.protenat.pedidos.dominio.DetallePedido;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.excepciones.ArchivoException;

/* Guarda en un archivo de texto el pedido en curso que el cliente todavía no
   envió, para recuperarlo en otra sesión. Cada cliente tiene su archivo
   (borradores/pedido_cliente_N.csv) y cada línea es "código;cantidad".
   Solo se guardan código y cantidad: al recuperarlo se vuelven a tomar el
   precio y la cantidad máxima vigentes de la base.
 */
public class ArchivoBorrador {

    private static final String SEPARADOR = ";";
    private static final String COMENTARIO = "#";

    private final Path carpeta;

    public ArchivoBorrador(Path carpeta) {
        this.carpeta = carpeta;
    }

    public ArchivoBorrador() {
        this(Path.of("borradores"));
    }

    private Path archivoDe(int idCliente) {
        return carpeta.resolve("pedido_cliente_" + idCliente + ".csv");
    }

    public boolean existe(int idCliente) {
        return Files.exists(archivoDe(idCliente));
    }

    /* Fecha y hora en que se guardó el borrador. */
    public LocalDateTime fechaGuardado(int idCliente) throws ArchivoException {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(archivoDe(idCliente)).toInstant(),
                    ZoneId.systemDefault());
        } catch (IOException e) {
            throw new ArchivoException("No se pudo leer el borrador guardado", e);
        }
    }

    /* Escribe (o reemplaza) el borrador con los productos del pedido. */
    public void guardar(Pedido pedido) throws ArchivoException {

        Path archivo = archivoDe(pedido.getCliente().getIdCliente());

        try {
            Files.createDirectories(carpeta);
            try (BufferedWriter out = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
                out.write(COMENTARIO + " Borrador de pedido - " + pedido.getCliente().getRazonSocial());
                out.newLine();
                for (DetallePedido d : pedido.getDetalles()) {
                    out.write(d.getProducto().getCodigo() + SEPARADOR + d.getCantidad());
                    out.newLine();
                }
            }
        } catch (IOException e) {
            throw new ArchivoException("No se pudo guardar el borrador en " + archivo, e);
        }
    }

    /* Lee el borrador. Devuelve una lista (su tamaño depende del archivo) en arreglos de dos posiciones: [0] = código y [1] = cantidad.
     */
    public List<String[]> recuperar(int idCliente) throws ArchivoException {

        List<String[]> lineas = new ArrayList<>();
        Path archivo = archivoDe(idCliente);
        if (!Files.exists(archivo)) {
            return lineas;
        }

        try (BufferedReader in = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = in.readLine()) != null) {
                if (linea.isBlank() || linea.startsWith(COMENTARIO)) {
                    continue;
                }
                String[] campos = linea.split(SEPARADOR);
                if (campos.length != 2) {
                    throw new ArchivoException("El borrador guardado tiene una línea inválida: " + linea, null);
                }
                lineas.add(new String[] {campos[0].trim(), campos[1].trim()});
            }
        } catch (IOException e) {
            throw new ArchivoException("No se pudo leer el borrador " + archivo, e);
        }
        return lineas;
    }

    public void eliminar(int idCliente) throws ArchivoException {
        try {
            Files.deleteIfExists(archivoDe(idCliente));
        } catch (IOException e) {
            throw new ArchivoException("No se pudo eliminar el borrador", e);
        }
    }
}
