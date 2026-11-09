package com.protenat.pedidos.control;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.dominio.DetallePedido;
import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.ArchivoException;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.persistencia.ArchivoBorrador;
import com.protenat.pedidos.persistencia.ExportadorCSV;
import com.protenat.pedidos.persistencia.PedidoDAO;
import com.protenat.pedidos.persistencia.ProductoDAO;
import com.protenat.pedidos.util.Formato;
import com.protenat.pedidos.util.Ordenamiento;

/**
 * Coordina la gestión de pedidos: CU-03, CU-04 y CU-10 para el cliente, y
 * CU-05 a CU-08 para la consulta, verificación y resolución.
 */
public class ControlPedido {

    // Criterios de ordenación del catálogo que ofrece la pantalla
    public static final Comparator<Producto> POR_CODIGO = Comparator.comparing(Producto::getCodigo);
    public static final Comparator<Producto> POR_NOMBRE =
            Comparator.comparing(Producto::getNombre).thenComparing(Producto::getPresentacion);
    public static final Comparator<Producto> POR_PRECIO = Comparator.comparing(Producto::getPrecio);

    // Cada cambio del pedido en curso se guarda para poder deshacerlo
    private static class Movimiento {
        private final Producto producto;
        private final int cantidadAnterior; // 0 = el producto no estaba en el pedido

        Movimiento(Producto producto, int cantidadAnterior) {
            this.producto = producto;
            this.cantidadAnterior = cantidadAnterior;
        }
    }

    private final PedidoDAO pedidoDAO;
    private final ProductoDAO productoDAO;
    private final ControlCondicionComercial controlCondicion;
    private final ArchivoBorrador borrador;
    private final ExportadorCSV exportador = new ExportadorCSV();

    private Pedido pedidoEnCurso;
    private final Deque<Movimiento> historial = new ArrayDeque<>(); // pila (LIFO)

    /** Las dependencias son interfaces: en las pruebas se reemplazan por DAO en memoria. */
    public ControlPedido(PedidoDAO pedidoDAO, ProductoDAO productoDAO,
                         ControlCondicionComercial controlCondicion, ArchivoBorrador borrador) {
        this.pedidoDAO = pedidoDAO;
        this.productoDAO = productoDAO;
        this.controlCondicion = controlCondicion;
        this.borrador = borrador;
    }

    public ControlPedido(PedidoDAO pedidoDAO, ProductoDAO productoDAO,
                         ControlCondicionComercial controlCondicion) {
        this(pedidoDAO, productoDAO, controlCondicion, new ArchivoBorrador());
    }

    // =====================================================================
    // CU-03 Consultar productos
    // =====================================================================

    // Productos activos, ordenados por código con ordenación por inserción
    public List<Producto> consultarProductos() throws BaseDatosException {
        List<Producto> productos = productoDAO.listarActivos(); // ArrayList: tamaño variable
        Ordenamiento.ordenarPorInsercion(productos, POR_CODIGO);
        return productos;
    }

    // Ordena el catálogo con el criterio que elige el usuario
    public void ordenarProductos(List<Producto> productos, Comparator<Producto> criterio,
                                 boolean descendente) {
        Ordenamiento.ordenarPorInsercion(productos, descendente ? criterio.reversed() : criterio);
    }

    // Busca un producto por código con búsqueda binaria (la lista se ordena antes)
    public Producto buscarProducto(List<Producto> productos, String codigo)
            throws ValidacionException {

        if (codigo == null || codigo.isBlank()) {
            throw new ValidacionException("Ingresá el código del producto que querés buscar.");
        }

        // Se copia el catálogo a un arreglo ordenado por código: la búsqueda
        // binaria necesita acceso directo por posición y el arreglo no cambia
        List<Producto> ordenados = new ArrayList<>(productos);
        Ordenamiento.ordenarPorInsercion(ordenados, POR_CODIGO);
        Producto[] arreglo = ordenados.toArray(new Producto[0]);

        Producto encontrado = Ordenamiento.busquedaBinaria(arreglo, codigo.trim(), Producto::getCodigo);
        if (encontrado == null) {
            throw new ValidacionException("No existe un producto habilitado con el código "
                    + codigo.trim().toUpperCase() + ".");
        }
        return encontrado;
    }

    // =====================================================================
    // CU-04 Gestionar pedido y CU-10 Validar condiciones comerciales
    // =====================================================================

    public Pedido iniciarPedido(Cliente cliente) throws BaseDatosException {
        CondicionComercial vigente = controlCondicion.consultarCondiciones();
        pedidoEnCurso = new Pedido(cliente, vigente);
        historial.clear();
        return pedidoEnCurso;
    }

    // CU-10 paso 3: la cantidad debe estar entre 1 y la cantidad máxima
    public boolean validarCantidad(Producto producto, int cantidad) {

        if (cantidad <= 0) {
            return false;
        }

        return cantidad <= producto.getCantidadMaxima();
    }

    // CU-10 paso 6: el total debe alcanzar o superar el importe mínimo
    public boolean validarImporteMinimo(Pedido pedido, CondicionComercial condicion) {
        return pedido.getTotal().compareTo(condicion.getImporteMinimoCompra()) >= 0;
    }

    /**
     * Incorpora el producto al pedido en curso. Si ya estaba, suma la cantidad
     * y valida el total contra la cantidad máxima (CU-10 pasos 2 a 4).
     */
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

    // CU-04 A3: quita un producto del pedido
    public void quitarProducto(Pedido pedido, String codigo) throws ValidacionException {

        DetallePedido detalle = pedido.buscarDetalle(codigo);
        if (detalle == null) {
            throw new ValidacionException("Seleccioná un producto del pedido para quitarlo.");
        }

        historial.push(new Movimiento(detalle.getProducto(), detalle.getCantidad()));
        pedido.eliminarDetalle(detalle);
    }

    // Deshace el último cambio tomándolo del tope de la pila
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

    // Importe que le falta al pedido para llegar al mínimo (cero si ya lo alcanzó)
    public BigDecimal calcularFaltante(Pedido pedido) {
        BigDecimal falta = pedido.getCondicionComercial().getImporteMinimoCompra()
                .subtract(pedido.getTotal());
        return falta.max(BigDecimal.ZERO);
    }

    // CU-10 pasos 5 a 7, antes de mostrar el resumen
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

    // CU-04 pasos 11 a 13: valida y registra el pedido; devuelve su número
    public int generarPedido(Pedido pedido) throws ValidacionException, BaseDatosException {
        validarPedido(pedido);
        pedidoDAO.registrar(pedido); // pedido y detalles en una misma transacción
        cancelarGestion();
        descartarBorrador(pedido.getCliente());
        return pedido.getIdPedido();
    }

    // CU-04 A4: se descarta el pedido en curso sin registrarlo
    public void cancelarGestion() {
        pedidoEnCurso = null;
        historial.clear();
    }

    public Pedido getPedidoEnCurso() {
        return pedidoEnCurso;
    }

    // =====================================================================
    // CU-05 Consultar mis pedidos y CU-06 Gestionar pedidos recibidos
    // =====================================================================

    // CU-05: pedidos del cliente, del más reciente al más antiguo
    public List<Pedido> consultarPedidosCliente(Cliente cliente) throws BaseDatosException {
        return pedidoDAO.listarPorCliente(cliente.getIdCliente());
    }

    // CU-06: todos los pedidos o solo los de un estado
    public List<Pedido> consultarPedidos(EstadoPedido filtroEstado) throws BaseDatosException {
        return pedidoDAO.listar(filtroEstado);
    }

    // CU-06: filtra por número de pedido o razón social (búsqueda secuencial)
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

    /**
     * Pedidos pendientes como cola (FIFO): el primero que llegó es el primero
     * que atiende el personal administrativo.
     */
    public Queue<Pedido> obtenerColaPendientes() throws BaseDatosException {
        Queue<Pedido> cola = new LinkedList<>();
        for (Pedido p : pedidoDAO.listarPendientesPorAntiguedad()) {
            cola.offer(p);
        }
        return cola;
    }

    // Recupera un pedido con sus detalles
    public Pedido consultarPedido(int idPedido) throws ValidacionException, BaseDatosException {

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            throw new ValidacionException("No existe el pedido N.° " + idPedido + ".");
        }
        return pedido;
    }

    /**
     * Resumen de la bandeja: cantidad de pedidos por estado. El arreglo tiene
     * una posición por estado, en el orden de EstadoPedido.values().
     */
    public int[] contarPedidosPorEstado() throws BaseDatosException {
        return pedidoDAO.contarPorEstado();
    }

    // =====================================================================
    // CU-07 Verificar pedido y CU-08 Resolver pedido
    // =====================================================================

    // CU-07: registra el resultado de las verificaciones y las observaciones
    public void verificarPedido(Pedido pedido, EstadoVerificacion estadoPago,
                                EstadoVerificacion estadoDisponibilidad, String observaciones)
            throws ValidacionException, BaseDatosException {

        if (!pedido.estaPendiente()) {
            throw new ValidacionException("Solo se pueden verificar pedidos PENDIENTES.");
        }

        if (!pedidoDAO.actualizarVerificaciones(pedido.getIdPedido(), estadoPago,
                estadoDisponibilidad, observaciones)) {
            throw new ValidacionException("El pedido ya no está PENDIENTE; no se guardaron los cambios.");
        }

        // El objeto se actualiza solo si la base se modificó
        pedido.setEstadoPago(estadoPago);
        pedido.setEstadoDisponibilidad(estadoDisponibilidad);
        pedido.setObservaciones(observaciones);
    }

    /**
     * CU-08 paso 4: solo se puede confirmar si pago y disponibilidad están
     * APROBADAS. Se informa primero el rechazo (A2) y después lo pendiente (A1).
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

    // CU-08: confirmar pedido
    public void confirmarPedido(Pedido pedido) throws ValidacionException, BaseDatosException {

        verificarPendiente(pedido);
        validarVerificaciones(pedido);

        // El WHERE repite la regla en la base por si otro usuario cambió el pedido
        actualizarEstado(pedido, EstadoPedido.CONFIRMADO, pedido.getObservaciones(), true);
    }

    // CU-08 A3: rechazar pedido, con cualquier combinación de verificaciones
    public void rechazarPedido(Pedido pedido, String motivo)
            throws ValidacionException, BaseDatosException {

        verificarPendiente(pedido);

        String observaciones = (motivo == null || motivo.isBlank())
                ? pedido.getObservaciones() : motivo.trim();
        actualizarEstado(pedido, EstadoPedido.RECHAZADO, observaciones, false);
    }

    private void verificarPendiente(Pedido pedido) throws ValidacionException {
        if (!pedido.estaPendiente()) {
            throw new ValidacionException("El pedido N.° " + pedido.getIdPedido() + " ya está "
                    + pedido.getEstado() + " y no puede resolverse.");
        }
    }

    private void actualizarEstado(Pedido pedido, EstadoPedido nuevoEstado, String observaciones,
                                  boolean exigirAprobadas) throws ValidacionException, BaseDatosException {

        if (!pedidoDAO.actualizarEstado(pedido.getIdPedido(), nuevoEstado, observaciones, exigirAprobadas)) {
            throw new ValidacionException("El pedido cambió mientras se resolvía. Consultalo nuevamente.");
        }

        pedido.setEstado(nuevoEstado);
        pedido.setObservaciones(observaciones);
    }

    // =====================================================================
    // Archivos: borrador del pedido en curso y exportación de la bandeja
    // =====================================================================

    /** Guarda el pedido en curso en un archivo para terminarlo en otra sesión. */
    public void guardarBorrador(Pedido pedido) throws ValidacionException, ArchivoException {
        if (pedido.estaVacio()) {
            throw new ValidacionException("El pedido no tiene productos para guardar.");
        }
        borrador.guardar(pedido);
    }

    public boolean hayBorrador(Cliente cliente) {
        return borrador.existe(cliente.getIdCliente());
    }

    public String fechaBorrador(Cliente cliente) throws ArchivoException {
        return Formato.fechaHora(borrador.fechaGuardado(cliente.getIdCliente()));
    }

    /**
     * Carga en el pedido los productos del borrador. Cada producto vuelve a
     * pasar por agregarProducto(), así se aplican el precio y la cantidad
     * máxima vigentes. Devuelve los avisos de lo que no se pudo recuperar.
     */
    public List<String> recuperarBorrador(Pedido pedido, List<Producto> catalogo) throws ArchivoException {

        List<String> avisos = new ArrayList<>();

        for (String[] linea : borrador.recuperar(pedido.getCliente().getIdCliente())) {
            String codigo = linea[0];
            try {
                int cantidad = Integer.parseInt(linea[1]);
                agregarProducto(pedido, buscarProducto(catalogo, codigo), cantidad);
            } catch (NumberFormatException e) {
                avisos.add(codigo + ": la cantidad guardada no es válida.");
            } catch (ValidacionException e) {
                avisos.add(codigo + ": " + e.getMessage());
            }
        }

        historial.clear(); // lo recuperado no se deshace de a un producto
        return avisos;
    }

    public void descartarBorrador(Cliente cliente) {
        try {
            borrador.eliminar(cliente.getIdCliente());
        } catch (ArchivoException e) {
            // si no se puede borrar, al volver a ingresar se ofrece recuperarlo
        }
    }

    /** Exporta los pedidos que se ven en la bandeja a un archivo CSV. */
    public int exportarPedidos(List<Pedido> pedidos, Path destino)
            throws ValidacionException, ArchivoException {
        if (pedidos.isEmpty()) {
            throw new ValidacionException("No hay pedidos en la bandeja para exportar.");
        }
        return exportador.exportarPedidos(pedidos, destino);
    }
}
