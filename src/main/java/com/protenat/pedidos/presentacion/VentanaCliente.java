package com.protenat.pedidos.presentacion;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.protenat.pedidos.persistencia.FabricaDAO;
import com.protenat.pedidos.control.ControlCondicionComercial;
import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.Cliente;

/**
 * Portal del Cliente (figura 10): catálogo, pedido en curso y mis pedidos.
 */
public class VentanaCliente extends VentanaPrincipal {

    static final String CATALOGO = "Catálogo y nuevo pedido";
    static final String MIS_PEDIDOS = "Mis pedidos";

    private final Cliente cliente;

    public VentanaCliente(Cliente cliente, FabricaDAO fabricaDAO) {
        super(cliente, fabricaDAO);
        this.cliente = cliente;
    }

    @Override
    protected void agregarOpciones(JMenuBar barra) {
        FabricaDAO fabrica = getFabricaDAO();
        ControlCondicionComercial controlCondicion = new ControlCondicionComercial(
                fabrica.crearCondicionComercialDAO(), fabrica.crearProductoDAO());
        ControlPedido controlPedido = new ControlPedido(fabrica.crearPedidoDAO(),
                fabrica.crearProductoDAO(), controlCondicion);

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
