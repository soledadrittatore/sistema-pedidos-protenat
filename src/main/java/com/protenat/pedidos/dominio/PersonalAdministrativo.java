package com.protenat.pedidos.dominio;

import java.time.LocalDate;

public class PersonalAdministrativo extends Usuario {

    private String legajo;
    private String nombre;
    private String apellido;
    private LocalDate fechaIngreso;

    public PersonalAdministrativo(
            int idUsuario,
            String email,
            String clave,
            boolean activo,
            String legajo,
            String nombre,
            String apellido,
            LocalDate fechaIngreso) {

        super(idUsuario, email, clave, activo);

        this.legajo = legajo;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaIngreso = fechaIngreso;
    }

    public String getLegajo() {
        return legajo;
    }

    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public LocalDate getFechaIngreso() {
    return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
    
    @Override
    public String getTipoUsuario() {
        return "ADMINISTRATIVO";
    }

    @Override
    public String getNombreVisible() {
        return nombre + " " + apellido;
    }
}