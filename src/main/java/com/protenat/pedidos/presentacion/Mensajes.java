package com.protenat.pedidos.presentacion;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.util.Registro;

/* Muestra mensajes con cuadros de diálogo JOptionPane
  del tipo que corresponde: error, advertencia, información o confirmación. */
public final class Mensajes {

    private static final String TITULO = "PROTENAT";

    private Mensajes() {
    }

    public static void informar(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, TITULO, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void advertir(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, TITULO, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, TITULO, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String pregunta) {
        int opcion = JOptionPane.showConfirmDialog(padre, pregunta, TITULO,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }

    /**
     * Convierte una excepción en el mensaje para el usuario. Las reglas de
     * negocio se muestran tal cual; los errores técnicos se registran en el log
     * y se informa el mensaje general MSG-16.
     */
    public static void mostrar(Component padre, Exception e) {
        if (e instanceof ValidacionException) {
            advertir(padre, e.getMessage());
        } else if (e instanceof BaseDatosException) {
            Registro.error(e.getMessage(), e.getCause());
            error(padre, ((BaseDatosException) e).getMensajeUsuario());
        } else {
            Registro.error("Error inesperado", e);
            error(padre, BaseDatosException.MENSAJE_USUARIO);
        }
    }
}
