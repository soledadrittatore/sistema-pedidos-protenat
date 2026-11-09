package com.protenat.pedidos.excepciones;

/* Se lanza cuando no se cumple una regla de negocio o un dato ingresado no es válido */
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
