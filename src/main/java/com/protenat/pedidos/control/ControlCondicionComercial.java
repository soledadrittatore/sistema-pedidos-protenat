package com.protenat.pedidos.control;

import java.math.BigDecimal;
import java.util.List;

import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.persistencia.CondicionComercialDAO;
import com.protenat.pedidos.persistencia.ProductoDAO;

/**
 * Coordina CU-09 Gestionar condiciones comerciales, CU-11 Modificar importe
 * mínimo de compra y CU-12 Modificar cantidad máxima por producto.
 */
public class ControlCondicionComercial {

    public static final String MSG_VALOR_INVALIDO = "Ingresá un valor numérico mayor que cero.";

    private final CondicionComercialDAO condicionDAO;
    private final ProductoDAO productoDAO;

    public ControlCondicionComercial(CondicionComercialDAO condicionDAO, ProductoDAO productoDAO) {
        this.condicionDAO = condicionDAO;
        this.productoDAO = productoDAO;
    }

    // Consulta la condición comercial vigente (activo = 1)
    public CondicionComercial consultarCondiciones() throws BaseDatosException {
        CondicionComercial vigente = condicionDAO.buscarVigente();
        if (vigente == null) {
            throw new BaseDatosException("No hay una condición comercial vigente", null);
        }
        return vigente;
    }

    // Importes mínimos anteriores, del más reciente al más antiguo
    public List<CondicionComercial> consultarHistorial() throws BaseDatosException {
        return condicionDAO.listarHistorial();
    }

    /**
     * CU-11. No sobrescribe la condición anterior: la desactiva y registra una
     * nueva, así cada pedido conserva la condición con la que se validó.
     */
    public CondicionComercial modificarImporteMinimo(BigDecimal nuevoImporte)
            throws ValidacionException, BaseDatosException {

        if (nuevoImporte == null || nuevoImporte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException(MSG_VALOR_INVALIDO);
        }

        condicionDAO.reemplazarVigente(nuevoImporte);
        return consultarCondiciones();
    }

    // CU-12. Modifica la cantidad máxima permitida para un producto
    public void modificarCantidadMaxima(Producto producto, int nuevaCantidad)
            throws ValidacionException, BaseDatosException {

        if (nuevaCantidad <= 0) {
            throw new ValidacionException(MSG_VALOR_INVALIDO);
        }

        if (!productoDAO.actualizarCantidadMaxima(producto.getIdProducto(), nuevaCantidad)) {
            throw new ValidacionException("El producto " + producto.getCodigo() + " ya no existe.");
        }

        // Se actualiza el objeto solo si la base se modificó sin errores
        producto.setCantidadMaxima(nuevaCantidad);
    }
}
