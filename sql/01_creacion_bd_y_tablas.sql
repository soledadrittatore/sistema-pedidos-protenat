
CREATE DATABASE IF NOT EXISTS bd_protenat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1) CREACIÓN DE LAS TABLAS

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS detalle_pedido, pedido, producto, linea,
  condicion_comercial, personal_administrativo, cliente, usuario;
SET FOREIGN_KEY_CHECKS = 1;

-- Datos de acceso comunes (superclase Usuario).
CREATE TABLE usuario (
  id_usuario    INT          NOT NULL AUTO_INCREMENT,
  email         VARCHAR(150) NOT NULL,
  clave         VARCHAR(255) NOT NULL,
  activo        TINYINT(1)   NOT NULL DEFAULT 1,
  tipo_usuario  VARCHAR(30)  NOT NULL,          -- CLIENTE o ADMINISTRATIVO
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
  CONSTRAINT uq_usuario_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Cliente comercial (especialización de Usuario, relación 1 a 1).
CREATE TABLE cliente (
  id_cliente    INT          NOT NULL AUTO_INCREMENT,
  id_usuario    INT          NOT NULL,
  cuit_cuil     VARCHAR(13)  NOT NULL,
  razon_social  VARCHAR(150) NOT NULL,
  telefono      VARCHAR(30)  NULL,
  direccion     VARCHAR(200) NULL,
  localidad     VARCHAR(100) NULL,
  CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
  CONSTRAINT uq_cliente_usuario UNIQUE (id_usuario),
  CONSTRAINT uq_cliente_cuit UNIQUE (cuit_cuil),
  CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Personal administrativo (especialización de Usuario, relación 1 a 1).
CREATE TABLE personal_administrativo (
  id_personal    INT          NOT NULL AUTO_INCREMENT,
  id_usuario     INT          NOT NULL,
  legajo         VARCHAR(30)  NOT NULL,
  nombre         VARCHAR(100) NOT NULL,
  apellido       VARCHAR(100) NOT NULL,
  fecha_ingreso  DATE         NULL,
  CONSTRAINT pk_personal PRIMARY KEY (id_personal),
  CONSTRAINT uq_personal_usuario UNIQUE (id_usuario),
  CONSTRAINT uq_personal_legajo UNIQUE (legajo),
  CONSTRAINT fk_personal_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Líneas en las que se agrupan los productos.
CREATE TABLE linea (
  id_linea     INT          NOT NULL AUTO_INCREMENT,
  nombre       VARCHAR(100) NOT NULL,
  descripcion  VARCHAR(255) NULL,
  CONSTRAINT pk_linea PRIMARY KEY (id_linea),
  CONSTRAINT uq_linea_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Productos habilitados para el canal de pedidos directos.
CREATE TABLE producto (
  id_producto      INT           NOT NULL AUTO_INCREMENT,
  id_linea         INT           NOT NULL,
  codigo           VARCHAR(30)   NOT NULL,
  nombre           VARCHAR(150)  NOT NULL,
  descripcion      VARCHAR(255)  NULL,
  presentacion     VARCHAR(100)  NULL,
  precio           DECIMAL(12,2) NOT NULL,
  cantidad_maxima  INT           NOT NULL,       -- RN-03
  activo           TINYINT(1)    NOT NULL DEFAULT 1,
  CONSTRAINT pk_producto PRIMARY KEY (id_producto),
  CONSTRAINT uq_producto_codigo UNIQUE (codigo),
  CONSTRAINT fk_producto_linea FOREIGN KEY (id_linea) REFERENCES linea (id_linea)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Condiciones comerciales; se conserva el historial y solo una está activa.
CREATE TABLE condicion_comercial (
  id_condicion           INT           NOT NULL AUTO_INCREMENT,
  importe_minimo_compra  DECIMAL(12,2) NOT NULL, -- RN-04
  fecha_desde            DATE          NOT NULL,
  activo                 TINYINT(1)    NOT NULL DEFAULT 1,
  CONSTRAINT pk_condicion PRIMARY KEY (id_condicion)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Encabezado del pedido: estado general y estados de verificación.
CREATE TABLE pedido (
  id_pedido              INT           NOT NULL AUTO_INCREMENT,
  id_cliente             INT           NOT NULL,
  id_condicion           INT           NOT NULL, -- condición aplicada al generarlo
  fecha                  DATETIME      NOT NULL,
  total                  DECIMAL(12,2) NOT NULL,
  estado                 ENUM('PENDIENTE','CONFIRMADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
  estado_pago            ENUM('PENDIENTE','APROBADA','RECHAZADA')   NOT NULL DEFAULT 'PENDIENTE',
  estado_disponibilidad  ENUM('PENDIENTE','APROBADA','RECHAZADA')   NOT NULL DEFAULT 'PENDIENTE',
  observaciones          VARCHAR(500)  NULL,
  CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
  CONSTRAINT fk_pedido_condicion FOREIGN KEY (id_condicion) REFERENCES condicion_comercial (id_condicion)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Detalle del pedido: un renglón por producto.
CREATE TABLE detalle_pedido (
  id_detalle       INT           NOT NULL AUTO_INCREMENT,
  id_pedido        INT           NOT NULL,
  id_producto      INT           NOT NULL,
  cantidad         INT           NOT NULL,
  precio_unitario  DECIMAL(12,2) NOT NULL,       -- precio al momento del pedido
  subtotal         DECIMAL(12,2) NOT NULL,
  CONSTRAINT pk_detalle PRIMARY KEY (id_detalle),
  CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
  CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


