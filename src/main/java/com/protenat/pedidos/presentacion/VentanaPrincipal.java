package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.PersonalAdministrativo;
import com.protenat.pedidos.dominio.Usuario;

/* Ventana principal después de iniciar sesión. Es abstracta: define la barra
 * de menú, el área de contenido y la opción Sesión, y cada subclase agrega las
 * opciones de su perfil.
 */
public abstract class VentanaPrincipal extends JFrame {

    private final Usuario usuario;
    private final ConexionBD conexionBD;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);

    protected VentanaPrincipal(Usuario usuario, ConexionBD conexionBD) {
        super("PROTENAT - Sistema de Gestión de Pedidos");
        this.usuario = usuario;
        this.conexionBD = conexionBD;
    }

    /* Se crea la ventana de acuerdo al perfil del usuario conoce el tipo VentanaPrincipal (polimorfismo).  */
    public static VentanaPrincipal crearPara(Usuario usuario, ConexionBD conexionBD) {
        VentanaPrincipal ventana;
        if (usuario instanceof Cliente) {
            ventana = new VentanaCliente((Cliente) usuario, conexionBD);
        } else if (usuario instanceof PersonalAdministrativo) {
            ventana = new VentanaAdministrativo((PersonalAdministrativo) usuario, conexionBD);
        } else {
            throw new IllegalArgumentException("Tipo de usuario no soportado: " + usuario.getTipoUsuario());
        }
        ventana.construir();
        return ventana;
    }

    /* Cada perfil agrega su menú y sus pantallas. */
    protected abstract void agregarOpciones(JMenuBar barra);

    /* Pantalla que se muestra al ingresar. */
    protected abstract String pantallaInicial();

    private void construir() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel lblUsuario = new JLabel("Usuario: " + usuario.getNombreVisible()
                + "  |  Perfil: " + usuario.getTipoUsuario());
        lblUsuario.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        add(lblUsuario, BorderLayout.SOUTH);
        add(contenido, BorderLayout.CENTER);

        JMenuBar barra = new JMenuBar();
        agregarOpciones(barra);
        barra.add(crearMenuSesion());
        setJMenuBar(barra);

        mostrarPantalla(pantallaInicial());
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
    }

    private JMenu crearMenuSesion() {
        JMenu menu = new JMenu("Sesión");
        JMenuItem cerrar = new JMenuItem("Cerrar sesión");
        JMenuItem salir = new JMenuItem("Salir");
        cerrar.addActionListener(e -> {
            dispose();
            InterfazLogin.mostrar(conexionBD);
        });
        salir.addActionListener(e -> System.exit(0));
        menu.add(cerrar);
        menu.addSeparator();
        menu.add(salir);
        return menu;
    }

    /* Agrega una pantalla y una opción de menú que la muestra. */
    protected void agregarPantalla(JMenu menu, String nombre, PantallaBase pantalla) {
        contenido.add(pantalla, nombre);
        JMenuItem opcion = new JMenuItem(nombre);
        opcion.addActionListener(e -> mostrarPantalla(nombre));
        menu.add(opcion);
    }

    protected void mostrarPantalla(String nombre) {
        tarjetas.show(contenido, nombre);
        for (java.awt.Component c : contenido.getComponents()) {
            if (c.isVisible()) {
                ((PantallaBase) c).actualizar(); // cada pantalla recarga sus datos (polimorfismo)
            }
        }
    }

    protected ConexionBD getConexionBD() {
        return conexionBD;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
