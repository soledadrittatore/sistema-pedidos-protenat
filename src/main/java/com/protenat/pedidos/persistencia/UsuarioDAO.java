package com.protenat.pedidos.persistencia;

import com.protenat.pedidos.dominio.Cliente;
import com.protenat.pedidos.dominio.Usuario;
import com.protenat.pedidos.excepciones.BaseDatosException;

/* Operaciones de persistencia de usuarios (CU-01 y CU-02). Las clases de control dependen de esta interfaz y no de la implementación MySQL. */
public interface UsuarioDAO {

    /** Devuelve un Cliente o un PersonalAdministrativo, o null si el correo no existe. */
    Usuario buscarPorEmail(String email) throws BaseDatosException;

    boolean existeEmail(String email) throws BaseDatosException;

    boolean existeCuitCuil(String cuitCuil) throws BaseDatosException;

    /** Registra el usuario y el cliente en una misma transacción y asigna sus claves. */
    void registrarCliente(Cliente cliente) throws BaseDatosException;
}
