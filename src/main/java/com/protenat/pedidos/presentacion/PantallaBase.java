package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public abstract class PantallaBase extends JPanel {

    protected PantallaBase(String titulo, String ayuda) {
        super(new BorderLayout(8, 8));
        add(Estilo.encabezado(titulo, ayuda), BorderLayout.NORTH);
    }

    /** Se llama cada vez que la pantalla se muestra. */
    public abstract void actualizar();
}
