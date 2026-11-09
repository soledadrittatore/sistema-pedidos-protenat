package com.protenat.pedidos.persistencia;

import java.util.ArrayList;
import java.util.List;

import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Pedido;

/**
 * Implementación de PedidoDAO que guarda los pedidos en un ArrayList. Permite
 * probar ControlPedido sin MySQL: el control solo conoce la interfaz.
 */
public class PedidoDAOEnMemoria implements PedidoDAO {

    private final List<Pedido> pedidos = new ArrayList<>();

    @Override
    public void registrar(Pedido pedido) {
        pedido.setIdPedido(pedidos.size() + 1);
        pedidos.add(pedido);
    }

    @Override
    public List<Pedido> listarPorCliente(int idCliente) {
        List<Pedido> resultado = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (p.getCliente().getIdCliente() == idCliente) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    @Override
    public List<Pedido> listar(EstadoPedido estado) {
        List<Pedido> resultado = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (estado == null || p.getEstado() == estado) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    @Override
    public List<Pedido> listarPendientesPorAntiguedad() {
        return listar(EstadoPedido.PENDIENTE);
    }

    @Override
    public Pedido buscarPorId(int idPedido) {
        for (Pedido p : pedidos) {
            if (p.getIdPedido() == idPedido) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean actualizarVerificaciones(int idPedido, EstadoVerificacion pago,
            EstadoVerificacion disponibilidad, String observaciones) {
        Pedido p = buscarPorId(idPedido);
        return p != null && p.estaPendiente();
    }

    @Override
    public boolean actualizarEstado(int idPedido, EstadoPedido nuevoEstado, String observaciones,
            boolean exigirAprobadas) {
        Pedido p = buscarPorId(idPedido);
        return p != null && p.estaPendiente();
    }

    @Override
    public int[] contarPorEstado() {
        int[] cantidades = new int[EstadoPedido.values().length];
        for (Pedido p : pedidos) {
            cantidades[p.getEstado().ordinal()]++;
        }
        return cantidades;
    }
}
