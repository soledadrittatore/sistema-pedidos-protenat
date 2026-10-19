package com.protenat.pedidos.presentacion;

import javax.swing.table.DefaultTableModel;

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
