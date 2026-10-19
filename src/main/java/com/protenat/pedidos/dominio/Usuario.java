package com.protenat.pedidos.dominio;

public abstract class Usuario {

    private int idUsuario;
    private String email;
    private String clave;
    private boolean activo;

    public Usuario(int idUsuario, String email, String clave, boolean activo) {
        this.idUsuario = idUsuario;
        this.email = email;
        this.clave = clave;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean validarClave(String claveIngresada) {
        return clave != null && clave.equals(claveIngresada);
    }

    // Cada tipo de usuario deberá indicar su rol
    public abstract String getTipoUsuario();

    // Cada tipo de usuario indica cómo se lo nombra en las pantallas
    public abstract String getNombreVisible();

    @Override
    public String toString() {
        return getTipoUsuario() + " - " + getNombreVisible();
    }
}