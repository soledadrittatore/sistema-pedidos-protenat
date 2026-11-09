package com.protenat.pedidos.excepciones;

/*  Se lanza cuando falla el acceso a la base de datos. Conserva la causa
    técnica (SQLException) para registrarla en el log */
public class BaseDatosException extends Exception {

    public static final String MENSAJE_USUARIO =
            "No se pudo completar la operación. Intentá nuevamente.";

    public BaseDatosException(String detalleTecnico, Throwable causa) {
        super(detalleTecnico, causa);
    }

    public String getMensajeUsuario() {
        return MENSAJE_USUARIO;
    }
}
