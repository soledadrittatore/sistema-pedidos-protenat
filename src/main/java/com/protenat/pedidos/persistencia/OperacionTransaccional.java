package com.protenat.pedidos.persistencia;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Conjunto de sentencias que deben ejecutarse todas o ninguna. Lo usa
 * DAOMySQL.ejecutarEnTransaccion() para hacer commit o rollback.
 */
@FunctionalInterface
public interface OperacionTransaccional {

    void ejecutar(Connection con) throws SQLException;
}
