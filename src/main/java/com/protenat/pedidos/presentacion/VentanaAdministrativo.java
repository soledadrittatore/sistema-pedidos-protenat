package com.protenat.pedidos.presentacion;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.control.ControlCondicionComercial;
import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.PersonalAdministrativo;

/* Pantalla de Gestión administrativa (figura 11): bandeja de pedidos y condiciones comerciales. */
public class VentanaAdministrativo extends VentanaPrincipal {

    static final String BANDEJA = "Pedidos recibidos";
    static final String CONDICIONES = "Condiciones comerciales";

    public VentanaAdministrativo(PersonalAdministrativo personal, ConexionBD conexionBD) {
        super(personal, conexionBD);
    }

    @Override
    protected void agregarOpciones(JMenuBar barra) {
        ControlCondicionComercial controlCondicion = new ControlCondicionComercial(getConexionBD());
        ControlPedido controlPedido = new ControlPedido(getConexionBD(), controlCondicion);

        JMenu menu = new JMenu("Gestión");
        agregarPantalla(menu, BANDEJA, new InterfazPedidos(controlPedido));
        agregarPantalla(menu, CONDICIONES, new InterfazCondiciones(controlCondicion, controlPedido));
        barra.add(menu);
    }

    @Override
    protected String pantallaInicial() {
        return BANDEJA;
    }
}
