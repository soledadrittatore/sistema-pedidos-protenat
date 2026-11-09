package com.protenat.pedidos.persistencia;

/**
 * Crea los DAO de MySQL a partir de la conexión. Es el único lugar donde se
 * nombran las clases concretas: las ventanas y los controles trabajan con las
 * interfaces, así que cambiar de motor de base solo modifica esta clase.
 */
public class FabricaDAO {

    private final ConexionBD conexionBD;

    public FabricaDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    public UsuarioDAO crearUsuarioDAO() {
        return new UsuarioDAOMySQL(conexionBD);
    }

    public ProductoDAO crearProductoDAO() {
        return new ProductoDAOMySQL(conexionBD);
    }

    public PedidoDAO crearPedidoDAO() {
        return new PedidoDAOMySQL(conexionBD);
    }

    public CondicionComercialDAO crearCondicionComercialDAO() {
        return new CondicionComercialDAOMySQL(conexionBD);
    }

    public ConexionBD getConexionBD() {
        return conexionBD;
    }
}
