package com.protenat.pedidos.persistencia;

import java.util.List;

import com.protenat.pedidos.dominio.Producto;
import com.protenat.pedidos.excepciones.BaseDatosException;

/* Operaciones de persistencia de productos (CU-03 y CU-12). */
public interface ProductoDAO {

    /* Productos habilitados para el canal de pedidos directos, con su línea. */
    List<Producto> listarActivos() throws BaseDatosException;

    /* Devuelve true si el producto existía y se actualizó. */
    boolean actualizarCantidadMaxima(int idProducto, int cantidadMaxima) throws BaseDatosException;
}
