package com.protenat.pedidos.presentacion;

import javax.swing.table.DefaultTableModel;

/**
 * Modelo de tabla que no permite editar las celdas: los datos se cambian con
 * los botones de cada pantalla, que pasan por las clases de control.
 */
public class TablaSoloLectura extends DefaultTableModel {

    public TablaSoloLectura(String... columnas) {
        super(columnas, 0);
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    public void vaciar() {
        setRowCount(0);
    }
}
