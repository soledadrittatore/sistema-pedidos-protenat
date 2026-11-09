package com.protenat.pedidos.persistencia;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.excepciones.BaseDatosException;

/**
 * Implementación MySQL de CondicionComercialDAO sobre la tabla
 * condicion_comercial.
 */
public class CondicionComercialDAOMySQL extends DAOMySQL implements CondicionComercialDAO {

    private static final String SQL_CONDICION =
            "SELECT id_condicion, importe_minimo_compra, fecha_desde, activo FROM condicion_comercial ";

    public CondicionComercialDAOMySQL(ConexionBD conexionBD) {
        super(conexionBD);
    }

    @Override
    protected String getEntidad() {
        return "condiciones comerciales";
    }

    @Override
    public CondicionComercial buscarVigente() throws BaseDatosException {
        return consultarUno(SQL_CONDICION + "WHERE activo = 1", CondicionComercialDAOMySQL::crearCondicion);
    }

    @Override
    public void reemplazarVigente(BigDecimal nuevoImporte) throws BaseDatosException {
        ejecutarEnTransaccion("modificar el importe mínimo", con -> {
            try (PreparedStatement baja = con.prepareStatement(
                         "UPDATE condicion_comercial SET activo = 0 WHERE activo = 1");
                 PreparedStatement alta = con.prepareStatement(
                         "INSERT INTO condicion_comercial (importe_minimo_compra, fecha_desde, activo) "
                         + "VALUES (?, CURDATE(), 1)")) {
                baja.executeUpdate();
                alta.setBigDecimal(1, nuevoImporte);
                alta.executeUpdate();
            }
        });
    }

    @Override
    public List<CondicionComercial> listarHistorial() throws BaseDatosException {
        return consultar(SQL_CONDICION + "ORDER BY id_condicion DESC", CondicionComercialDAOMySQL::crearCondicion);
    }

    /** También lo usa PedidoDAOMySQL para la condición con la que se validó cada pedido. */
    static CondicionComercial crearCondicion(ResultSet rs) throws SQLException {
        return new CondicionComercial(
                rs.getInt("id_condicion"),
                rs.getBigDecimal("importe_minimo_compra"),
                rs.getDate("fecha_desde").toLocalDate(),
                rs.getBoolean("activo"));
    }
}
