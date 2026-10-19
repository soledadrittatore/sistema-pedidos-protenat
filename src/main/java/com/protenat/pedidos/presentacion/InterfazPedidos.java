package com.protenat.pedidos.presentacion;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.protenat.pedidos.control.ControlPedido;
import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.util.Formato;

/* CU-06 Gestionar pedidos recibidos, CU-07 Verificar pedido y CU-08 Resolver pedido */
public class InterfazPedidos extends PantallaBase {

    private static final String TODOS = "Todos";

    private final ControlPedido controlPedido;
    private List<Pedido> pedidosMostrados = new ArrayList<>();
    private Pedido seleccionado;

    private final JComboBox<Object> cmbEstado = new JComboBox<>(new Object[] {TODOS,
        EstadoPedido.PENDIENTE, EstadoPedido.CONFIRMADO, EstadoPedido.RECHAZADO});
    private final JTextField txtFiltro = new JTextField(14);
    private final JLabel lblCola = new JLabel();

    private final TablaSoloLectura modelo = new TablaSoloLectura(
            "N.° pedido", "Cliente", "Fecha", "Total", "Estado", "Pago", "Disponibilidad");
    private final JTable tabla = Estilo.tabla(modelo);
    private final PanelDetallePedido detalle = new PanelDetallePedido();

    private final JComboBox<EstadoVerificacion> cmbPago = new JComboBox<>(EstadoVerificacion.values());
    private final JComboBox<EstadoVerificacion> cmbDisponibilidad = new JComboBox<>(EstadoVerificacion.values());
    private final JTextField txtObservaciones = new JTextField(30);
    private final JButton btnGuardar = new JButton("Guardar verificaciones");
    private final JButton btnConfirmar = Estilo.botonPrincipal("Confirmar pedido");
    private final JButton btnRechazar = new JButton("Rechazar pedido");

    public InterfazPedidos(ControlPedido controlPedido) {
        super("Pedidos recibidos",
                "Filtrá la bandeja, seleccioná un pedido, registrá las verificaciones y resolvelo.");
        this.controlPedido = controlPedido;
        construir();
    }

    private void construir() {
        /* Bandeja con filtros */
        JPanel bandeja = Estilo.titulado("Bandeja de pedidos");
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnFiltrar = new JButton("Aplicar filtros");
        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnSiguiente = Estilo.botonPrincipal("Atender siguiente pendiente");
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbEstado);
        filtros.add(new JLabel("  N.° o cliente:"));
        filtros.add(txtFiltro);
        filtros.add(btnFiltrar);
        filtros.add(btnLimpiar);
        filtros.add(new JLabel("   "));
        filtros.add(btnSiguiente);
        filtros.add(lblCola);
        bandeja.add(filtros, BorderLayout.NORTH);
        bandeja.add(Estilo.desplazable(tabla, 170), BorderLayout.CENTER);

        /* Detalle, verificaciones y resolución */
        JPanel inferior = new JPanel(new BorderLayout(6, 6));
        JPanel panelDetalle = Estilo.titulado("Detalle del pedido");
        panelDetalle.add(detalle, BorderLayout.CENTER);
        inferior.add(panelDetalle, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new BorderLayout());
        JPanel verificaciones = Estilo.titulado("Verificaciones (CU-07)");
        JPanel filaVerif = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0;
        filaVerif.add(new JLabel("Pago:"), g);
        g.gridx = 1;
        filaVerif.add(cmbPago, g);
        g.gridx = 2;
        filaVerif.add(new JLabel("Disponibilidad:"), g);
        g.gridx = 3;
        filaVerif.add(cmbDisponibilidad, g);
        g.gridx = 0; g.gridy = 1;
        filaVerif.add(new JLabel("Observaciones:"), g);
        g.gridx = 1; g.gridwidth = 3; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        filaVerif.add(txtObservaciones, g);
        g.gridx = 4; g.gridwidth = 1; g.fill = GridBagConstraints.NONE; g.weightx = 0;
        filaVerif.add(btnGuardar, g);
        verificaciones.add(filaVerif, BorderLayout.CENTER);

        JPanel resolucion = Estilo.titulado("Resolución (CU-08)");
        JPanel filaRes = new JPanel(new GridLayout(2, 1, 4, 6));
        filaRes.add(btnConfirmar);
        filaRes.add(btnRechazar);
        resolucion.add(filaRes, BorderLayout.CENTER);

        acciones.add(verificaciones, BorderLayout.CENTER);
        acciones.add(resolucion, BorderLayout.EAST);
        inferior.add(acciones, BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, bandeja, inferior);
        division.setResizeWeight(0.45);
        add(division, BorderLayout.CENTER);

        /* Acciones */
        btnFiltrar.addActionListener(e -> cargarBandeja());
        txtFiltro.addActionListener(e -> cargarBandeja());
        btnLimpiar.addActionListener(e -> {
            cmbEstado.setSelectedIndex(0);
            txtFiltro.setText("");
            cargarBandeja();
        });
        btnSiguiente.addActionListener(e -> atenderSiguiente());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionar();
            }
        });
        btnGuardar.addActionListener(e -> guardarVerificaciones());
        btnConfirmar.addActionListener(e -> confirmar());
        btnRechazar.addActionListener(e -> rechazar());
        habilitarAcciones();
    }

    @Override
    public void actualizar() {
        cargarBandeja();
    }

    private void cargarBandeja() {
        try {
            Object estado = cmbEstado.getSelectedItem();
            List<Pedido> pedidos = controlPedido.consultarPedidos(
                    estado == TODOS ? null : (EstadoPedido) estado);
            pedidosMostrados = controlPedido.filtrarPedidos(pedidos, txtFiltro.getText());
            modelo.vaciar();
            for (Pedido p : pedidosMostrados) {
                modelo.addRow(new Object[] {p.getIdPedido(), p.getCliente().getRazonSocial(),
                    Formato.fechaHora(p.getFecha()), Formato.moneda(p.getTotal()), p.getEstado(),
                    p.getEstadoPago(), p.getEstadoDisponibilidad()});
            }
            lblCola.setText("Pendientes en cola: " + controlPedido.obtenerColaPendientes().size());
            seleccionado = null;
            detalle.mostrar(null);
            habilitarAcciones();
            if (pedidosMostrados.isEmpty()) {
                Mensajes.informar(this, pedidos.isEmpty() ? "No hay pedidos para mostrar."
                        : "No hay pedidos que coincidan con los filtros. Modificalos o limpialos.");
            }
        } catch (BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    // Toma el primer pedido de la cola de pendientes (el más antiguo)
    private void atenderSiguiente() {
        try {
            Queue<Pedido> cola = controlPedido.obtenerColaPendientes();
            Pedido siguiente = cola.poll();
            if (siguiente == null) {
                Mensajes.informar(this, "No hay pedidos pendientes para atender.");
                return;
            }
            cmbEstado.setSelectedItem(EstadoPedido.PENDIENTE);
            txtFiltro.setText("");
            cargarBandeja();
            for (int i = 0; i < pedidosMostrados.size(); i++) {
                if (pedidosMostrados.get(i).getIdPedido() == siguiente.getIdPedido()) {
                    tabla.setRowSelectionInterval(i, i);
                    break;
                }
            }
        } catch (BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void seleccionar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        try {
            seleccionado = controlPedido.consultarPedido(pedidosMostrados.get(fila).getIdPedido());
            detalle.mostrar(seleccionado);
            cmbPago.setSelectedItem(seleccionado.getEstadoPago());
            cmbDisponibilidad.setSelectedItem(seleccionado.getEstadoDisponibilidad());
            txtObservaciones.setText(seleccionado.getObservaciones() == null ? "" : seleccionado.getObservaciones());
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
        habilitarAcciones();
    }

    private void guardarVerificaciones() {
        try {
            controlPedido.verificarPedido(seleccionado,
                    (EstadoVerificacion) cmbPago.getSelectedItem(),
                    (EstadoVerificacion) cmbDisponibilidad.getSelectedItem(),
                    txtObservaciones.getText().trim());
            Mensajes.informar(this, "Las verificaciones se registraron correctamente.");
            refrescarSeleccionado();
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void confirmar() {
        try {
            controlPedido.validarVerificaciones(seleccionado);
            if (Mensajes.confirmar(this, "¿Confirmás el pedido N.° " + seleccionado.getIdPedido() + "?")) {
                controlPedido.confirmarPedido(seleccionado);
                Mensajes.informar(this, "Pedido N.° " + seleccionado.getIdPedido() + " CONFIRMADO.");
                refrescarSeleccionado();
            }
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    private void rechazar() {
        if (!Mensajes.confirmar(this, "¿Confirmás el rechazo del pedido N.° " + seleccionado.getIdPedido() + "?")) {
            return;
        }
        try {
            controlPedido.rechazarPedido(seleccionado, txtObservaciones.getText());
            Mensajes.informar(this, "Pedido N.° " + seleccionado.getIdPedido() + " RECHAZADO.");
            refrescarSeleccionado();
        } catch (ValidacionException | BaseDatosException e) {
            Mensajes.mostrar(this, e);
        }
    }

    /* Recarga la bandeja y vuelve a seleccionar el mismo pedido */
    private void refrescarSeleccionado() {
        int id = seleccionado.getIdPedido();
        cargarBandeja();
        for (int i = 0; i < pedidosMostrados.size(); i++) {
            if (pedidosMostrados.get(i).getIdPedido() == id) {
                tabla.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    /* Se deshabilitan los botones que no corresponden, solo un pedido
       PENDIENTE se verifica o resuelve, y Confirmar exige pago y disponibilidad APROBADAS. */
    private void habilitarAcciones() {
        boolean pendiente = seleccionado != null && seleccionado.estaPendiente();
        boolean aprobadas = pendiente
                && seleccionado.getEstadoPago() == EstadoVerificacion.APROBADA
                && seleccionado.getEstadoDisponibilidad() == EstadoVerificacion.APROBADA;
        cmbPago.setEnabled(pendiente);
        cmbDisponibilidad.setEnabled(pendiente);
        txtObservaciones.setEnabled(pendiente);
        btnGuardar.setEnabled(pendiente);
        btnRechazar.setEnabled(pendiente);
        btnConfirmar.setEnabled(aprobadas);
    }
}
