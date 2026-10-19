package com.protenat.pedidos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pedido {

    private int idPedido;
    private Cliente cliente;
    private CondicionComercial condicionComercial;
    private LocalDateTime fecha;
    private BigDecimal total;
    private EstadoPedido estado;
    private EstadoVerificacion estadoPago;
    private EstadoVerificacion estadoDisponibilidad;
    private String observaciones;
    private List<DetallePedido> detalles;

    // Constructor para un pedido nuevo, todos los estados en PENDIENTE
    public Pedido(Cliente cliente, CondicionComercial condicionComercial) {
        this(0, cliente, condicionComercial, LocalDateTime.now(), BigDecimal.ZERO,
                EstadoPedido.PENDIENTE, EstadoVerificacion.PENDIENTE,
                EstadoVerificacion.PENDIENTE, null);
    }

    // Constructor completo, usado al recuperar el pedido de la base
    public Pedido(
            int idPedido,
            Cliente cliente,
            CondicionComercial condicionComercial,
            LocalDateTime fecha,
            BigDecimal total,
            EstadoPedido estado,
            EstadoVerificacion estadoPago,
            EstadoVerificacion estadoDisponibilidad,
            String observaciones) {

        this.idPedido = idPedido;
        this.cliente = cliente;
        this.condicionComercial = condicionComercial;
        this.fecha = fecha;
        this.total = total;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.estadoDisponibilidad = estadoDisponibilidad;
        this.observaciones = observaciones;
        this.detalles = new ArrayList<>();
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public CondicionComercial getCondicionComercial() {
        return condicionComercial;
    }

    public void setCondicionComercial(CondicionComercial condicionComercial) {
        this.condicionComercial = condicionComercial;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public EstadoVerificacion getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoVerificacion estadoPago) {
        this.estadoPago = estadoPago;
    }

    public EstadoVerificacion getEstadoDisponibilidad() {
        return estadoDisponibilidad;
    }

    public void setEstadoDisponibilidad(
            EstadoVerificacion estadoDisponibilidad) {
        this.estadoDisponibilidad = estadoDisponibilidad;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    // Devuelve la lista de solo lectura: los detalles se cambian con los
    // métodos del pedido, así el total siempre queda actualizado
    public List<DetallePedido> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        calcularTotal();
    }

    public void modificarDetalle(DetallePedido detalle, int nuevaCantidad) {
        detalle.setCantidad(nuevaCantidad);
        calcularTotal();
    }

    public void eliminarDetalle(DetallePedido detalle) {
        detalles.remove(detalle);
        calcularTotal();
    }

    // Búsqueda secuencial: devuelve el detalle del producto o null
    public DetallePedido buscarDetalle(String codigoProducto) {
        for (DetallePedido detalle : detalles) {
            if (detalle.getProducto().getCodigo().equalsIgnoreCase(codigoProducto)) {
                return detalle;
            }
        }
        return null;
    }

    public boolean estaVacio() {
        return detalles.isEmpty();
    }

    public boolean estaPendiente() {
        return estado == EstadoPedido.PENDIENTE;
    }

    private void calcularTotal() {
        total = BigDecimal.ZERO;

        for (DetallePedido detalle : detalles) {
            total = total.add(detalle.getSubtotal());
        }
    }
}