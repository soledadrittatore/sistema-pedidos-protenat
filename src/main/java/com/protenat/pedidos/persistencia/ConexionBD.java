package com.protenat.pedidos.persistencia;

import com.protenat.pedidos.excepciones.BaseDatosException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Crea las conexiones a MySQL a partir de config.properties. Cada operación
 * pide una conexión nueva y la cierra con try-with-resources.
 */
public final class ConexionBD {

    private static final String ARCHIVO = "config.properties";

    private final Properties props = new Properties();
    private final String url;
    private final String usuario;
    private final String clave;

    public ConexionBD() throws BaseDatosException {
        try (InputStream in = abrirConfiguracion()) {
            if (in == null) {
                throw new BaseDatosException("No se encontró " + ARCHIVO, null);
            }
            props.load(in);
        } catch (IOException e) {
            throw new BaseDatosException("No se pudo leer " + ARCHIVO, e);
        }
        this.url = props.getProperty("db.url");
        this.usuario = props.getProperty("db.usuario");
        this.clave = props.getProperty("db.clave");
    }

    /** Prioriza el archivo externo (junto al JAR) y si no existe usa el interno. */
    private InputStream abrirConfiguracion() throws IOException {
        Path externo = Path.of(ARCHIVO);
        if (Files.exists(externo)) {
            return new FileInputStream(externo.toFile());
        }
        return ConexionBD.class.getClassLoader().getResourceAsStream(ARCHIVO);
    }

    /** Devuelve otra opción de config.properties, por ejemplo ui.escala. */
    public String getPropiedad(String clave, String porDefecto) {
        return props.getProperty(clave, porDefecto).trim();
    }

    public Connection obtenerConexion() throws BaseDatosException {
        try {
            return DriverManager.getConnection(url, usuario, clave);
        } catch (SQLException e) {
            throw new BaseDatosException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Comprueba que el servidor responda. Si no, lanza la excepción con la
     * causa de MySQL, para que quede registrada en protenat.log.
     */
    public void verificarConexion() throws BaseDatosException {
        try (Connection c = obtenerConexion()) {
            if (!c.isValid(3)) {
                throw new BaseDatosException("El servidor MySQL no responde", null);
            }
        } catch (SQLException e) {
            throw new BaseDatosException("El servidor MySQL no responde: " + e.getMessage(), e);
        }
    }
}
