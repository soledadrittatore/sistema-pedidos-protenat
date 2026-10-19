package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/* Criterios visuales comunes a todas las pantallas */
public final class Estilo {

    public static final Color PRIMARIO = new Color(0x2E, 0x6B, 0x4F);
    public static final Color FONDO_TITULO = new Color(0xE8, 0xF1, 0xEC);
    public static final Font TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font NEGRITA = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    private Estilo() {
    }

    /* Encabezado con el nombre de la pantalla y una ayuda breve. */
    public static JPanel encabezado(String titulo, String ayuda) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(FONDO_TITULO);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(TITULO);
        lblTitulo.setForeground(PRIMARIO);
        panel.add(lblTitulo, BorderLayout.NORTH);
        if (ayuda != null) {
            panel.add(new JLabel(ayuda), BorderLayout.SOUTH);
        }
        return panel;
    }

    public static JButton botonPrincipal(String texto) {
        JButton boton = new JButton(texto);
        boton.setFont(NEGRITA);
        return boton;
    }

    public static JTable tabla(TablaSoloLectura modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(22);
        tabla.getTableHeader().setReorderingAllowed(false);
        return tabla;
    }

    public static JScrollPane desplazable(JTable tabla, int alto) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(400, alto));
        return scroll;
    }

    public static JPanel titulado(String titulo) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(titulo),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        return panel;
    }
}
