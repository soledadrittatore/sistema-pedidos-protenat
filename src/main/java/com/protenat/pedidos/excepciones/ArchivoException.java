package com.protenat.pedidos.excepciones;

/* Se lanza cuando no se puede leer o escribir un archivo (borrador del pedido
   o exportación CSV). Envuelve la IOException original para el log. */
public class ArchivoException extends Exception {

    public ArchivoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
