package com.protenat.pedidos.dominio;

public class Cliente extends Usuario {

    private int idCliente;
    private String cuitCuil;
    private String razonSocial;
    private String telefono;
    private String direccion;
    private String localidad;

    public Cliente(
            int idUsuario,
            String email,
            String clave,
            boolean activo,
            String cuitCuil,
            String razonSocial,
            String telefono,
            String direccion,
            String localidad) {

        super(idUsuario, email, clave, activo);

        this.cuitCuil = cuitCuil;
        this.razonSocial = razonSocial;
        this.telefono = telefono;
        this.direccion = direccion;
        this.localidad = localidad;
    }

    public String getCuitCuil() {
        return cuitCuil;
    }

    public void setCuitCuil(String cuitCuil) {
        this.cuitCuil = cuitCuil;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    @Override
    public String getTipoUsuario() {
        return "CLIENTE";
    }

    @Override
    public String getNombreVisible() {
        return razonSocial;
    }
}