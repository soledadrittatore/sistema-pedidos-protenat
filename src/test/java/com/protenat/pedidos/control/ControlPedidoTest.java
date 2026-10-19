package com.protenat.pedidos.control;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.dominio.EstadoPedido;
import com.protenat.pedidos.dominio.EstadoVerificacion;
import com.protenat.pedidos.dominio.Linea;
import com.protenat.pedidos.dominio.Pedido;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.ValidacionException;

/**
 * Repite en Java los casos de prueba CP02 a CP05 del informe (sección 13).
 * Solo usan la lógica de ControlPedido, sin conectarse a la base.
 */
class ControlPedidoTest {

    private ControlPedido control;
    private Pedido pedido;
    private Producto col001;
    private Producto col002;
    private Producto col005;
    private Producto col006;
    private Producto col007;

    @BeforeEach
    void preparar() {
        control = new ControlPedido(null, null);
        Linea color = new Linea(1, "Línea Color", null);
        col001 = producto(1, "COL-001", "Shampoo Color", "280 ml", 12000, 10, color);
        col002 = producto(2, "COL-002", "Shampoo Color", "1,1 L", 28000, 5, color);
        col005 = producto(5, "COL-005", "Máscara Color", "200 g", 15000, 10, color);
        col006 = producto(6, "COL-006", "Máscara Color", "800 g", 32000, 5, color);
        col007 = producto(7, "COL-007", "Spray 10 en 1", "260 ml", 18000, 10, color);

        Cliente cliente = new Cliente(1, "estilonorte@correo.com", "clave123", true,
                "30-71234567-8", "Peluquería Estilo Norte", null, null, "Santa Fe");
        CondicionComercial vigente = new CondicionComercial(2, new BigDecimal("80000.00"),
                LocalDate.of(2026, 9, 1), true);
        pedido = new Pedido(cliente, vigente);
    }

    private Producto producto(int id, String codigo, String nombre, String presentacion,
                              int precio, int maximo, Linea linea) {
        return new Producto(id, codigo, nombre, null, presentacion, new BigDecimal(precio),
                maximo, true, linea);
    }

    // ----- CP02: cantidad máxima de COL-002 (máximo 5), valores límite -----

    @ParameterizedTest(name = "CP02 cantidad {0} → incorpora: {1}")
    @CsvSource({"0,false", "1,true", "4,true", "5,true", "6,false"})
    void cp02CantidadMaxima(int cantidad, boolean seIncorpora) {
        if (seIncorpora) {
            assertDoesNotThrow(() -> control.agregarProducto(pedido, col002, cantidad));
            assertEquals(cantidad, pedido.buscarDetalle("COL-002").getCantidad());
        } else {
            assertThrows(ValidacionException.class, () -> control.agregarProducto(pedido, col002, cantidad));
            assertTrue(pedido.estaVacio());
        }
    }

    @Test
    @DisplayName("CP02: el máximo se controla sobre la suma de lo que ya está en el pedido")
    void cp02SumaCantidades() throws ValidacionException {
        control.agregarProducto(pedido, col002, 3);
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> control.agregarProducto(pedido, col002, 3));
        assertTrue(ex.getMessage().contains("5 unidades"));
        assertEquals(3, pedido.buscarDetalle("COL-002").getCantidad());
    }

    // ----- CP03: importe mínimo de $80.000, valores límite -----

    @Test
    void cp03Total79000NoPermite() throws ValidacionException {
        control.agregarProducto(pedido, col006, 2);
        control.agregarProducto(pedido, col005, 1);
        assertEquals(0, new BigDecimal("79000").compareTo(pedido.getTotal()));
        ValidacionException ex = assertThrows(ValidacionException.class, () -> control.validarPedido(pedido));
        assertTrue(ex.getMessage().contains("Te faltan"));
    }

    @Test
    void cp03Total80000Permite() throws ValidacionException {
        control.agregarProducto(pedido, col002, 2);
        control.agregarProducto(pedido, col001, 2);
        assertEquals(0, new BigDecimal("80000").compareTo(pedido.getTotal()));
        assertDoesNotThrow(() -> control.validarPedido(pedido));
    }

    @Test
    void cp03Total81000Permite() throws ValidacionException {
        control.agregarProducto(pedido, col005, 3);
        control.agregarProducto(pedido, col007, 2);
        assertEquals(0, new BigDecimal("81000").compareTo(pedido.getTotal()));
        assertDoesNotThrow(() -> control.validarPedido(pedido));
    }

    // ----- CP04: cobertura de decisiones D1 (cantidad) y D2 (importe) -----

    @Test
    void cp04CaminoC1() throws ValidacionException {
        control.agregarProducto(pedido, col006, 2);
        control.agregarProducto(pedido, col007, 1);
        assertTrue(control.validarImporteMinimo(pedido, pedido.getCondicionComercial()));
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
    }

    @Test
    void cp04CaminoC2() {
        assertFalse(control.validarCantidad(col002, 6));
    }

    @Test
    void cp04CaminoC3() throws ValidacionException {
        control.agregarProducto(pedido, col001, 2);
        assertFalse(control.validarImporteMinimo(pedido, pedido.getCondicionComercial()));
    }

    // ----- CP05: confirmar según las verificaciones -----

    @ParameterizedTest(name = "CP05 pago {0} / disponibilidad {1} → confirma: {2}")
    @CsvSource({
        "APROBADA,APROBADA,true",
        "PENDIENTE,APROBADA,false",
        "APROBADA,PENDIENTE,false",
        "RECHAZADA,APROBADA,false",
        "APROBADA,RECHAZADA,false"})
    void cp05Verificaciones(EstadoVerificacion pago, EstadoVerificacion disponibilidad, boolean confirma) {
        pedido.setEstadoPago(pago);
        pedido.setEstadoDisponibilidad(disponibilidad);
        if (confirma) {
            assertDoesNotThrow(() -> control.validarVerificaciones(pedido));
        } else {
            assertThrows(ValidacionException.class, () -> control.validarVerificaciones(pedido));
        }
    }

    // ----- Pila: deshacer el último cambio -----

    @Test
    void deshacerUltimoCambio() throws ValidacionException {
        control.agregarProducto(pedido, col001, 2);
        control.agregarProducto(pedido, col001, 3);
        control.quitarProducto(pedido, "COL-001");
        assertTrue(pedido.estaVacio());

        control.deshacerUltimoCambio(pedido); // vuelve a 5
        assertEquals(5, pedido.buscarDetalle("COL-001").getCantidad());
        control.deshacerUltimoCambio(pedido); // vuelve a 2
        assertEquals(2, pedido.buscarDetalle("COL-001").getCantidad());
        control.deshacerUltimoCambio(pedido); // el producto no estaba
        assertTrue(pedido.estaVacio());
        assertThrows(ValidacionException.class, () -> control.deshacerUltimoCambio(pedido));
    }
}
