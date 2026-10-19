package com.protenat.pedidos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrdenamientoTest {

    @Test
    void ordenaPorInsercion() {
        List<Integer> numeros = new ArrayList<>(List.of(32, 12, 28, 15, 12, 18));
        Ordenamiento.ordenarPorInsercion(numeros, Comparator.naturalOrder());
        assertEquals(List.of(12, 12, 15, 18, 28, 32), numeros);
    }

    @Test
    void busquedaBinariaEncuentraYNoEncuentra() {
        List<String> codigos = List.of("COL-001", "COL-002", "COL-005", "NUT-001", "PLE-002");
        assertEquals("NUT-001", Ordenamiento.busquedaBinaria(codigos, "nut-001", c -> c));
        assertNull(Ordenamiento.busquedaBinaria(codigos, "COL-003", c -> c));
    }

    @Test
    void validaCuitYCorreo() {
        assertTrue(Validador.esCuitValido("20-12345678-6"));
        assertFalse(Validador.esCuitValido("20-12345678-9"));
        assertFalse(Validador.esCuitValido("2012345678"));
        assertTrue(Validador.esEmailValido("compras@salonluna.com"));
        assertFalse(Validador.esEmailValido("compras@"));
    }
}
