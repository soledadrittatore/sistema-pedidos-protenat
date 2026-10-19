package com.protenat.pedidos.control;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.dominio.Producto;


/* Coordina CU-09 Gestionar condiciones comerciales, CU-11 Modificar importe
   mínimo de compra y CU-12 Modificar cantidad máxima por producto. */
public class ControlCondicionComercial {

    public static final String MSG_VALOR_INVALIDO = "Ingresá un valor numérico mayor que cero.";

    private final ConexionBD conexionBD;

    public ControlCondicionComercial(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /* Consulta la condición comercial vigente (activo = 1) */
    public CondicionComercial consultarCondiciones() throws BaseDatosException {

        String sql = "SELECT id_condicion, importe_minimo_compra, fecha_desde, activo "
                + "FROM condicion_comercial WHERE activo = 1";

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                throw new BaseDatosException("No hay una condición comercial vigente", null);
            }

            return new CondicionComercial(
                    rs.getInt("id_condicion"),
                    rs.getBigDecimal("importe_minimo_compra"),
                    rs.getDate("fecha_desde").toLocalDate(),
                    rs.getBoolean("activo"));

        } catch (SQLException e) {
            throw new BaseDatosException("Error al consultar la condición comercial vigente", e);
        }
    }

    /*  CU-11. No sobrescribe la condición anterior: la desactiva y registra una
        nueva, así cada pedido conserva la condición con la que se validó. */
    public CondicionComercial modificarImporteMinimo(BigDecimal nuevoImporte)
            throws ValidacionException, BaseDatosException {

        if (nuevoImporte == null || nuevoImporte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException(MSG_VALOR_INVALIDO);
        }

        try (Connection con = conexionBD.obtenerConexion()) {

            con.setAutoCommit(false);

            try (PreparedStatement baja = con.prepareStatement(
                         "UPDATE condicion_comercial SET activo = 0 WHERE activo = 1");
                 PreparedStatement alta = con.prepareStatement(
                         "INSERT INTO condicion_comercial (importe_minimo_compra, fecha_desde, activo) "
                         + "VALUES (?, CURDATE(), 1)")) {

                baja.executeUpdate();
                alta.setBigDecimal(1, nuevoImporte);
                alta.executeUpdate();
                con.commit();

            } catch (SQLException e) {
                // permite conservar el importe anterior
                con.rollback(); 
                throw e;
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al modificar el importe mínimo", e);
        }

        return consultarCondiciones();
    }

    /* CU-12. Modifica la cantidad máxima permitida para un producto */
    public void modificarCantidadMaxima(Producto producto, int nuevaCantidad)
            throws ValidacionException, BaseDatosException {

        if (nuevaCantidad <= 0) {
            throw new ValidacionException(MSG_VALOR_INVALIDO);
        }

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE producto SET cantidad_maxima = ? WHERE id_producto = ?")) {

            ps.setInt(1, nuevaCantidad);
            ps.setInt(2, producto.getIdProducto());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new BaseDatosException("Error al modificar la cantidad máxima de " + producto.getCodigo(), e);
        }

        /* Se actualiza el objeto solo si la base se modificó sin errores */
        producto.setCantidadMaxima(nuevaCantidad);
    }
}
