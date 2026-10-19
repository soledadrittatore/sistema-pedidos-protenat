package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.protenat.pedidos.control.ControlLogin;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;

/* CU-01 Registrarse: formulario de alta de un cliente comercial. */
public class InterfazRegistro extends JDialog {

    private final ControlLogin controlLogin;

    private final JTextField txtCuit = new JTextField(20);
    private final JTextField txtRazonSocial = new JTextField(20);
    private final JTextField txtEmail = new JTextField(20);
    private final JPasswordField txtClave = new JPasswordField(20);
    private final JTextField txtTelefono = new JTextField(20);
    private final JTextField txtDireccion = new JTextField(20);
    private final JTextField txtLocalidad = new JTextField(20);

    public InterfazRegistro(JFrame padre, ControlLogin controlLogin) {
        super(padre, "PROTENAT - Registro de cliente", true);
        this.controlLogin = controlLogin;
        construir();
        setLocationRelativeTo(padre);
    }

    private void construir() {
        setLayout(new BorderLayout());
        add(Estilo.encabezado("Registro de cliente comercial",
                "Los campos con * son obligatorios."), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        String[] etiquetas = {"CUIT/CUIL * (30-71234567-8)", "Nombre o razón social *",
            "Correo electrónico *", "Contraseña * (mín. 6)", "Teléfono", "Dirección", "Localidad *"};
        JTextField[] campos = {txtCuit, txtRazonSocial, txtEmail, txtClave,
            txtTelefono, txtDireccion, txtLocalidad};

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < etiquetas.length; i++) {
            g.gridx = 0; g.gridy = i;
            formulario.add(new JLabel(etiquetas[i]), g);
            g.gridx = 1;
            formulario.add(campos[i], g);
        }
        add(formulario, BorderLayout.CENTER);

        JButton btnRegistrar = Estilo.botonPrincipal("Registrarme");
        JButton btnCancelar = new JButton("Cancelar");
        btnRegistrar.addActionListener(e -> registrar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnRegistrar);
        add(botones, BorderLayout.SOUTH);
        pack();
    }

    private void registrar() {
        // Se crea el objeto Cliente con su constructor y se lo pasa al control
        Cliente cliente = new Cliente(0,
                txtEmail.getText().trim(),
                new String(txtClave.getPassword()),
                true,
                txtCuit.getText().trim(),
                txtRazonSocial.getText().trim(),
                txtTelefono.getText().trim(),
                txtDireccion.getText().trim(),
                txtLocalidad.getText().trim());
        try {
            controlLogin.registrarCliente(cliente);
            Mensajes.informar(this, "Registro realizado. Ya podés iniciar sesión.");
            dispose();
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }
}
