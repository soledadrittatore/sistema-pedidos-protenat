package com.protenat.pedidos.excepciones;

/* Se lanza cuando no se cumple una regla de negocio o un dato ingresado no
   es válido. Su mensaje está pensado para mostrarse directamente al usuario
 * (RNF-06). */

public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
