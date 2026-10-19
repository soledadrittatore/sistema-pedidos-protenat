package com.protenat.pedidos.control;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.protenat.pedidos.conexion.ConexionBD;
import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.PersonalAdministrativo;
import com.protenat.pedidos.dominio.Usuario;
import com.protenat.pedidos.util.Validador;

/* Coordina CU-02 Iniciar sesión y CU-01 Registrarse. */
public class ControlLogin {

    public static final String MSG_CREDENCIALES = "El correo o la contraseña no son correctos.";
    public static final String MSG_INACTIVO = "Tu usuario no está habilitado. Comunicate con PROTENAT.";

    private static final String SQL_USUARIO =
            "SELECT u.id_usuario, u.email, u.clave, u.activo, u.tipo_usuario, "
            + "c.id_cliente, c.cuit_cuil, c.razon_social, c.telefono, c.direccion, c.localidad, "
            + "p.legajo, p.nombre, p.apellido, p.fecha_ingreso "
            + "FROM usuario u "
            + "LEFT JOIN cliente c ON c.id_usuario = u.id_usuario "
            + "LEFT JOIN personal_administrativo p ON p.id_usuario = u.id_usuario "
            + "WHERE u.email = ?";

    private final ConexionBD conexionBD;

    public ControlLogin(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /* Valida las credenciales ingresadas contra el usuario encontrado */
    public boolean validarCredenciales(Usuario usuario, String email, String clave) {

        if (usuario == null) {
            return false;
        }

        if (!usuario.getEmail().equalsIgnoreCase(email)) {
            return false;
        }

        return usuario.validarClave(clave);
    }

    /* CU-02. Devuelve un Cliente o un PersonalAdministrativo; la ventana lo
       recibe como Usuario y abre la pantalla de su perfil -polimorfismo-. */
    public Usuario iniciarSesion(String email, String clave)
            throws ValidacionException, BaseDatosException {

        if (Validador.esVacio(email) || Validador.esVacio(clave)) {
            throw new ValidacionException("Ingresá el correo y la contraseña.");
        }

        Usuario usuario = consultarUsuario(email.trim());

        // Clave incorrecta y correo inexistente muestran el mismo mensaje (CP01: CE2 y CE3)
        if (!validarCredenciales(usuario, email.trim(), clave)) {
            throw new ValidacionException(MSG_CREDENCIALES);
        }

        if (!usuario.isActivo()) {
            throw new ValidacionException(MSG_INACTIVO);
        }

        return usuario;
    }

    /* Busca el usuario por correo y crea el objeto de la subclase que corresponde */
    public Usuario consultarUsuario(String email) throws BaseDatosException {

        try (Connection con = conexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SQL_USUARIO)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

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

        } catch (SQLException e) {
            throw new BaseDatosException("Error al consultar el usuario " + email, e);
        }
    }

    /* CU-01. Valida los datos y registra usuario y cliente en una transacción. */
    public void registrarCliente(Cliente cliente)
            throws ValidacionException, BaseDatosException {

        validarDatosRegistro(cliente);

        try (Connection con = conexionBD.obtenerConexion()) {

            if (existe(con, "SELECT 1 FROM cliente WHERE cuit_cuil = ?", cliente.getCuitCuil())) {
                throw new ValidacionException("Ya existe un cliente registrado con ese CUIT/CUIL.");
            }
            if (existe(con, "SELECT 1 FROM usuario WHERE email = ?", cliente.getEmail())) {
                throw new ValidacionException("El correo ya está asociado a otro usuario. Ingresá otro.");
            }

            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO usuario (email, clave, activo, tipo_usuario) VALUES (?, ?, 1, 'CLIENTE')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, cliente.getEmail());
                    ps.setString(2, cliente.getClave());
                    ps.executeUpdate();
                    cliente.setIdUsuario(claveGenerada(ps));
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO cliente (id_usuario, cuit_cuil, razon_social, telefono, direccion, localidad) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, cliente.getIdUsuario());
                    ps.setString(2, cliente.getCuitCuil());
                    ps.setString(3, cliente.getRazonSocial());
                    ps.setString(4, cliente.getTelefono());
                    ps.setString(5, cliente.getDireccion());
                    ps.setString(6, cliente.getLocalidad());
                    ps.executeUpdate();
                    cliente.setIdCliente(claveGenerada(ps));
                }

                con.commit();

            } catch (SQLException e) {
                con.rollback(); // no queda un usuario sin su cliente
                throw e;
            }

        } catch (SQLException e) {
            throw new BaseDatosException("Error al registrar el cliente", e);
        }
    }

    // CU-01 A3: junta todos los datos inválidos en un solo mensaje
    private void validarDatosRegistro(Cliente c) throws ValidacionException {

        StringBuilder errores = new StringBuilder();

        if (!Validador.esCuitValido(c.getCuitCuil())) {
            errores.append("\n- CUIT/CUIL: formato 30-71234567-8 con dígito verificador válido.");
        }
        if (Validador.esVacio(c.getRazonSocial())) {
            errores.append("\n- Nombre o razón social: es obligatorio.");
        }
        if (!Validador.esEmailValido(c.getEmail())) {
            errores.append("\n- Correo electrónico: formato inválido.");
        }
        if (c.getClave() == null || c.getClave().length() < 6) {
            errores.append("\n- Contraseña: al menos 6 caracteres.");
        }
        if (Validador.esVacio(c.getLocalidad())) {
            errores.append("\n- Localidad: es obligatoria.");
        }

        if (errores.length() > 0) {
            throw new ValidacionException("Revisá los datos marcados:" + errores);
        }
    }

    private boolean existe(Connection con, String sql, String valor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int claveGenerada(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
