package com.protenat.pedidos.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;

/* Convierte la fila actual de un ResultSet en un objeto del dominio. Cada DAO
 * indica cómo se arma su objeto y DAOMySQL se encarga de recorrer las filas.
 *
 * @param <T> tipo de objeto que se obtiene de cada fila
 */
@FunctionalInterface
public interface Mapeador<T> {

    T mapear(ResultSet rs) throws SQLException;
}
