package com.protenat.pedidos.dominio;

public class Linea {

    private int idLinea;
    private String nombre;
    private String descripcion;

    // Constructor
    public Linea(int idLinea, String nombre, String descripcion) {
        this.idLinea = idLinea;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getIdLinea() {
        return idLinea;
    }

    public void setIdLinea(int idLinea) {
        this.idLinea = idLinea;
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

    @Override
    public String toString() {
        return nombre;
    }
}