-- Contenido: 1) Creación de las tablas
--            2) Inserción de datos de prueba
--            3) Consultas asociadas a los casos de uso
--            4) Actualizaciones propias de los casos de uso
--            5) Borrado de datos
-- Las partes 4 y 5 se ejecutan dentro de una transacción que termina en
-- ROLLBACK, para que la base vuelva a quedar con los datos de prueba.
-- Para conservar esos cambios, reemplazar el ROLLBACK final por COMMIT.

CREATE DATABASE IF NOT EXISTS bd_protenat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Workbench bloquea por defecto los UPDATE/DELETE sin clave en el WHERE.
SET SQL_SAFE_UPDATES = 0;


-- 1) Creación de las tablas

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



-- 2) Inserción de datos de prueba
-- Productos y presentaciones tomados del catálogo de PROTENAT; usuarios,
-- clientes, pedidos e importes son datos de prueba.

INSERT INTO usuario (id_usuario, email, clave, activo, tipo_usuario) VALUES
(1, 'estilonorte@correo.com',    'clave123', 1, 'CLIENTE'),
(2, 'bellaimagen@correo.com',    'clave123', 1, 'CLIENTE'),
(3, 'espaciobelleza@correo.com', 'clave123', 1, 'CLIENTE'),
(4, 'admin@protenat.com.ar',     'admin123', 1, 'ADMINISTRATIVO'),
(5, 'ventas@protenat.com.ar',    'admin123', 1, 'ADMINISTRATIVO');

INSERT INTO cliente (id_cliente, id_usuario, cuit_cuil, razon_social, telefono, direccion, localidad) VALUES
(1, 1, '30-71234567-8', 'Peluquería Estilo Norte', '3424551100', 'Av. Aristóbulo del Valle 4200', 'Santa Fe'),
(2, 2, '27-28456789-3', 'Bella Imagen',            '3424552200', 'San Martín 1850',               'Santa Fe'),
(3, 3, '30-71876543-2', 'Espacio Belleza',         '3492455333', 'Belgrano 950',                  'Rafaela');

INSERT INTO personal_administrativo (id_personal, id_usuario, legajo, nombre, apellido, fecha_ingreso) VALUES
(1, 4, 'ADM001', 'Laura',  'Gómez', '2020-03-02'),
(2, 5, 'ADM002', 'Martín', 'López', '2022-08-15');

INSERT INTO linea (id_linea, nombre, descripcion) VALUES
(1, 'Color',            'Protección del color y brillo para cabellos teñidos'),
(2, 'Nutrición',        'Nutrición y reparación para cabellos secos y dañados'),
(3, 'Keratina + Argán', 'Hidratación y control del frizz'),
(4, 'Fuerza',           'Fortalecimiento de cabellos frágiles y debilitados'),
(5, 'Coloración',       'Productos destinados a procesos de coloración'),
(6, 'Plex',             'Productos para reparación capilar'),
(7, 'Styling',          'Productos para peinado, terminación y cuidado'),
(8, 'Bio Q',            'Línea de productos Bio Q'),
(9, 'Salón',            'Productos destinados al uso profesional en salón');

INSERT INTO producto (id_producto, id_linea, codigo, nombre, descripcion, presentacion, precio, cantidad_maxima, activo) VALUES
(1,  1, 'COL-001', 'Shampoo Color',                      'Shampoo Línea Color',                  '280 ml',    12000.00, 10, 1),
(2,  1, 'COL-002', 'Shampoo Color',                      'Shampoo Línea Color',                  '1,1 L',     28000.00,  5, 1),
(3,  1, 'COL-003', 'Acondicionador Color',               'Acondicionador Línea Color',           '280 ml',    12000.00, 10, 1),
(4,  1, 'COL-004', 'Acondicionador Color',               'Acondicionador Línea Color',           '1,1 L',     28000.00,  5, 1),
(5,  1, 'COL-005', 'Máscara Color',                      'Máscara Línea Color',                  '200 g',     15000.00, 10, 1),
(6,  1, 'COL-006', 'Máscara Color',                      'Máscara Línea Color',                  '800 g',     32000.00,  5, 1),
(7,  1, 'COL-007', 'Spray 10 en 1',                      'Tratamiento Spray 10 en 1',            '260 ml',    18000.00, 10, 1),
(8,  2, 'NUT-001', 'Shampoo Nutrición',                  'Shampoo Línea Nutrición',              '280 ml',    12000.00, 10, 1),
(9,  2, 'NUT-002', 'Acondicionador Nutrición',           'Acondicionador Línea Nutrición',       '280 ml',    12000.00, 10, 1),
(10, 2, 'NUT-003', 'Máscara Nutrición',                  'Máscara Línea Nutrición',              '200 g',     15000.00, 10, 1),
(11, 2, 'NUT-004', 'Crema pre-brushing',                 'Crema pre-brushing Línea Nutrición',   '260 ml',    18000.00,  8, 1),
(12, 2, 'NUT-005', 'Ampolla Nutrición Extrema',          'Ampolla Línea Nutrición',              '14,5 ml',    6000.00, 20, 1),
(13, 3, 'KER-001', 'Shampoo Keratina + Argán',           'Shampoo Línea Keratina + Argán',       '280 ml',    13000.00, 10, 1),
(14, 3, 'KER-002', 'Revitalizador Keratina + Argán',     'Revitalizador Línea Keratina + Argán', '280 ml',    14000.00, 10, 1),
(15, 3, 'KER-003', 'Máscara Keratina + Argán',           'Máscara Línea Keratina + Argán',       '200 g',     16000.00, 10, 1),
(16, 3, 'KER-004', 'Ampolla Keratina y Argán',           'Ampolla Línea Keratina + Argán',       '14,5 ml',    6500.00, 20, 1),
(17, 4, 'FUE-001', 'Shampoo Fuerza',                     'Shampoo Línea Fuerza',                 '280 ml',    12500.00, 10, 1),
(18, 4, 'FUE-002', 'Acondicionador Fuerza',              'Acondicionador Línea Fuerza',          '280 ml',    12500.00, 10, 1),
(19, 4, 'FUE-003', 'Tónico Fuerza',                      'Tónico Línea Fuerza',                  '260 ml',    17000.00,  8, 1),
(20, 5, 'CLR-001', 'Tintura Natural Color',              'Tintura Natural Color',                '60 g',       9000.00, 15, 1),
(21, 5, 'CLR-002', 'Tintura Natural Color sin amoníaco', 'Tintura sin amoníaco',                 '60 g',      10500.00, 15, 1),
(22, 5, 'CLR-003', 'Shampoo Reflex',                     'Shampoo Reflex Color',                 '350 ml',    14000.00, 10, 1),
(23, 5, 'CLR-004', 'Polvo Decolorante',                  'Polvo decolorante profesional',        '700 g',     30000.00,  5, 1),
(24, 6, 'PLE-001', 'Kit E-Plex',                         'Kit E-Plex para tratamiento capilar',  '350 g c/u', 45000.00,  5, 1),
(25, 6, 'PLE-002', 'Crema Reparación Instantánea',       'Crema de reparación instantánea',      '350 g',     22000.00,  8, 1);

-- La condición 1 es histórica; la 2 es la vigente (importe mínimo $80.000).
INSERT INTO condicion_comercial (id_condicion, importe_minimo_compra, fecha_desde, activo) VALUES
(1, 60000.00, '2026-01-01', 0),
(2, 80000.00, '2026-09-01', 1);

INSERT INTO pedido (id_pedido, id_cliente, id_condicion, fecha, total, estado, estado_pago, estado_disponibilidad, observaciones) VALUES
(1, 1, 2, '2026-09-20 10:30:00', 86000.00, 'PENDIENTE',  'PENDIENTE', 'PENDIENTE', NULL),
(2, 2, 2, '2026-09-21 11:15:00', 92000.00, 'PENDIENTE',  'APROBADA',  'APROBADA',  'Pago y disponibilidad verificados'),
(3, 1, 2, '2026-09-22 09:45:00', 84000.00, 'CONFIRMADO', 'APROBADA',  'APROBADA',  'Pedido confirmado'),
(4, 3, 2, '2026-09-23 12:20:00', 98000.00, 'RECHAZADO',  'RECHAZADA', 'APROBADA',  'Pedido rechazado por inconveniente con el pago');

INSERT INTO detalle_pedido (id_detalle, id_pedido, id_producto, cantidad, precio_unitario, subtotal) VALUES
(1, 1,  2, 2, 28000.00, 56000.00),
(2, 1,  5, 2, 15000.00, 30000.00),
(3, 2,  6, 2, 32000.00, 64000.00),
(4, 2,  2, 1, 28000.00, 28000.00),
(5, 3,  2, 3, 28000.00, 84000.00),
(6, 4,  6, 2, 32000.00, 64000.00),
(7, 4, 10, 1, 15000.00, 15000.00),
(8, 4, 12, 1,  6000.00,  6000.00),
(9, 4, 13, 1, 13000.00, 13000.00);


-- 3) Consultas asociadas a los casos de uso

-- C1 (CU-02 Iniciar sesión): datos para validar el acceso y el perfil.
SELECT id_usuario, email, activo, tipo_usuario
FROM usuario
WHERE email = 'estilonorte@correo.com' AND clave = 'clave123';

-- C2 (CU-03 Consultar productos): catálogo de productos activos con su línea.
SELECT p.codigo, p.nombre, l.nombre AS linea, p.presentacion, p.precio, p.cantidad_maxima
FROM producto p
INNER JOIN linea l ON l.id_linea = p.id_linea
WHERE p.activo = 1
ORDER BY l.nombre, p.nombre;

-- C3 (CU-09 / CU-10): condición comercial vigente.
SELECT id_condicion, importe_minimo_compra, fecha_desde
FROM condicion_comercial
WHERE activo = 1;

-- C4 (CU-10 Validar condiciones): ¿se pueden pedir 6 unidades de COL-002?
SELECT codigo, nombre, presentacion, cantidad_maxima,
       IF(6 <= cantidad_maxima, 'PERMITIDA', 'SUPERA EL MÁXIMO') AS resultado
FROM producto
WHERE codigo = 'COL-002';

-- C5 (CU-05 Consultar mis pedidos): pedidos del cliente Peluquería Estilo Norte.
SELECT id_pedido, fecha, total, estado
FROM pedido
WHERE id_cliente = 1
ORDER BY fecha DESC;

-- C6 (CU-05 / CU-06): detalle de un pedido.
SELECT d.id_pedido, pr.codigo, pr.nombre, pr.presentacion, d.cantidad, d.precio_unitario, d.subtotal
FROM detalle_pedido d
INNER JOIN producto pr ON pr.id_producto = d.id_producto
WHERE d.id_pedido = 4;

-- C7 (CU-06 Gestionar pedidos recibidos): bandeja filtrada por estado y fechas.
SELECT p.id_pedido, c.razon_social AS cliente, p.fecha, p.total,
       p.estado, p.estado_pago, p.estado_disponibilidad
FROM pedido p
INNER JOIN cliente c ON c.id_cliente = p.id_cliente
WHERE p.estado = 'PENDIENTE'
  AND p.fecha BETWEEN '2026-09-01' AND '2026-09-30 23:59:59'
ORDER BY p.fecha;

-- C8 (CU-08 Resolver pedido): pedidos pendientes listos para confirmar.
SELECT p.id_pedido, c.razon_social AS cliente, p.fecha, p.total,
       p.estado, p.estado_pago, p.estado_disponibilidad
FROM pedido p
INNER JOIN cliente c ON c.id_cliente = p.id_cliente
WHERE p.estado = 'PENDIENTE'
  AND p.estado_pago = 'APROBADA'
  AND p.estado_disponibilidad = 'APROBADA';

-- C9 (control de integridad): el total de cada pedido coincide con sus detalles.
SELECT p.id_pedido, p.total, SUM(d.subtotal) AS suma_detalles,
       IF(p.total = SUM(d.subtotal), 'OK', 'DIFERENCIA') AS control
FROM pedido p
INNER JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
GROUP BY p.id_pedido, p.total;

-- C10 (control de RN-04): cada pedido y el importe mínimo que se le aplicó.
SELECT p.id_pedido, p.total, cc.importe_minimo_compra,
       IF(p.total >= cc.importe_minimo_compra, 'CUMPLE', 'NO CUMPLE') AS rn04
FROM pedido p
INNER JOIN condicion_comercial cc ON cc.id_condicion = p.id_condicion;

-- C11 (resumen): cantidad de pedidos e importe por estado.
SELECT estado, COUNT(*) AS cantidad, SUM(total) AS importe_total
FROM pedido
GROUP BY estado;


-- 4) Actualizaciones propias de los casos de uso
START TRANSACTION;

-- A1 (CU-04 Gestionar pedido): se registran el encabezado y sus detalles
-- juntos, para que no quede un pedido sin productos.
INSERT INTO pedido (id_cliente, id_condicion, fecha, total)
SELECT 2, id_condicion, NOW(), 0 FROM condicion_comercial WHERE activo = 1;
SET @id_nuevo = LAST_INSERT_ID();
INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
SELECT @id_nuevo, id_producto, 2, precio, 2 * precio FROM producto WHERE codigo = 'COL-006';
INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
SELECT @id_nuevo, id_producto, 1, precio, 1 * precio FROM producto WHERE codigo = 'COL-007';
UPDATE pedido
SET total = (SELECT SUM(subtotal) FROM detalle_pedido WHERE id_pedido = @id_nuevo)
WHERE id_pedido = @id_nuevo;
SELECT id_pedido, id_cliente, total, estado FROM pedido WHERE id_pedido = @id_nuevo;

-- A2 (CU-07 Verificar pedido): se registran las verificaciones del pedido 1.
UPDATE pedido
SET estado_pago = 'APROBADA',
    estado_disponibilidad = 'APROBADA',
    observaciones = 'Transferencia verificada. Stock controlado.'
WHERE id_pedido = 1 AND estado = 'PENDIENTE';

-- A3 (CU-08 Resolver pedido): confirmar. El WHERE impide confirmar si
-- alguna verificación no está APROBADA (RN-05, RN-07).
UPDATE pedido
SET estado = 'CONFIRMADO'
WHERE id_pedido = 2
  AND estado = 'PENDIENTE'
  AND estado_pago = 'APROBADA'
  AND estado_disponibilidad = 'APROBADA';

-- A4 (CU-08 Resolver pedido): rechazar un pedido pendiente.
UPDATE pedido
SET estado = 'RECHAZADO', observaciones = 'Rechazado a pedido del cliente'
WHERE id_pedido = @id_nuevo AND estado = 'PENDIENTE';

-- A5 (CU-11 Modificar importe mínimo): se desactiva la condición vigente y
-- se registra una nueva, para que los pedidos anteriores conserven la suya.
UPDATE condicion_comercial SET activo = 0 WHERE activo = 1;
INSERT INTO condicion_comercial (importe_minimo_compra, fecha_desde, activo)
VALUES (85000.00, CURDATE(), 1);

-- A6 (CU-12 Modificar cantidad máxima por producto).
UPDATE producto SET cantidad_maxima = 8 WHERE codigo = 'COL-006';

SELECT id_pedido, total, estado, estado_pago, estado_disponibilidad FROM pedido ORDER BY id_pedido;
SELECT * FROM condicion_comercial ORDER BY id_condicion;


-- 5) Borrado de datos

-- B1 Baja lógica de un producto: tiene pedidos asociados, por eso no se
-- borra; se marca inactivo y deja de aparecer en el catálogo (C2).
UPDATE producto SET activo = 0 WHERE codigo = 'COL-004';

-- B2 Borrado de un pedido de prueba: primero sus detalles y después el
-- encabezado, porque la clave foránea no permite dejar detalles sin pedido.
DELETE FROM detalle_pedido WHERE id_pedido = @id_nuevo;
DELETE FROM pedido WHERE id_pedido = @id_nuevo;
SELECT COUNT(*) AS pedidos_restantes FROM pedido WHERE id_pedido = @id_nuevo;

-- B3 Intento de borrar un cliente con pedidos. La clave foránea lo impide
-- y MySQL responde con el error 1451 (integridad referencial). Se deja
-- comentado para que el script se ejecute completo; ejecutarlo aparte
-- para obtener la evidencia.
-- DELETE FROM cliente WHERE id_cliente = 1;

-- Se deshacen las actualizaciones y borrados de prueba (cambiar por COMMIT
-- para conservarlos).
ROLLBACK;
SET SQL_SAFE_UPDATES = 1;
