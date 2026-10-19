package com.protenat.pedidos.dominio;

import java.math.BigDecimal;

public class DetallePedido {

    private int idDetalle;
    private Producto producto;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    // Constructor
    public DetallePedido(
            int idDetalle,
            Producto producto,
            int cantidad,
            BigDecimal precioUnitario) {

        this.idDetalle = idDetalle;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }

    // Constructor para un detalle nuevo: toma el precio actual del producto
    public DetallePedido(Producto producto, int cantidad) {
        this(0, producto, cantidad, producto.getPrecio());
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        calcularSubtotal();
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    private void calcularSubtotal() {
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}