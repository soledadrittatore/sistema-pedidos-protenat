package com.protenat.pedidos.presentacion;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.control.ControlCondicionComercial;
import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.Cliente;

/* Pantalla portal del Cliente: catálogo, pedido en curso y mis pedidos.*/
public class VentanaCliente extends VentanaPrincipal {

    static final String CATALOGO = "Catálogo y nuevo pedido";
    static final String MIS_PEDIDOS = "Mis pedidos";

    private final Cliente cliente;

    public VentanaCliente(Cliente cliente, ConexionBD conexionBD) {
        super(cliente, conexionBD);
        this.cliente = cliente;
    }

    @Override
    protected void agregarOpciones(JMenuBar barra) {
        ControlPedido controlPedido = new ControlPedido(getConexionBD(),
                new ControlCondicionComercial(getConexionBD()));

        JMenu menu = new JMenu("Pedidos");
        agregarPantalla(menu, CATALOGO, new InterfazPedido(cliente, controlPedido,
                () -> mostrarPantalla(MIS_PEDIDOS)));
        agregarPantalla(menu, MIS_PEDIDOS, new InterfazMisPedidos(cliente, controlPedido));
        barra.add(menu);
    }

    @Override
    protected String pantallaInicial() {
        return CATALOGO;
    }
}
