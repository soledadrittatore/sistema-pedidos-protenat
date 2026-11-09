package com.protenat.pedidos.persistencia;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.PersonalAdministrativo;
import com.protenat.pedidos.dominio.Usuario;
import com.protenat.pedidos.excepciones.BaseDatosException;

/* Implementación MySQL de UsuarioDAO sobre las tablas usuario, cliente y personal_administrativo.*/
public class UsuarioDAOMySQL extends DAOMySQL implements UsuarioDAO {

    private static final String SQL_USUARIO =
            "SELECT u.id_usuario, u.email, u.clave, u.activo, u.tipo_usuario, "
            + "c.id_cliente, c.cuit_cuil, c.razon_social, c.telefono, c.direccion, c.localidad, "
            + "p.legajo, p.nombre, p.apellido, p.fecha_ingreso "
            + "FROM usuario u "
            + "LEFT JOIN cliente c ON c.id_usuario = u.id_usuario "
            + "LEFT JOIN personal_administrativo p ON p.id_usuario = u.id_usuario "
            + "WHERE u.email = ?";

    public UsuarioDAOMySQL(ConexionBD conexionBD) {
        super(conexionBD);
    }

    @Override
    protected String getEntidad() {
        return "usuarios";
    }

    @Override
    public Usuario buscarPorEmail(String email) throws BaseDatosException {
        return consultarUno(SQL_USUARIO, this::crearUsuario, email);
    }

    @Override
    public boolean existeEmail(String email) throws BaseDatosException {
        return consultarUno("SELECT 1 FROM usuario WHERE email = ?", rs -> Boolean.TRUE, email) != null;
    }

    @Override
    public boolean existeCuitCuil(String cuitCuil) throws BaseDatosException {
        return consultarUno("SELECT 1 FROM cliente WHERE cuit_cuil = ?", rs -> Boolean.TRUE, cuitCuil) != null;
    }

    @Override
    public void registrarCliente(Cliente cliente) throws BaseDatosException {

        // Si falla el segundo INSERT se deshace el primero: no queda un usuario sin su cliente
        ejecutarEnTransaccion("registrar el cliente", con -> {

            try (PreparedStatement ps = prepararInsercion(con,
                    "INSERT INTO usuario (email, clave, activo, tipo_usuario) VALUES (?, ?, 1, 'CLIENTE')")) {
                asignarParametros(ps, new Object[] {cliente.getEmail(), cliente.getClave()});
                ps.executeUpdate();
                cliente.setIdUsuario(claveGenerada(ps));
            }

            try (PreparedStatement ps = prepararInsercion(con,
                    "INSERT INTO cliente (id_usuario, cuit_cuil, razon_social, telefono, direccion, localidad) "
                    + "VALUES (?, ?, ?, ?, ?, ?)")) {
                asignarParametros(ps, new Object[] {cliente.getIdUsuario(), cliente.getCuitCuil(),
                    cliente.getRazonSocial(), cliente.getTelefono(), cliente.getDireccion(),
                    cliente.getLocalidad()});
                ps.executeUpdate();
                cliente.setIdCliente(claveGenerada(ps));
            }
        });
    }

    // Crea el objeto de la subclase que corresponde según tipo_usuario
    private Usuario crearUsuario(ResultSet rs) throws SQLException {

        if ("CLIENTE".equals(rs.getString("tipo_usuario"))) {
            Cliente cliente = new Cliente(
                    rs.getInt("id_usuario"),
                    rs.getString("email"),
                    rs.getString("clave"),
                    rs.getBoolean("activo"),
                    rs.getString("cuit_cuil"),
                    rs.getString("razon_social"),
                    rs.getString("telefono"),
                    rs.getString("direccion"),
                    rs.getString("localidad"));
            cliente.setIdCliente(rs.getInt("id_cliente"));
            return cliente;
        }

        Date ingreso = rs.getDate("fecha_ingreso");
        return new PersonalAdministrativo(
                rs.getInt("id_usuario"),
                rs.getString("email"),
                rs.getString("clave"),
                rs.getBoolean("activo"),
                rs.getString("legajo"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                ingreso == null ? null : ingreso.toLocalDate());
    }
}
