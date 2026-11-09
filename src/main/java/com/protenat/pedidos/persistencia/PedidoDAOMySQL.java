package com.protenat.pedidos.persistencia;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.DetallePedido;
import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.excepciones.BaseDatosException;

/**
 * Implementación MySQL de PedidoDAO sobre las tablas pedido y detalle_pedido.
 */
public class PedidoDAOMySQL extends DAOMySQL implements PedidoDAO {

    private static final String SQL_PEDIDOS =
            "SELECT p.id_pedido, p.fecha, p.total, p.estado, p.estado_pago, p.estado_disponibilidad, "
            + "p.observaciones, c.id_cliente, c.id_usuario, c.cuit_cuil, c.razon_social, "
            + "cc.id_condicion, cc.importe_minimo_compra, cc.fecha_desde, cc.activo "
            + "FROM pedido p "
            + "INNER JOIN cliente c ON c.id_cliente = p.id_cliente "
            + "INNER JOIN condicion_comercial cc ON cc.id_condicion = p.id_condicion ";

    private static final String SQL_DETALLES =
            "SELECT d.id_detalle, d.cantidad, d.precio_unitario, " + ProductoDAOMySQL.COLUMNAS_PRODUCTO
            + "FROM detalle_pedido d "
            + "INNER JOIN producto p ON p.id_producto = d.id_producto "
            + "INNER JOIN linea l ON l.id_linea = p.id_linea "
            + "WHERE d.id_pedido = ? ORDER BY d.id_detalle";

    public PedidoDAOMySQL(ConexionBD conexionBD) {
        super(conexionBD);
    }

    @Override
    protected String getEntidad() {
        return "pedidos";
    }

    @Override
    public void registrar(Pedido pedido) throws BaseDatosException {
        try {
            // Si falla algún detalle se deshace también el pedido
            ejecutarEnTransaccion("registrar el pedido", con -> {

                try (PreparedStatement ps = prepararInsercion(con,
                        "INSERT INTO pedido (id_cliente, id_condicion, fecha, total) VALUES (?, ?, ?, ?)")) {
                    asignarParametros(ps, new Object[] {pedido.getCliente().getIdCliente(),
                        pedido.getCondicionComercial().getIdCondicion(),
                        Timestamp.valueOf(pedido.getFecha()), pedido.getTotal()});
                    ps.executeUpdate();
                    pedido.setIdPedido(claveGenerada(ps));
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
                    for (DetallePedido d : pedido.getDetalles()) {
                        asignarParametros(ps, new Object[] {pedido.getIdPedido(),
                            d.getProducto().getIdProducto(), d.getCantidad(),
                            d.getPrecioUnitario(), d.getSubtotal()});
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            });
        } catch (BaseDatosException e) {
            pedido.setIdPedido(0); // el pedido no quedó registrado
            throw e;
        }
    }

    @Override
    public List<Pedido> listarPorCliente(int idCliente) throws BaseDatosException {
        return consultar(SQL_PEDIDOS + "WHERE p.id_cliente = ? ORDER BY p.fecha DESC",
                PedidoDAOMySQL::crearPedido, idCliente);
    }

    @Override
    public List<Pedido> listar(EstadoPedido estado) throws BaseDatosException {
        if (estado == null) {
            return consultar(SQL_PEDIDOS + "ORDER BY p.fecha DESC", PedidoDAOMySQL::crearPedido);
        }
        return consultar(SQL_PEDIDOS + "WHERE p.estado = ? ORDER BY p.fecha DESC",
                PedidoDAOMySQL::crearPedido, estado.name());
    }

    @Override
    public List<Pedido> listarPendientesPorAntiguedad() throws BaseDatosException {
        return consultar(SQL_PEDIDOS + "WHERE p.estado = 'PENDIENTE' ORDER BY p.fecha ASC",
                PedidoDAOMySQL::crearPedido);
    }

    @Override
    public Pedido buscarPorId(int idPedido) throws BaseDatosException {

        Pedido pedido = consultarUno(SQL_PEDIDOS + "WHERE p.id_pedido = ?", PedidoDAOMySQL::crearPedido, idPedido);
        if (pedido == null) {
            return null;
        }

        List<DetallePedido> detalles = consultar(SQL_DETALLES, rs -> new DetallePedido(
                rs.getInt("id_detalle"),
                ProductoDAOMySQL.crearProducto(rs),
                rs.getInt("cantidad"),
                rs.getBigDecimal("precio_unitario")), idPedido);

        for (DetallePedido d : detalles) {
            pedido.agregarDetalle(d);
        }
        return pedido;
    }

    @Override
    public boolean actualizarVerificaciones(int idPedido, EstadoVerificacion pago,
            EstadoVerificacion disponibilidad, String observaciones) throws BaseDatosException {
        return actualizar("UPDATE pedido SET estado_pago = ?, estado_disponibilidad = ?, observaciones = ? "
                + "WHERE id_pedido = ? AND estado = 'PENDIENTE'",
                pago.name(), disponibilidad.name(), observaciones, idPedido) > 0;
    }

    @Override
    public boolean actualizarEstado(int idPedido, EstadoPedido nuevoEstado, String observaciones,
            boolean exigirAprobadas) throws BaseDatosException {

        String sql = "UPDATE pedido SET estado = ?, observaciones = ? "
                + "WHERE id_pedido = ? AND estado = 'PENDIENTE'";
        if (exigirAprobadas) {
            // La regla de CU-08 se repite en la base por si otro usuario cambió el pedido
            sql += " AND estado_pago = 'APROBADA' AND estado_disponibilidad = 'APROBADA'";
        }
        return actualizar(sql, nuevoEstado.name(), observaciones, idPedido) > 0;
    }

    @Override
    public int[] contarPorEstado() throws BaseDatosException {

        int[] cantidades = new int[EstadoPedido.values().length];

        List<Object[]> filas = consultar("SELECT estado, COUNT(*) AS cantidad FROM pedido GROUP BY estado",
                rs -> new Object[] {rs.getString("estado"), rs.getInt("cantidad")});

        for (Object[] fila : filas) {
            EstadoPedido estado = EstadoPedido.valueOf((String) fila[0]);
            cantidades[estado.ordinal()] = (Integer) fila[1];
        }
        return cantidades;
    }

    private static Pedido crearPedido(ResultSet rs) throws SQLException {

        Cliente cliente = new Cliente(rs.getInt("id_usuario"), null, null, true,
                rs.getString("cuit_cuil"), rs.getString("razon_social"), null, null, null);
        cliente.setIdCliente(rs.getInt("id_cliente"));

        return new Pedido(
                rs.getInt("id_pedido"),
                cliente,
                CondicionComercialDAOMySQL.crearCondicion(rs),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getBigDecimal("total"),
                EstadoPedido.valueOf(rs.getString("estado")),
                EstadoVerificacion.valueOf(rs.getString("estado_pago")),
                EstadoVerificacion.valueOf(rs.getString("estado_disponibilidad")),
                rs.getString("observaciones"));
    }
}
