package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.protenat.pedidos.dominio.DetallePedido;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.util.Formato;

/* Visualiza los dato de los datos y productos de un pedido. Puede ser usado por Mis pedidos y la bandeja administrativa. */
public class PanelDetallePedido extends JPanel {

    private final JLabel lblEncabezado = new JLabel(" ");
    private final JLabel lblEstados = new JLabel(" ");
    private final JLabel lblObservaciones = new JLabel(" ");
    private final TablaSoloLectura modelo =
            new TablaSoloLectura("Código", "Producto", "Cantidad", "Precio unit.", "Subtotal");

    public PanelDetallePedido() {
        super(new BorderLayout(4, 4));
        JPanel datos = new JPanel(new GridLayout(3, 1));
        lblEncabezado.setFont(Estilo.NEGRITA);
        datos.add(lblEncabezado);
        datos.add(lblEstados);
        datos.add(lblObservaciones);
        add(datos, BorderLayout.NORTH);
        add(Estilo.desplazable(Estilo.tabla(modelo), 120), BorderLayout.CENTER);
    }

    public void mostrar(Pedido pedido) {
        modelo.vaciar();
        if (pedido == null) {
            lblEncabezado.setText("Seleccioná un pedido para ver su detalle.");
            lblEstados.setText(" ");
            lblObservaciones.setText(" ");
            return;
        }
        lblEncabezado.setText("Pedido N.° " + pedido.getIdPedido() + "  |  "
                + pedido.getCliente().getRazonSocial() + "  |  " + Formato.fechaHora(pedido.getFecha())
                + "  |  Total " + Formato.moneda(pedido.getTotal()) + "  |  Estado " + pedido.getEstado());
        lblEstados.setText("Verificación del pago: " + pedido.getEstadoPago()
                + "   |   Disponibilidad: " + pedido.getEstadoDisponibilidad());
        String obs = pedido.getObservaciones();
        lblObservaciones.setText("Observaciones: " + (obs == null || obs.isBlank() ? "-" : obs));
        for (DetallePedido d : pedido.getDetalles()) {
            modelo.addRow(new Object[] {d.getProducto().getCodigo(),
                d.getProducto().getNombre() + " " + d.getProducto().getPresentacion(),
                d.getCantidad(), Formato.moneda(d.getPrecioUnitario()), Formato.moneda(d.getSubtotal())});
        }
    }
}
