package com.protenat.pedidos;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.presentacion.InterfazLogin;
import com.protenat.pedidos.util.Registro;

/**
 * Punto de entrada del Sistema de Gestión de Pedidos PROTENAT.
 */
public class Main {

    public static void main(String[] args) {

        ConexionBD conexionBD;
        try {
            conexionBD = new ConexionBD();
        } catch (BaseDatosException e) {
            mostrarErrorConexion(e);
            return;
        }

        // Tamaño de las ventanas y letras (ui.escala en config.properties).
        // Debe indicarse antes de crear la primera ventana.
        System.setProperty("sun.java2d.uiScale", conexionBD.getPropiedad("ui.escala", "1"));

        usarAspectoNimbus();

        SwingUtilities.invokeLater(() -> {
            try {
                if (!conexionBD.probarConexion()) {
                    throw new BaseDatosException("El servidor MySQL no responde", null);
                }

                new InterfazLogin(conexionBD).setVisible(true);

            } catch (BaseDatosException e) {
                mostrarErrorConexion(e);
            }
        });
    }

    private static void mostrarErrorConexion(BaseDatosException e) {
        Registro.error(e.getMessage(), e.getCause());
        JOptionPane.showMessageDialog(null,
                e.getMensajeUsuario() + "\nRevisá que MySQL esté iniciado y los datos de config.properties.",
                "PROTENAT", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void usarAspectoNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // si no está disponible se usa el aspecto por defecto de Java
        }
    }
}
