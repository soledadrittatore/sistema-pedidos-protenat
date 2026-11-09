package com.protenat.pedidos.persistencia;

import java.math.BigDecimal;
import java.util.List;

import com.protenat.pedidos.dominio.CondicionComercial;
import com.protenat.pedidos.excepciones.BaseDatosException;

/* Operaciones de persistencia de las condiciones comerciales */
public interface CondicionComercialDAO {

    /* Condición vigente (activo = 1), o null si no hay ninguna. */
    CondicionComercial buscarVigente() throws BaseDatosException;

    /* Desactiva la condición vigente y registra una nueva con el importe
     * indicado, en una misma transacción (se conserva el historial).
     */
    void reemplazarVigente(BigDecimal nuevoImporte) throws BaseDatosException;

    /** Todas las condiciones, de la más reciente a la más antigua. */
    List<CondicionComercial> listarHistorial() throws BaseDatosException;
}
