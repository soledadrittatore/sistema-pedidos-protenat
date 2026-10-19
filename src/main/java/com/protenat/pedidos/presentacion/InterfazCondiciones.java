package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.protenat.pedidos.control.ControlCondicionComercial;
import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.util.Formato;

/**
 * CU-09 Gestionar condiciones comerciales, CU-11 Modificar importe mínimo de
 * compra y CU-12 Modificar cantidad máxima por producto.
 */
public class InterfazCondiciones extends PantallaBase {

    private final ControlCondicionComercial controlCondicion;
    private final ControlPedido controlPedido;
    private List<Producto> productos = new ArrayList<>();

    private final JLabel lblVigente = new JLabel();
    private final JTextField txtImporte = new JTextField(10);
    private final JTextField txtCantidad = new JTextField(5);
    private final TablaSoloLectura modelo =
            new TablaSoloLectura("Código", "Producto", "Línea", "Presentación", "Precio", "Cantidad máxima");
    private final JTable tabla = Estilo.tabla(modelo);

    public InterfazCondiciones(ControlCondicionComercial controlCondicion, ControlPedido controlPedido) {
        super("Condiciones comerciales",
                "Consultá las condiciones vigentes y modificá el importe mínimo o la cantidad máxima de un producto.");
        this.controlCondicion = controlCondicion;
        this.controlPedido = controlPedido;

        JPanel importe = Estilo.titulado("Importe mínimo de compra (CU-11)");
        JPanel filaImporte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblVigente.setFont(Estilo.NEGRITA);
        JButton btnImporte = new JButton("Modificar importe mínimo");
        filaImporte.add(lblVigente);
        filaImporte.add(new JLabel("    Nuevo importe: $"));
        filaImporte.add(txtImporte);
        filaImporte.add(btnImporte);
        importe.add(filaImporte, BorderLayout.CENTER);

        JPanel cantidades = Estilo.titulado("Cantidad máxima por producto (CU-12)");
        cantidades.add(Estilo.desplazable(tabla, 300), BorderLayout.CENTER);
        JPanel filaCantidad = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCantidad = new JButton("Modificar cantidad máxima");
        filaCantidad.add(new JLabel("Nueva cantidad máxima del producto seleccionado:"));
        filaCantidad.add(txtCantidad);
        filaCantidad.add(btnCantidad);
        cantidades.add(filaCantidad, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(importe, BorderLayout.NORTH);
        centro.add(cantidades, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        btnImporte.addActionListener(e -> modificarImporte());
        btnCantidad.addActionListener(e -> modificarCantidad());
    }

    @Override
    public void actualizar() {
        try {
            CondicionComercial vigente = controlCondicion.consultarCondiciones();
            lblVigente.setText("Vigente: " + Formato.moneda(vigente.getImporteMinimoCompra())
                    + " desde " + vigente.getFechaDesde().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            productos = controlPedido.consultarProductos();
            modelo.vaciar();
            for (Producto p : productos) {
                modelo.addRow(new Object[] {p.getCodigo(), p.getNombre(), p.getLinea().getNombre(),
                    p.getPresentacion(), Formato.moneda(p.getPrecio()), p.getCantidadMaxima()});
            }
        } catch (BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void modificarImporte() {
        try {
            BigDecimal nuevo = leerImporte(txtImporte.getText());
            if (Mensajes.confirmar(this, "¿Confirmás el nuevo importe mínimo de " + Formato.moneda(nuevo) + "?")) {
                controlCondicion.modificarImporteMinimo(nuevo);
                Mensajes.informar(this, "La modificación se realizó correctamente.");
                txtImporte.setText("");
                actualizar();
            }
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void modificarCantidad() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.advertir(this, "Seleccioná el producto que querés modificar.");
            return;
        }
        Producto producto = productos.get(fila);
        try {
            int nueva = leerEntero(txtCantidad.getText());
            if (Mensajes.confirmar(this, "¿Confirmás la cantidad máxima de " + nueva
                    + " unidades para " + producto + "?")) {
                controlCondicion.modificarCantidadMaxima(producto, nueva);
                Mensajes.informar(this, "La modificación se realizó correctamente.");
                txtCantidad.setText("");
                actualizar();
            }
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    /* Convierte el texto en número; si no es válido lanza la excepción con MSG-15 */
    private BigDecimal leerImporte(String texto) throws ValidacionException {
        try {
            String limpio = texto.trim().replace(".", "").replace(",", ".");
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new ValidacionException(ControlCondicionComercial.MSG_VALOR_INVALIDO);
        }
    }

    private int leerEntero(String texto) throws ValidacionException {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new ValidacionException(ControlCondicionComercial.MSG_VALOR_INVALIDO);
        }
    }
}
