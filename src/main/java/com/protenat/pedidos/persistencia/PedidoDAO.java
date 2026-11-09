package com.protenat.pedidos.persistencia;

import java.util.List;

import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.excepciones.BaseDatosException;

/**
 * Operaciones de persistencia de pedidos y sus detalles (CU-04 a CU-08).
 */
public interface PedidoDAO {

    /* Registra el pedido y todos sus detalles en una transacción y le asigna el número. */
    void registrar(Pedido pedido) throws BaseDatosException;

    /* Pedidos de un cliente, del más reciente al más antiguo (sin detalles). */
    List<Pedido> listarPorCliente(int idCliente) throws BaseDatosException;

    /* Todos los pedidos, o solo los del estado indicado si no es null (sin detalles). */
    List<Pedido> listar(EstadoPedido estado) throws BaseDatosException;

    /* Pedidos PENDIENTES del más antiguo al más reciente, para la cola de atención. */
    List<Pedido> listarPendientesPorAntiguedad() throws BaseDatosException;

    /** Pedido con sus detalles, o null si no existe. */
    Pedido buscarPorId(int idPedido) throws BaseDatosException;

    /* Registra las verificaciones de un pedido PENDIENTE. Devuelve false si el pedido ya no estaba pendiente. */
    boolean actualizarVerificaciones(int idPedido, EstadoVerificacion pago,
            EstadoVerificacion disponibilidad, String observaciones) throws BaseDatosException;

    /* Cambia el estado de un pedido PENDIENTE. Si exigirAprobadas es true, la base solo lo actualiza cuando pago y disponibilidad están APROBADAS.
       Devuelve false si no se modificó ninguna fila. */
    boolean actualizarEstado(int idPedido, EstadoPedido nuevoEstado, String observaciones,
            boolean exigirAprobadas) throws BaseDatosException;

    /* Cantidad de pedidos por estado. El arreglo tiene una posición por cada valor de EstadoPedido, en el orden de EstadoPedido.values().*/
    int[] contarPorEstado() throws BaseDatosException;
}
