package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.util.Formato;

/* Pantalla Consultar mis pedidos. */
public class InterfazMisPedidos extends PantallaBase {

    private final Cliente cliente;
    private final ControlPedido controlPedido;
    private List<Pedido> pedidos = new ArrayList<>();

    private final TablaSoloLectura modelo =
            new TablaSoloLectura("N.° pedido", "Fecha", "Total", "Estado");
    private final JTable tabla = Estilo.tabla(modelo);
    private final PanelDetallePedido detalle = new PanelDetallePedido();

    public InterfazMisPedidos(Cliente cliente, ControlPedido controlPedido) {
        super("Mis pedidos", "Seleccioná un pedido para ver sus productos, su estado y las observaciones.");
        this.cliente = cliente;
        this.controlPedido = controlPedido;

        JPanel lista = Estilo.titulado("Pedidos realizados");
        lista.add(Estilo.desplazable(tabla, 200), BorderLayout.CENTER);
        JPanel panelDetalle = Estilo.titulado("Detalle");
        panelDetalle.add(detalle, BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lista, panelDetalle);
        division.setResizeWeight(0.5);
        add(division, BorderLayout.CENTER);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                verDetalle();
            }
        });
    }

    @Override
    public void actualizar() {
        modelo.vaciar();
        detalle.mostrar(null);
        try {
            pedidos = controlPedido.consultarPedidosCliente(cliente);
            for (Pedido p : pedidos) {
                modelo.addRow(new Object[] {p.getIdPedido(), Formato.fechaHora(p.getFecha()),
                    Formato.moneda(p.getTotal()), p.getEstado()});
            }
            if (pedidos.isEmpty()) {
                Mensajes.informar(this, "No hay pedidos para mostrar.");
            } else {
                tabla.setRowSelectionInterval(0, 0); // el más reciente
            }
        } catch (BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void verDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        try {
            detalle.mostrar(controlPedido.consultarPedido(pedidos.get(fila).getIdPedido()));
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }
}
