package com.protenat.pedidos.dominio;

import java.math.BigDecimal;

public class Producto {

    private int idProducto;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String presentacion;
    private BigDecimal precio;
    private int cantidadMaxima;
    private boolean activo;
    private Linea linea;

    public Producto(
            int idProducto,
            String codigo,
            String nombre,
            String descripcion,
            String presentacion,
            BigDecimal precio,
            int cantidadMaxima,
            boolean activo,
            Linea linea) {

        this.idProducto = idProducto;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.presentacion = presentacion;
        this.precio = precio;
        this.cantidadMaxima = cantidadMaxima;
        this.activo = activo;
        this.linea = linea;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getCantidadMaxima() {
        return cantidadMaxima;
    }

    public void setCantidadMaxima(int cantidadMaxima) {
        this.cantidadMaxima = cantidadMaxima;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Linea getLinea() {
        return linea;
    }

    public void setLinea(Linea linea) {
        this.linea = linea;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}