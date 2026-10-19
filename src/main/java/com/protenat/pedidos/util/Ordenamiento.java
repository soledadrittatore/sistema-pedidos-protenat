package com.protenat.pedidos.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/* Algoritmos de ordenación y búsqueda implementados sin usar los de la API de
 * Java, para mostrar su funcionamiento. Son genéricos: sirven para productos,
 * pedidos o cualquier otro tipo, según el criterio (Comparator) recibido.
 */
public final class Ordenamiento {

    private Ordenamiento() {
    }

    /*
     * Ordenación por inserción. Recorre la lista y va ubicando cada elemento
     * en su lugar dentro de la parte ya ordenada. Es estable: dos productos con
     * el mismo precio conservan su orden anterior.
     */
    public static <T> void ordenarPorInsercion(List<T> lista, Comparator<T> criterio) {
        for (int i = 1; i < lista.size(); i++) {
            T actual = lista.get(i);
            int j = i - 1;
            while (j >= 0 && criterio.compare(lista.get(j), actual) > 0) {
                lista.set(j + 1, lista.get(j)); // desplaza el mayor una posición
                j--;
            }
            lista.set(j + 1, actual);
        }
    }

    /**
     * Búsqueda binaria. La lista debe estar ordenada por la misma clave que se
     * busca. En cada paso descarta la mitad de los elementos.
     *
     * @return el elemento encontrado o null si no existe.
     */
    public static <T> T busquedaBinaria(List<T> listaOrdenada, String claveBuscada,
                                        Function<T, String> obtenerClave) {
        int inicio = 0;
        int fin = listaOrdenada.size() - 1;
        while (inicio <= fin) {
            int medio = (inicio + fin) / 2;
            T elemento = listaOrdenada.get(medio);
            int comparacion = obtenerClave.apply(elemento).compareToIgnoreCase(claveBuscada);
            if (comparacion == 0) {
                return elemento;
            } else if (comparacion < 0) {
                inicio = medio + 1;
            } else {
                fin = medio - 1;
            }
        }
        return null;
    }
}
