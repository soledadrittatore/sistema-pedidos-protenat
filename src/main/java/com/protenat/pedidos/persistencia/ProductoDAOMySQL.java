package com.protenat.pedidos.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.protenat.pedidos.dominio.Linea;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.BaseDatosException;

/* Implementación MySQL de ProductoDAO sobre las tablas producto y linea. */
public class ProductoDAOMySQL extends DAOMySQL implements ProductoDAO {

    static final String COLUMNAS_PRODUCTO =
            "p.id_producto, p.codigo, p.nombre, p.descripcion, p.presentacion, p.precio, "
            + "p.cantidad_maxima, p.activo, l.id_linea, l.nombre AS linea, l.descripcion AS linea_desc ";

    public ProductoDAOMySQL(ConexionBD conexionBD) {
        super(conexionBD);
    }

    @Override
    protected String getEntidad() {
        return "productos";
    }

    @Override
    public List<Producto> listarActivos() throws BaseDatosException {
        return consultar("SELECT " + COLUMNAS_PRODUCTO
                + "FROM producto p INNER JOIN linea l ON l.id_linea = p.id_linea WHERE p.activo = 1",
                ProductoDAOMySQL::crearProducto);
    }

    @Override
    public boolean actualizarCantidadMaxima(int idProducto, int cantidadMaxima) throws BaseDatosException {
        return actualizar("UPDATE producto SET cantidad_maxima = ? WHERE id_producto = ?",
                cantidadMaxima, idProducto) > 0;
    }

    /* También lo usa PedidoDAOMySQL al leer los detalles de un pedido. */
    static Producto crearProducto(ResultSet rs) throws SQLException {
        Linea linea = new Linea(rs.getInt("id_linea"), rs.getString("linea"), rs.getString("linea_desc"));
        return new Producto(
                rs.getInt("id_producto"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("presentacion"),
                rs.getBigDecimal("precio"),
                rs.getInt("cantidad_maxima"),
                rs.getBoolean("activo"),
                linea);
    }
}
