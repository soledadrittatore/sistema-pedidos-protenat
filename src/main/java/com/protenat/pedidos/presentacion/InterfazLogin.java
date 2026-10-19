package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.control.ControlLogin;
import com.protenat.pedidos.dominio.Usuario;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;

/* Pantalla Iniciar sesión. */
public class InterfazLogin extends JFrame {

    private final ConexionBD conexionBD;
    private final ControlLogin controlLogin;

    private final JTextField txtEmail = new JTextField(22);
    private final JPasswordField txtClave = new JPasswordField(22);

    public InterfazLogin(ConexionBD conexionBD) {
        super("PROTENAT - Acceso al sistema");
        this.conexionBD = conexionBD;
        this.controlLogin = new ControlLogin(conexionBD);
        construir();
    }

    private void construir() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(Estilo.encabezado("Sistema de Gestión de Pedidos",
                "Ingresá con tu correo y contraseña."), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;

        txtEmail.setName("txtEmail");
        txtClave.setName("txtClave");

        g.gridx = 0; g.gridy = 0;
        formulario.add(new JLabel("Correo electrónico"), g);
        g.gridx = 1;
        formulario.add(txtEmail, g);
        g.gridx = 0; g.gridy = 1;
        formulario.add(new JLabel("Contraseña"), g);
        g.gridx = 1;
        formulario.add(txtClave, g);
        add(formulario, BorderLayout.CENTER);

        JButton btnIngresar = Estilo.botonPrincipal("Ingresar");
        JButton btnRegistrarme = new JButton("Registrarme");
        btnIngresar.addActionListener(e -> ingresar());
        btnRegistrarme.addActionListener(e -> new InterfazRegistro(this, controlLogin).setVisible(true));
        getRootPane().setDefaultButton(btnIngresar); 

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnRegistrarme);
        botones.add(btnIngresar);
        add(botones, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        String email = txtEmail.getText();
        String clave = new String(txtClave.getPassword());
        try {
            Usuario usuario = controlLogin.iniciarSesion(email, clave);
            // Polimorfismo: se abre la ventana del perfil sin preguntar la clase concreta
            VentanaPrincipal principal = VentanaPrincipal.crearPara(usuario, conexionBD);
            principal.setVisible(true);
            txtClave.setText("");
            dispose();
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
            txtClave.setText("");
            txtClave.requestFocusInWindow();
        }
    }

    /** Lo usa la ventana principal al cerrar sesión. */
    public static void mostrar(ConexionBD conexionBD) {
        new InterfazLogin(conexionBD).setVisible(true);
    }
}
