package com.protenat.pedidos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CondicionComercial {

    private int idCondicion;
    private BigDecimal importeMinimoCompra;
    private LocalDate fechaDesde;
    private boolean activo;

    public CondicionComercial(
            int idCondicion,
            BigDecimal importeMinimoCompra,
            LocalDate fechaDesde,
            boolean activo) {

        this.idCondicion = idCondicion;
        this.importeMinimoCompra = importeMinimoCompra;
        this.fechaDesde = fechaDesde;
        this.activo = activo;
    }

    public int getIdCondicion() {
        return idCondicion;
    }

    public void setIdCondicion(int idCondicion) {
        this.idCondicion = idCondicion;
    }

    public BigDecimal getImporteMinimoCompra() {
        return importeMinimoCompra;
    }

    public void setImporteMinimoCompra(BigDecimal importeMinimoCompra) {
        this.importeMinimoCompra = importeMinimoCompra;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}