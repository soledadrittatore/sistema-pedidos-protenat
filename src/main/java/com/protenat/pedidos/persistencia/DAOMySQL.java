package com.protenat.pedidos.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.protenat.pedidos.excepciones.BaseDatosException;

/**
 * Clase abstracta base de todos los DAO de MySQL. Reúne el código que se
 * repetía en cada consulta: pedir la conexión, cargar los parámetros, recorrer
 * el ResultSet, cerrar los recursos y convertir la SQLException en una
 * BaseDatosException. No se puede instanciar: cada subclase agrega las
 * sentencias de su tabla.
 */
public abstract class DAOMySQL {

    private final ConexionBD conexionBD;

    protected DAOMySQL(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /** Nombre de la entidad que maneja el DAO; se usa en los mensajes de error. */
    protected abstract String getEntidad();

    /**
     * Ejecuta un SELECT y devuelve una fila convertida en objeto por cada
     * resultado. Los parámetros llegan como arreglo (varargs) porque su
     * cantidad se conoce al escribir la sentencia; los resultados se guardan
     * en un ArrayList porque su cantidad depende de la base.
     */
    protected <T> List<T> consultar(String sql, Mapeador<T> mapeador, Object... parametros)
            throws BaseDatosException {

        List<T> resultado = new ArrayList<>();

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            asignarParametros(ps, parametros);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapeador.mapear(rs));
                }
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al consultar " + getEntidad(), e);
        }
        return resultado;
    }

    /** Igual que consultar(), pero para un único resultado; devuelve null si no hay filas. */
    protected <T> T consultarUno(String sql, Mapeador<T> mapeador, Object... parametros)
            throws BaseDatosException {
        List<T> lista = consultar(sql, mapeador, parametros);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve la cantidad de filas afectadas. */
    protected int actualizar(String sql, Object... parametros) throws BaseDatosException {

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            asignarParametros(ps, parametros);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new BaseDatosException("Error al actualizar " + getEntidad(), e);
        }
    }

    /**
     * Ejecuta varias sentencias en una transacción: si todas terminan bien se
     * confirma (commit); si alguna falla se deshace todo (rollback).
     */
    protected void ejecutarEnTransaccion(String descripcion, OperacionTransaccional operacion)
            throws BaseDatosException {

        try (Connection con = conexionBD.obtenerConexion()) {

            con.setAutoCommit(false);
            try {
                operacion.ejecutar(con);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al " + descripcion, e);
        }
    }

    /** Asigna los parámetros en orden: el primero del arreglo reemplaza al primer "?". */
    protected static void asignarParametros(PreparedStatement ps, Object[] parametros)
            throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }

    /** Clave AUTO_INCREMENT generada por el último INSERT. */
    protected static int claveGenerada(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Prepara un INSERT que devuelve la clave generada. */
    protected static PreparedStatement prepararInsercion(Connection con, String sql)
            throws SQLException {
        return con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
}
