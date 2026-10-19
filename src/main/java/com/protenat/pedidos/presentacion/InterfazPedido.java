package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.DetallePedido;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.util.Formato;

/* CU-03 Consultar productos, CU-04 Gestionar pedido y CU-10 Validar
 * condiciones comerciales (figura 10). */
public class InterfazPedido extends PantallaBase {

    private static final String[] CRITERIOS = {"Código", "Nombre", "Precio"};

    private final Cliente cliente;
    private final ControlPedido controlPedido;
    private final Runnable irAMisPedidos;

    private List<Producto> catalogo = new ArrayList<>();
    private Pedido pedido;

    private final TablaSoloLectura modeloCatalogo =
            new TablaSoloLectura("Código", "Producto", "Línea", "Presentación", "Precio", "Máx. por pedido");
    private final JTable tablaCatalogo = Estilo.tabla(modeloCatalogo);
    private final TablaSoloLectura modeloPedido =
            new TablaSoloLectura("Código", "Producto", "Cantidad", "Precio unit.", "Subtotal");
    private final JTable tablaPedido = Estilo.tabla(modeloPedido);

    private final JTextField txtBuscar = new JTextField(10);
    private final JComboBox<String> cmbOrden = new JComboBox<>(CRITERIOS);
    private final JCheckBox chkDescendente = new JCheckBox("Descendente");
    private final JSpinner spnCantidad = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));

    private final JLabel lblTotal = new JLabel();
    private final JLabel lblMinimo = new JLabel();
    private final JLabel lblFaltante = new JLabel();
    private final JButton btnRevisar = Estilo.botonPrincipal("Revisar pedido");

    public InterfazPedido(Cliente cliente, ControlPedido controlPedido, Runnable irAMisPedidos) {
        super("Catálogo y pedido en curso",
                "Elegí un producto, indicá la cantidad y agregalo. El total se actualiza solo.");
        this.cliente = cliente;
        this.controlPedido = controlPedido;
        this.irAMisPedidos = irAMisPedidos;
        construir();
    }

    private void construir() {
        // ---- Catálogo ----
        JPanel panelCatalogo = Estilo.titulado("Productos habilitados");
        JPanel barraCatalogo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnBuscar = new JButton("Buscar");
        barraCatalogo.add(new JLabel("Código:"));
        barraCatalogo.add(txtBuscar);
        barraCatalogo.add(btnBuscar);
        barraCatalogo.add(new JLabel("   Ordenar por:"));
        barraCatalogo.add(cmbOrden);
        barraCatalogo.add(chkDescendente);
        panelCatalogo.add(barraCatalogo, BorderLayout.NORTH);
        panelCatalogo.add(Estilo.desplazable(tablaCatalogo, 220), BorderLayout.CENTER);

        JPanel barraAgregar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAgregar = Estilo.botonPrincipal("Agregar al pedido");
        barraAgregar.add(new JLabel("Cantidad:"));
        barraAgregar.add(spnCantidad);
        barraAgregar.add(btnAgregar);
        panelCatalogo.add(barraAgregar, BorderLayout.SOUTH);

        // ---- Pedido en curso ----
        JPanel panelPedido = Estilo.titulado("Pedido en curso");
        panelPedido.add(Estilo.desplazable(tablaPedido, 150), BorderLayout.CENTER);

        JPanel resumen = new JPanel(new GridLayout(3, 1));
        resumen.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        lblTotal.setFont(Estilo.NEGRITA);
        resumen.add(lblTotal);
        resumen.add(lblMinimo);
        resumen.add(lblFaltante);
        panelPedido.add(resumen, BorderLayout.EAST);

        JPanel barraPedido = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnQuitar = new JButton("Quitar producto");
        JButton btnDeshacer = new JButton("Deshacer último cambio");
        JButton btnCancelar = new JButton("Cancelar pedido");
        barraPedido.add(btnQuitar);
        barraPedido.add(btnDeshacer);
        barraPedido.add(btnCancelar);
        barraPedido.add(btnRevisar);
        panelPedido.add(barraPedido, BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelCatalogo, panelPedido);
        division.setResizeWeight(0.6);
        add(division, BorderLayout.CENTER);

        // ---- Acciones ----
        btnBuscar.addActionListener(e -> buscar());
        txtBuscar.addActionListener(e -> buscar());
        cmbOrden.addActionListener(e -> ordenar());
        chkDescendente.addActionListener(e -> ordenar());
        btnAgregar.addActionListener(e -> agregar());
        btnQuitar.addActionListener(e -> quitar());
        btnDeshacer.addActionListener(e -> deshacer());
        btnCancelar.addActionListener(e -> cancelar());
        btnRevisar.addActionListener(e -> revisarYConfirmar());
    }

    @Override
    public void actualizar() {
        try {
            catalogo = controlPedido.consultarProductos();
            if (pedido == null) {
                pedido = controlPedido.iniciarPedido(cliente);
            }
            ordenar();
            if (catalogo.isEmpty()) {
                Mensajes.informar(this, "Actualmente no existen productos habilitados para consulta.");
            }
        } catch (BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
        refrescarPedido();
    }

    /* Catálogo de productos: ordenación y búsqueda */

    private void ordenar() {
        Comparator<Producto> criterio;
        switch (cmbOrden.getSelectedIndex()) {
            case 1:
                criterio = ControlPedido.POR_NOMBRE;
                break;
            case 2:
                criterio = ControlPedido.POR_PRECIO;
                break;
            default:
                criterio = ControlPedido.POR_CODIGO;
        }
        controlPedido.ordenarProductos(catalogo, criterio, chkDescendente.isSelected());
        mostrarCatalogo();
    }

    private void mostrarCatalogo() {
        modeloCatalogo.vaciar();
        for (Producto p : catalogo) {
            modeloCatalogo.addRow(new Object[] {p.getCodigo(), p.getNombre(), p.getLinea().getNombre(),
                p.getPresentacion(), Formato.moneda(p.getPrecio()), p.getCantidadMaxima()});
        }
    }

    private void buscar() {
        try {
            Producto encontrado = controlPedido.buscarProducto(catalogo, txtBuscar.getText());
            int fila = catalogo.indexOf(encontrado);
            tablaCatalogo.setRowSelectionInterval(fila, fila);
            tablaCatalogo.scrollRectToVisible(tablaCatalogo.getCellRect(fila, 0, true));
        } catch (ValidacionException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void agregar() {
        int fila = tablaCatalogo.getSelectedRow();
        if (fila < 0) {
            Mensajes.advertir(this, "Seleccioná un producto del catálogo.");
            return;
        }
        Producto producto = catalogo.get(fila);
        int cantidad = (Integer) spnCantidad.getValue();
        try {
            controlPedido.agregarProducto(pedido, producto, cantidad);
            spnCantidad.setValue(1);
            refrescarPedido();
        } catch (ValidacionException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void quitar() {
        int fila = tablaPedido.getSelectedRow();
        String codigo = fila < 0 ? "" : (String) modeloPedido.getValueAt(fila, 0);
        try {
            controlPedido.quitarProducto(pedido, codigo);
            refrescarPedido();
        } catch (ValidacionException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void deshacer() {
        try {
            controlPedido.deshacerUltimoCambio(pedido);
            refrescarPedido();
        } catch (ValidacionException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void cancelar() {
        if (pedido.estaVacio() || Mensajes.confirmar(this, "¿Cancelás el pedido en curso? No se va a registrar.")) {
            reiniciarPedido();
        }
    }

    private void refrescarPedido() {
        modeloPedido.vaciar();
        if (pedido == null) {
            return;
        }
        for (DetallePedido d : pedido.getDetalles()) {
            modeloPedido.addRow(new Object[] {d.getProducto().getCodigo(),
                d.getProducto().getNombre() + " " + d.getProducto().getPresentacion(),
                d.getCantidad(), Formato.moneda(d.getPrecioUnitario()), Formato.moneda(d.getSubtotal())});
        }
        BigDecimal faltante = controlPedido.calcularFaltante(pedido);
        lblTotal.setText("Total: " + Formato.moneda(pedido.getTotal()));
        lblMinimo.setText("Importe mínimo: "
                + Formato.moneda(pedido.getCondicionComercial().getImporteMinimoCompra()));
        lblFaltante.setText(faltante.signum() > 0
                ? "Te faltan " + Formato.moneda(faltante)
                : "Importe mínimo alcanzado");
        // Revisar pedido solo se habilita cuando se cumplen las condiciones 
        btnRevisar.setEnabled(!pedido.estaVacio() && faltante.signum() == 0);
    }

    private void revisarYConfirmar() {
        try {
            controlPedido.validarPedido(pedido);
            if (Mensajes.confirmar(this, armarResumen())) {
                int numero = controlPedido.generarPedido(pedido);
                Mensajes.informar(this, "Pedido N.° " + numero + " registrado. Estado: PENDIENTE.");
                reiniciarPedido();
                irAMisPedidos.run();
            }
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private String armarResumen() {
        StringBuilder sb = new StringBuilder("Resumen del pedido\n\n");
        for (DetallePedido d : pedido.getDetalles()) {
            sb.append(d.getCantidad()).append(" x ").append(d.getProducto()).append("  ")
              .append(Formato.moneda(d.getSubtotal())).append('\n');
        }
        sb.append("\nTotal: ").append(Formato.moneda(pedido.getTotal()));
        sb.append("\n\n¿Confirmás el pedido?");
        return sb.toString();
    }

    private void reiniciarPedido() {
        try {
            controlPedido.cancelarGestion();
            pedido = controlPedido.iniciarPedido(cliente);
        } catch (BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
        refrescarPedido();
    }
}
