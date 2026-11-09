package com.protenat.pedidos.control;

import com.protenat.pedidos.excepciones.BaseDatosException;
import com.protenat.pedidos.excepciones.ValidacionException;
import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.Usuario;
import com.protenat.pedidos.persistencia.UsuarioDAO;
import com.protenat.pedidos.util.Validador;

/**
 * Coordina CU-02 Iniciar sesión y CU-01 Registrarse.
 */
public class ControlLogin {

    public static final String MSG_CREDENCIALES = "El correo o la contraseña no son correctos.";
    public static final String MSG_INACTIVO = "Tu usuario no está habilitado. Comunicate con PROTENAT.";

    private final UsuarioDAO usuarioDAO;

    public ControlLogin(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    // Valida las credenciales ingresadas contra el usuario encontrado
    public boolean validarCredenciales(Usuario usuario, String email, String clave) {

        if (usuario == null) {
            return false;
        }

        if (!usuario.getEmail().equalsIgnoreCase(email)) {
            return false;
        }

        return usuario.validarClave(clave);
    }

    /**
     * CU-02. Devuelve un Cliente o un PersonalAdministrativo; la ventana lo
     * recibe como Usuario y abre la pantalla de su perfil (polimorfismo).
     */
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

    // Busca el usuario por correo; el DAO crea el objeto de la subclase que corresponde
    public Usuario consultarUsuario(String email) throws BaseDatosException {
        return usuarioDAO.buscarPorEmail(email);
    }

    /**
     * CU-01. Valida los datos, controla que el CUIT/CUIL y el correo no estén
     * registrados y registra usuario y cliente (el DAO usa una transacción).
     */
    public void registrarCliente(Cliente cliente)
            throws ValidacionException, BaseDatosException {

        validarDatosRegistro(cliente);

        if (usuarioDAO.existeCuitCuil(cliente.getCuitCuil())) {
            throw new ValidacionException("Ya existe un cliente registrado con ese CUIT/CUIL.");
        }
        if (usuarioDAO.existeEmail(cliente.getEmail())) {
            throw new ValidacionException("El correo ya está asociado a otro usuario. Ingresá otro.");
        }

        usuarioDAO.registrarCliente(cliente);
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
}
