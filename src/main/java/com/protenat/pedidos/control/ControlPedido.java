package com.protenat.pedidos.control;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.dominio.DetallePedido;
import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Linea;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.util.Formato;
import com.protenat.pedidos.util.Ordenamiento;

/* Coordina la gestión de pedidos: CU-03, CU-04 y CU-10 para el cliente, y
   CU-05 a CU-08 para la consulta, verificación y resolución. */
public class ControlPedido {

    private static final String SQL_PRODUCTOS =
            "SELECT p.id_producto, p.codigo, p.nombre, p.descripcion, p.presentacion, p.precio, "
            + "p.cantidad_maxima, p.activo, l.id_linea, l.nombre AS linea, l.descripcion AS linea_desc "
            + "FROM producto p INNER JOIN linea l ON l.id_linea = p.id_linea ";

    private static final String SQL_PEDIDOS =
            "SELECT p.id_pedido, p.fecha, p.total, p.estado, p.estado_pago, p.estado_disponibilidad, "
            + "p.observaciones, c.id_cliente, c.id_usuario, c.cuit_cuil, c.razon_social, "
            + "cc.id_condicion, cc.importe_minimo_compra, cc.fecha_desde, cc.activo "
            + "FROM pedido p "
            + "INNER JOIN cliente c ON c.id_cliente = p.id_cliente "
            + "INNER JOIN condicion_comercial cc ON cc.id_condicion = p.id_condicion ";

    /*  Criterios de ordenación del catálogo que ofrece la pantalla */
    public static final Comparator<Producto> POR_CODIGO = Comparator.comparing(Producto::getCodigo);
    public static final Comparator<Producto> POR_NOMBRE =
            Comparator.comparing(Producto::getNombre).thenComparing(Producto::getPresentacion);
    public static final Comparator<Producto> POR_PRECIO = Comparator.comparing(Producto::getPrecio);
    
    /*  Cada cambio del pedido en curso se guarda para poder deshacerlo */
    private static class Movimiento {
        private final Producto producto;
        private final int cantidadAnterior; // 0 = el producto no estaba en el pedido

        Movimiento(Producto producto, int cantidadAnterior) {
            this.producto = producto;
            this.cantidadAnterior = cantidadAnterior;
        }
    }

    private final ConexionBD conexionBD;
    private final ControlCondicionComercial controlCondicion;

    private Pedido pedidoEnCurso;
    private final Deque<Movimiento> historial = new ArrayDeque<>(); // pila (LIFO)

    public ControlPedido(ConexionBD conexionBD, ControlCondicionComercial controlCondicion) {
        this.conexionBD = conexionBD;
        this.controlCondicion = controlCondicion;
    }

   
    /*  CU-03 Consultar productos
       Productos activos, ordenados por código con ordenación por inserción */
    public List<Producto> consultarProductos() throws BaseDatosException {

        List<Producto> productos = new ArrayList<>();

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_PRODUCTOS + "WHERE p.activo = 1");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productos.add(crearProducto(rs));
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al consultar los productos", e);
        }

        Ordenamiento.ordenarPorInsercion(productos, POR_CODIGO);
        return productos;
    }

    /* Ordena el catálogo con el criterio que elige el usuario */
    public void ordenarProductos(List<Producto> productos, Comparator<Producto> criterio,
                                 boolean descendente) { 
        Ordenamiento.ordenarPorInsercion(productos, descendente ? criterio.reversed() : criterio);
    }

    /*  Busca un producto por código con búsqueda binaria (la lista se ordena antes) */
    public Producto buscarProducto(List<Producto> productos, String codigo)
            throws ValidacionException {

        if (codigo == null || codigo.isBlank()) {
            throw new ValidacionException("Ingresá el código del producto que querés buscar.");
        }

        List<Producto> ordenados = new ArrayList<>(productos);
        Ordenamiento.ordenarPorInsercion(ordenados, POR_CODIGO);

        Producto encontrado = Ordenamiento.busquedaBinaria(ordenados, codigo.trim(), Producto::getCodigo);
        if (encontrado == null) {
            throw new ValidacionException("No existe un producto habilitado con el código "
                    + codigo.trim().toUpperCase() + ".");
        }
        return encontrado;
    }

    
    /* CU-04 Gestionar pedido y CU-10 Validar condiciones comerciales */
 
    public Pedido iniciarPedido(Cliente cliente) throws BaseDatosException {
        CondicionComercial vigente = controlCondicion.consultarCondiciones();
        pedidoEnCurso = new Pedido(cliente, vigente);
        historial.clear();
        return pedidoEnCurso;
    }

    /*  CU-10 paso 3: la cantidad debe estar entre 1 y la cantidad máxima */
    public boolean validarCantidad(Producto producto, int cantidad) {

        if (cantidad <= 0) {
            return false;
        }

        return cantidad <= producto.getCantidadMaxima();
    }

    /* CU-10 paso 6: el total debe alcanzar o superar el importe mínimo */
    public boolean validarImporteMinimo(Pedido pedido, CondicionComercial condicion) {
        return pedido.getTotal().compareTo(condicion.getImporteMinimoCompra()) >= 0;
    }

    /* Incorpora el producto al pedido. Si ya estaba, suma la cantidad
     * y valida el total contra la cantidad máxima (CU-10 pasos 2 a 4). */
    public void agregarProducto(Pedido pedido, Producto producto, int cantidad)
            throws ValidacionException {

        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que cero.");
        }

        DetallePedido existente = pedido.buscarDetalle(producto.getCodigo());
        int anterior = (existente == null) ? 0 : existente.getCantidad();
        int nuevaCantidad = anterior + cantidad;

        if (!validarCantidad(producto, nuevaCantidad)) {
            String mensaje = "La cantidad máxima por pedido para este producto es de "
                    + producto.getCantidadMaxima() + " unidades.";
            if (anterior > 0) {
                mensaje += " Ya tenés " + anterior + " en el pedido.";
            }
            throw new ValidacionException(mensaje);
        }

        if (existente == null) {
            pedido.agregarDetalle(new DetallePedido(producto, cantidad));
        } else {
            pedido.modificarDetalle(existente, nuevaCantidad);
        }

        historial.push(new Movimiento(producto, anterior));
    }

    /*  CU-04 A3: quita un producto del pedido */
    public void quitarProducto(Pedido pedido, String codigo) throws ValidacionException {

        DetallePedido detalle = pedido.buscarDetalle(codigo);
        if (detalle == null) {
            throw new ValidacionException("Seleccioná un producto del pedido para quitarlo.");
        }

        historial.push(new Movimiento(detalle.getProducto(), detalle.getCantidad()));
        pedido.eliminarDetalle(detalle);
    }

    /* Deshace el último cambio tomándolo del tope de la pila */
    public String deshacerUltimoCambio(Pedido pedido) throws ValidacionException {

        if (historial.isEmpty()) {
            throw new ValidacionException("No hay cambios para deshacer.");
        }

        Movimiento ultimo = historial.pop();
        DetallePedido actual = pedido.buscarDetalle(ultimo.producto.getCodigo());

        if (ultimo.cantidadAnterior == 0) {
            pedido.eliminarDetalle(actual);
        } else if (actual == null) {
            pedido.agregarDetalle(new DetallePedido(ultimo.producto, ultimo.cantidadAnterior));
        } else {
            pedido.modificarDetalle(actual, ultimo.cantidadAnterior);
        }

        return ultimo.producto.getCodigo() + " vuelve a " + ultimo.cantidadAnterior + " unidad(es).";
    }

    /* Importe que le falta al pedido para llegar al mínimo (cero si ya lo alcanzó) */
    public BigDecimal calcularFaltante(Pedido pedido) {
        BigDecimal falta = pedido.getCondicionComercial().getImporteMinimoCompra()
                .subtract(pedido.getTotal());
        return falta.max(BigDecimal.ZERO);
    }

    /* CU-10 pasos 5 a 7, antes de mostrar el resumen */
    public void validarPedido(Pedido pedido) throws ValidacionException {

        if (pedido.estaVacio()) {
            throw new ValidacionException("El pedido no tiene productos.");
        }

        if (!validarImporteMinimo(pedido, pedido.getCondicionComercial())) {
            throw new ValidacionException("El importe mínimo de compra es "
                    + Formato.moneda(pedido.getCondicionComercial().getImporteMinimoCompra())
                    + ". Te faltan " + Formato.moneda(calcularFaltante(pedido)) + ".");
        }
    }

    /* CU-04 pasos 11 a 13: valida y registra el pedido; devuelve su número */
    public int generarPedido(Pedido pedido) throws ValidacionException, BaseDatosException {
        validarPedido(pedido);
        registrarPedido(pedido);
        cancelarGestion();
        return pedido.getIdPedido();
    }

    /* Registra el pedido y todos sus detalles en una misma transacción */
    public void registrarPedido(Pedido pedido) throws BaseDatosException {

        try (Connection con = conexionBD.obtenerConexion()) {

            con.setAutoCommit(false);

            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO pedido (id_cliente, id_condicion, fecha, total) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, pedido.getCliente().getIdCliente());
                    ps.setInt(2, pedido.getCondicionComercial().getIdCondicion());
                    ps.setTimestamp(3, Timestamp.valueOf(pedido.getFecha()));
                    ps.setBigDecimal(4, pedido.getTotal());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        pedido.setIdPedido(rs.getInt(1));
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
                    for (DetallePedido d : pedido.getDetalles()) {
                        ps.setInt(1, pedido.getIdPedido());
                        ps.setInt(2, d.getProducto().getIdProducto());
                        ps.setInt(3, d.getCantidad());
                        ps.setBigDecimal(4, d.getPrecioUnitario());
                        ps.setBigDecimal(5, d.getSubtotal());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                con.commit();

            } catch (SQLException e) {
                con.rollback(); // no queda un pedido sin sus detalles
                pedido.setIdPedido(0);
                throw e;
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al registrar el pedido", e);
        }
    }

    /* CU-04 A4: se descarta el pedido en curso sin registrarlo */
    public void cancelarGestion() {
        pedidoEnCurso = null;
        historial.clear();
    }

    public Pedido getPedidoEnCurso() {
        return pedidoEnCurso;
    }

    /* Consultar mis pedidos y Gestionar pedidos recibidos */
    /* pedidos del cliente, del más reciente al más antiguo */
    public List<Pedido> consultarPedidosCliente(Cliente cliente) throws BaseDatosException {
        return listarPedidos("WHERE p.id_cliente = ? ORDER BY p.fecha DESC", cliente.getIdCliente());
    }

    // CU-06: todos los pedidos o solo los de un estado
    public List<Pedido> consultarPedidos(EstadoPedido filtroEstado) throws BaseDatosException {
        if (filtroEstado == null) {
            return listarPedidos("ORDER BY p.fecha DESC", null);
        }
        return listarPedidos("WHERE p.estado = ? ORDER BY p.fecha DESC", filtroEstado.name());
    }

    /* filtra por número de pedido o razón social (búsqueda secuencial) */
    public List<Pedido> filtrarPedidos(List<Pedido> pedidos, String texto) {

        if (texto == null || texto.isBlank()) {
            return pedidos;
        }

        String buscado = texto.trim().toLowerCase();
        List<Pedido> resultado = new ArrayList<>();

        for (Pedido p : pedidos) {
            boolean coincideNumero = String.valueOf(p.getIdPedido()).equals(buscado);
            boolean coincideCliente = p.getCliente().getRazonSocial().toLowerCase().contains(buscado);
            if (coincideNumero || coincideCliente) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /* Pedidos pendientes, primero que llegó es el primero que atiende el personal administrativo. */
    public Queue<Pedido> obtenerColaPendientes() throws BaseDatosException {
        Queue<Pedido> cola = new LinkedList<>();
        for (Pedido p : listarPedidos("WHERE p.estado = 'PENDIENTE' ORDER BY p.fecha ASC", null)) {
            cola.offer(p);
        }
        return cola;
    }

    /*  Recupera un pedido con sus detalles */
    public Pedido consultarPedido(int idPedido) throws ValidacionException, BaseDatosException {

        List<Pedido> lista = listarPedidos("WHERE p.id_pedido = ?", idPedido);
        if (lista.isEmpty()) {
            throw new ValidacionException("No existe el pedido N.° " + idPedido + ".");
        }

        Pedido pedido = lista.get(0);
        cargarDetalles(pedido);
        return pedido;
    }

    
    /* registra el resultado de las verificaciones y las observaciones */
    public void verificarPedido(Pedido pedido, EstadoVerificacion estadoPago,
                                EstadoVerificacion estadoDisponibilidad, String observaciones)
            throws ValidacionException, BaseDatosException {

        if (!pedido.estaPendiente()) {
            throw new ValidacionException("Solo se pueden verificar pedidos PENDIENTES.");
        }

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE pedido SET estado_pago = ?, estado_disponibilidad = ?, observaciones = ? "
                     + "WHERE id_pedido = ? AND estado = 'PENDIENTE'")) {

            ps.setString(1, estadoPago.name());
            ps.setString(2, estadoDisponibilidad.name());
            ps.setString(3, observaciones);
            ps.setInt(4, pedido.getIdPedido());

            if (ps.executeUpdate() == 0) {
                throw new ValidacionException("El pedido ya no está PENDIENTE; no se guardaron los cambios.");
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al registrar las verificaciones del pedido "
                    + pedido.getIdPedido(), e);
        }

        // El objeto se actualiza solo si la base se modificó
        pedido.setEstadoPago(estadoPago);
        pedido.setEstadoDisponibilidad(estadoDisponibilidad);
        pedido.setObservaciones(observaciones);
    }

    /* solo se puede confirmar si pago y disponibilidad están
       APROBADAS. Se informa primero el rechazo (A2) y después lo pendiente (A1).
     */
    public void validarVerificaciones(Pedido pedido) throws ValidacionException {

        EstadoVerificacion pago = pedido.getEstadoPago();
        EstadoVerificacion disponibilidad = pedido.getEstadoDisponibilidad();

        if (pago == EstadoVerificacion.RECHAZADA || disponibilidad == EstadoVerificacion.RECHAZADA) {
            throw new ValidacionException("No se puede confirmar: hay verificaciones rechazadas.");
        }

        if (pago == EstadoVerificacion.PENDIENTE || disponibilidad == EstadoVerificacion.PENDIENTE) {
            throw new ValidacionException("No se puede confirmar: hay verificaciones pendientes.");
        }
    }

    /* CU-08: confirmar pedido */
    public void confirmarPedido(Pedido pedido) throws ValidacionException, BaseDatosException {

        verificarPendiente(pedido);
        validarVerificaciones(pedido);

        // El WHERE repite la regla en la base por si otro usuario cambió el pedido
        actualizarEstado(pedido, EstadoPedido.CONFIRMADO, pedido.getObservaciones(),
                "AND estado_pago = 'APROBADA' AND estado_disponibilidad = 'APROBADA'");
    }

    /* rechazar pedido, con cualquier combinación de verificaciones */
    public void rechazarPedido(Pedido pedido, String motivo)
            throws ValidacionException, BaseDatosException {

        verificarPendiente(pedido);

        String observaciones = (motivo == null || motivo.isBlank())
                ? pedido.getObservaciones() : motivo.trim();
        actualizarEstado(pedido, EstadoPedido.RECHAZADO, observaciones, "");
    }

    private void verificarPendiente(Pedido pedido) throws ValidacionException {
        if (!pedido.estaPendiente()) {
            throw new ValidacionException("El pedido N.° " + pedido.getIdPedido() + " ya está "
                    + pedido.getEstado() + " y no puede resolverse.");
        }
    }

    private void actualizarEstado(Pedido pedido, EstadoPedido nuevoEstado, String observaciones,
                                  String condicionExtra) throws ValidacionException, BaseDatosException {

        try (Connection con = conexionBD.obtenerConexion()) {

            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE pedido SET estado = ?, observaciones = ? "
                    + "WHERE id_pedido = ? AND estado = 'PENDIENTE' " + condicionExtra)) {

                ps.setString(1, nuevoEstado.name());
                ps.setString(2, observaciones);
                ps.setInt(3, pedido.getIdPedido());

                if (ps.executeUpdate() == 0) {
                    con.rollback();
                    throw new ValidacionException("El pedido cambió mientras se resolvía. Consultalo nuevamente.");
                }
                con.commit();

            } catch (SQLException e) {
                con.rollback(); // se mantiene el estado anterior
                throw e;
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al actualizar el estado del pedido " + pedido.getIdPedido(), e);
        }

        pedido.setEstado(nuevoEstado);
        pedido.setObservaciones(observaciones);
    }

  
    private List<Pedido> listarPedidos(String filtro, Object parametro) throws BaseDatosException {

        List<Pedido> pedidos = new ArrayList<>();

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_PEDIDOS + filtro)) {

            if (parametro != null) {
                ps.setObject(1, parametro);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(crearPedido(rs));
                }
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al consultar los pedidos", e);
        }
        return pedidos;
    }

    private void cargarDetalles(Pedido pedido) throws BaseDatosException {

        String sql = "SELECT d.id_detalle, d.cantidad, d.precio_unitario, "
                + "p.id_producto, p.codigo, p.nombre, p.descripcion, p.presentacion, p.precio, "
                + "p.cantidad_maxima, p.activo, l.id_linea, l.nombre AS linea, l.descripcion AS linea_desc "
                + "FROM detalle_pedido d "
                + "INNER JOIN producto p ON p.id_producto = d.id_producto "
                + "INNER JOIN linea l ON l.id_linea = p.id_linea "
                + "WHERE d.id_pedido = ? ORDER BY d.id_detalle";

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, pedido.getIdPedido());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedido.agregarDetalle(new DetallePedido(
                            rs.getInt("id_detalle"),
                            crearProducto(rs),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario")));
                }
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al consultar el detalle del pedido " + pedido.getIdPedido(), e);
        }
    }

    private Producto crearProducto(ResultSet rs) throws SQLException {
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

    private Pedido crearPedido(ResultSet rs) throws SQLException {

        Cliente cliente = new Cliente(rs.getInt("id_usuario"), null, null, true,
                rs.getString("cuit_cuil"), rs.getString("razon_social"), null, null, null);
        cliente.setIdCliente(rs.getInt("id_cliente"));

        CondicionComercial condicion = new CondicionComercial(
                rs.getInt("id_condicion"),
                rs.getBigDecimal("importe_minimo_compra"),
                rs.getDate("fecha_desde").toLocalDate(),
                rs.getBoolean("activo"));

        return new Pedido(
                rs.getInt("id_pedido"),
                cliente,
                condicion,
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getBigDecimal("total"),
                EstadoPedido.valueOf(rs.getString("estado")),
                EstadoVerificacion.valueOf(rs.getString("estado_pago")),
                EstadoVerificacion.valueOf(rs.getString("estado_disponibilidad")),
                rs.getString("observaciones"));
    }
}
