-- Sistema de Gestión de Pedidos PROTENAT
-- Ejecución de los casos de prueba CP01 a CP05 sobre bd_protenat

-- Usa tablas temporales el script prepara sus propias precondiciones dentro de una transacción
-- y al final hace ROLLBACK, así que la base queda igual que antes.
-- Cada bloque muestra su resultadoy almacena la información en la tabla "resultados".

USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SQL_SAFE_UPDATES = 0;

-- Procedimiento usado en CP04 (caja blanca). Reproduce las dos decisiones de ControlPedido al generar un pedido:
--   D1: ¿todas las cantidades están entre 1 y la cantidad máxima?
--   D2: ¿el total alcanza el importe mínimo vigente?

DROP PROCEDURE IF EXISTS prueba_generar_pedido;
DELIMITER //
CREATE PROCEDURE prueba_generar_pedido(IN p_id_cliente INT, OUT p_camino VARCHAR(5), OUT p_mensaje VARCHAR(200))
proc: BEGIN
    DECLARE v_total DECIMAL(12,2);
    DECLARE v_minimo DECIMAL(12,2);
    DECLARE v_condicion INT;
    DECLARE v_id_pedido INT;

    -- D1: cantidad máxima por producto (CU-10, RN-03)
    IF EXISTS (SELECT 1 FROM tmp_carrito c
               JOIN producto p ON p.codigo = c.codigo
               WHERE c.cantidad < 1 OR c.cantidad > p.cantidad_maxima) THEN
        SET p_camino = 'C2';
        SELECT CONCAT('Cantidad máxima superada: ', p.codigo, ' permite hasta ', p.cantidad_maxima, ' unidades')
          INTO p_mensaje
          FROM tmp_carrito c JOIN producto p ON p.codigo = c.codigo
         WHERE c.cantidad < 1 OR c.cantidad > p.cantidad_maxima
         LIMIT 1;
        LEAVE proc;
    END IF;

    -- D2: importe mínimo vigente (CU-10, RN-04)
    SELECT SUM(c.cantidad * p.precio) INTO v_total
      FROM tmp_carrito c JOIN producto p ON p.codigo = c.codigo;
    SELECT id_condicion, importe_minimo_compra INTO v_condicion, v_minimo
      FROM condicion_comercial WHERE activo = TRUE;

    IF v_total < v_minimo THEN
        SET p_camino = 'C3';
        SET p_mensaje = CONCAT('Importe mínimo no alcanzado: total $', v_total, ', mínimo $', v_minimo);
        LEAVE proc;
    END IF;

    -- Ambas condiciones se cumplen: se registra el pedido (CU-04)
    INSERT INTO pedido (id_cliente, id_condicion, fecha, total) VALUES (p_id_cliente, v_condicion, NOW(), v_total);
    SET v_id_pedido = LAST_INSERT_ID();
    INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
    SELECT v_id_pedido, p.id_producto, c.cantidad, p.precio, c.cantidad * p.precio
      FROM tmp_carrito c JOIN producto p ON p.codigo = c.codigo;

    SET p_camino = 'C1';
    SELECT CONCAT('Pedido ', id_pedido, ' registrado en estado ', estado, ' con total $', total)
      INTO p_mensaje FROM pedido WHERE id_pedido = v_id_pedido;
END //
DELIMITER ;

-- Tabla donde se registra cada escenario ejecutado
DROP TEMPORARY TABLE IF EXISTS resultados;
CREATE TEMPORARY TABLE resultados (
    nro        INT AUTO_INCREMENT PRIMARY KEY,
    caso       VARCHAR(5),
    escenario  VARCHAR(80),
    esperado   VARCHAR(120),
    obtenido   VARCHAR(160),
    estado     VARCHAR(10)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TEMPORARY TABLE IF EXISTS tmp_carrito;
CREATE TEMPORARY TABLE tmp_carrito (codigo VARCHAR(30), cantidad INT) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

START TRANSACTION;


-- Precondiciones comunes
-- Condición comercial vigente: importe mínimo $80.000
UPDATE condicion_comercial SET activo = FALSE;
UPDATE condicion_comercial SET activo = TRUE WHERE importe_minimo_compra = 80000.00 ORDER BY id_condicion LIMIT 1;
-- Usuario inactivo para CP01 (CE4): la base no tiene ninguno, se crea uno de prueba
INSERT INTO usuario (email, clave, activo, tipo_usuario)
VALUES ('usuario.inactivo@correo.com', 'clave123', 0, 'CLIENTE');

SELECT 'Precondición' AS paso, id_condicion, importe_minimo_compra FROM condicion_comercial WHERE activo = TRUE;
SELECT 'Precondición' AS paso, codigo, nombre, presentacion, precio, cantidad_maxima, activo
FROM producto WHERE codigo IN ('COL-001','COL-002','COL-005','COL-006','COL-007');


-- CP01 – Iniciar sesión (caja negra, partición de equivalencia)
-- Misma lógica que ControlLogin: buscar usuario, validar clave y estado.
-- (En los datos de prueba las claves están cargadas sin cifrar.)
DROP TEMPORARY TABLE IF EXISTS cp01_entradas;
CREATE TEMPORARY TABLE cp01_entradas (clase VARCHAR(5), email VARCHAR(150), clave VARCHAR(50), esperado VARCHAR(120)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO cp01_entradas VALUES
('CE1', 'estilonorte@correo.com',      'clave123', 'Acceso permitido: CLIENTE'),
('CE1', 'admin@protenat.com.ar',       'admin123', 'Acceso permitido: ADMINISTRATIVO'),
('CE2', 'estilonorte@correo.com',      'clave999', 'Datos de acceso no válidos'),
('CE3', 'noexiste@correo.com',         'clave123', 'Datos de acceso no válidos'),
('CE4', 'usuario.inactivo@correo.com', 'clave123', 'Acceso no habilitado');

DROP TEMPORARY TABLE IF EXISTS cp01_salida;
CREATE TEMPORARY TABLE cp01_salida AS
SELECT e.clase, e.email, e.clave, e.esperado,
       CASE
         WHEN u.id_usuario IS NULL                   THEN 'Datos de acceso no válidos'
         WHEN u.clave <> e.clave                     THEN 'Datos de acceso no válidos'
         WHEN u.activo = 0                           THEN 'Acceso no habilitado'
         ELSE CONCAT('Acceso permitido: ', u.tipo_usuario)
       END AS obtenido
FROM cp01_entradas e
LEFT JOIN usuario u ON u.email = e.email;

SELECT 'CP01' AS caso, clase, email, clave, esperado, obtenido,
       IF(esperado = obtenido, 'APROBADO', 'FALLIDO') AS estado
FROM cp01_salida;

-- inserta en la tabla resultados la ejecucion de la preuba
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
SELECT 'CP01', CONCAT(clase, ' ', email, ' / ', clave), esperado, obtenido,
       IF(esperado = obtenido, 'APROBADO', 'FALLIDO')
FROM cp01_salida;


-- CP02 – Cantidad máxima por producto (caja negra, valores límite)
-- Producto COL-002 Shampoo Color 1,1 L, cantidad máxima 5.
DROP TEMPORARY TABLE IF EXISTS cp02_entradas;
CREATE TEMPORARY TABLE cp02_entradas (cantidad INT, esperado VARCHAR(120)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO cp02_entradas VALUES
(0, 'No incorpora: la cantidad debe ser mayor que cero'),
(1, 'Incorpora el producto'),
(4, 'Incorpora el producto'),
(5, 'Incorpora el producto'),
(6, 'No incorpora: cantidad máxima 5');

DROP TEMPORARY TABLE IF EXISTS cp02_salida;
CREATE TEMPORARY TABLE cp02_salida AS
SELECT e.cantidad, e.esperado,
       CASE
         WHEN e.cantidad < 1                  THEN 'No incorpora: la cantidad debe ser mayor que cero'
         WHEN e.cantidad > p.cantidad_maxima  THEN CONCAT('No incorpora: cantidad máxima ', p.cantidad_maxima)
         ELSE 'Incorpora el producto'
       END AS obtenido
FROM cp02_entradas e
CROSS JOIN producto p
WHERE p.codigo = 'COL-002';

SELECT 'CP02' AS caso, cantidad, esperado, obtenido,
       IF(esperado = obtenido, 'APROBADO', 'FALLIDO') AS estado
FROM cp02_salida;

-- inserta en la tabla resultados la ejecucion de la preuba
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
SELECT 'CP02', CONCAT('COL-002, cantidad ', cantidad), esperado, obtenido,
       IF(esperado = obtenido, 'APROBADO', 'FALLIDO')
FROM cp02_salida;


-- CP03 – Importe mínimo de compra (caja negra, valores límite)
-- Totales $79.000 / $80.000 / $81.000 armados con productos reales.
DROP TEMPORARY TABLE IF EXISTS cp03_carritos;
CREATE TEMPORARY TABLE cp03_carritos (carrito VARCHAR(10), codigo VARCHAR(30), cantidad INT) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO cp03_carritos VALUES
('$79.000', 'COL-006', 2), ('$79.000', 'COL-005', 1),
('$80.000', 'COL-002', 2), ('$80.000', 'COL-001', 2),
('$81.000', 'COL-005', 3), ('$81.000', 'COL-007', 2);

DROP TEMPORARY TABLE IF EXISTS cp03_salida;
CREATE TEMPORARY TABLE cp03_salida AS
SELECT c.carrito,
       SUM(c.cantidad * p.precio) AS total,
       cc.importe_minimo_compra   AS minimo,
       IF(c.carrito = '$79.000', 'No permite generar el pedido', 'Permite continuar') AS esperado,
       IF(SUM(c.cantidad * p.precio) >= cc.importe_minimo_compra,
          'Permite continuar', 'No permite generar el pedido') AS obtenido
FROM cp03_carritos c
JOIN producto p ON p.codigo = c.codigo
CROSS JOIN condicion_comercial cc
WHERE cc.activo = TRUE
GROUP BY c.carrito, cc.importe_minimo_compra;

SELECT 'CP03' AS caso, carrito, total, minimo, esperado, obtenido,
       IF(esperado = obtenido, 'APROBADO', 'FALLIDO') AS estado
FROM cp03_salida;

-- inserta en la tabla resultados la ejecucion de la preuba
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
SELECT 'CP03', CONCAT('Total ', carrito, ' (mínimo $', minimo, ')'), esperado, obtenido,
       IF(esperado = obtenido, 'APROBADO', 'FALLIDO')
FROM cp03_salida;


-- CP04 – Validación de condiciones comerciales (caja blanca,
-- cobertura de decisiones). Cada carrito fuerza un camino distinto.
-- Camino C1: D1 verdadera, D2 verdadera -> 2 x COL-006 + 1 x COL-007 = $82.000
DELETE FROM tmp_carrito;
INSERT INTO tmp_carrito VALUES ('COL-006', 2), ('COL-007', 1);
CALL prueba_generar_pedido(2, @camino, @mensaje);
SELECT 'CP04' AS caso, 'Se espera C1' AS escenario, @camino AS camino_obtenido, @mensaje AS mensaje;

-- inserta en la tabla resultados la ejecucion de la preuba
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
VALUES ('CP04', 'C1: 2 x COL-006 + 1 x COL-007 ($82.000)', 'C1: pedido registrado PENDIENTE',
        CONCAT(@camino, ': ', @mensaje), IF(@camino = 'C1', 'APROBADO', 'FALLIDO'));

-- Camino C2: D1 falsa -> 6 x COL-002 (máximo 5)
DELETE FROM tmp_carrito;
INSERT INTO tmp_carrito VALUES ('COL-002', 6);
CALL prueba_generar_pedido(2, @camino, @mensaje);
SELECT 'CP04' AS caso, 'Se espera C2' AS escenario, @camino AS camino_obtenido, @mensaje AS mensaje;
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
VALUES ('CP04', 'C2: 6 x COL-002', 'C2: informa cantidad máxima superada',
        CONCAT(@camino, ': ', @mensaje), IF(@camino = 'C2', 'APROBADO', 'FALLIDO'));

-- Camino C3: D1 verdadera, D2 falsa -> 2 x COL-001 = $24.000
DELETE FROM tmp_carrito;
INSERT INTO tmp_carrito VALUES ('COL-001', 2);
CALL prueba_generar_pedido(2, @camino, @mensaje);
SELECT 'CP04' AS caso, 'Se espera C3' AS escenario, @camino AS camino_obtenido, @mensaje AS mensaje;

-- inserta en la tabla resultados la ejecucion de la preuba
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
VALUES ('CP04', 'C3: 2 x COL-001 ($24.000)', 'C3: informa importe mínimo no alcanzado',
        CONCAT(@camino, ': ', @mensaje), IF(@camino = 'C3', 'APROBADO', 'FALLIDO'));


-- CP05 – Resolver pedido (prueba funcional)
-- Para cada escenario se crea un pedido PENDIENTE con las verificaciones
-- indicadas y se ejecuta la misma sentencia que usa CU-08 (A3 / A4).
DROP TEMPORARY TABLE IF EXISTS cp05_escenarios;
CREATE TEMPORARY TABLE cp05_escenarios (
    nro INT, pago VARCHAR(10), disponibilidad VARCHAR(10), accion VARCHAR(10), esperado VARCHAR(20), id_pedido INT
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO cp05_escenarios (nro, pago, disponibilidad, accion, esperado) VALUES
(1, 'APROBADA',  'APROBADA',  'Confirmar', 'CONFIRMADO'),
(2, 'PENDIENTE', 'APROBADA',  'Confirmar', 'PENDIENTE'),
(3, 'APROBADA',  'PENDIENTE', 'Confirmar', 'PENDIENTE'),
(4, 'RECHAZADA', 'APROBADA',  'Confirmar', 'PENDIENTE'),
(5, 'APROBADA',  'RECHAZADA', 'Confirmar', 'PENDIENTE'),
(6, 'RECHAZADA', 'PENDIENTE', 'Rechazar',  'RECHAZADO');

-- Un pedido de prueba por escenario
INSERT INTO pedido (id_cliente, id_condicion, fecha, total, estado, estado_pago, estado_disponibilidad, observaciones)
SELECT 2, (SELECT id_condicion FROM condicion_comercial WHERE activo = TRUE), NOW(), 92000.00,
       'PENDIENTE', pago, disponibilidad, CONCAT('CP05 escenario ', nro)
FROM cp05_escenarios ORDER BY nro;

UPDATE cp05_escenarios e
JOIN pedido p ON p.observaciones = CONCAT('CP05 escenario ', e.nro)
SET e.id_pedido = p.id_pedido;

-- Confirmar (A3): solo actualiza si ambas verificaciones están APROBADAS
UPDATE pedido p
JOIN cp05_escenarios e ON e.id_pedido = p.id_pedido AND e.accion = 'Confirmar'
SET p.estado = 'CONFIRMADO'
WHERE p.estado = 'PENDIENTE'
  AND p.estado_pago = 'APROBADA'
  AND p.estado_disponibilidad = 'APROBADA';

-- Rechazar (A4): el administrativo rechaza un pedido pendiente
UPDATE pedido p
JOIN cp05_escenarios e ON e.id_pedido = p.id_pedido AND e.accion = 'Rechazar'
SET p.estado = 'RECHAZADO'
WHERE p.estado = 'PENDIENTE';

SELECT 'CP05' AS caso, e.nro, e.pago, e.disponibilidad, e.accion,
       e.esperado, p.estado AS obtenido,
       IF(e.esperado = p.estado, 'APROBADO', 'FALLIDO') AS estado
FROM cp05_escenarios e JOIN pedido p ON p.id_pedido = e.id_pedido
ORDER BY e.nro;

-- inserta en la tabla resultados la ejecucion de la preuba
INSERT INTO resultados (caso, escenario, esperado, obtenido, estado)
SELECT 'CP05', CONCAT('Pago ', e.pago, ' / Disp. ', e.disponibilidad, ' -> ', e.accion),
       e.esperado, p.estado, IF(e.esperado = p.estado, 'APROBADO', 'FALLIDO')
FROM cp05_escenarios e JOIN pedido p ON p.id_pedido = e.id_pedido
ORDER BY e.nro;


-- resumen de los resultados obtenidos
SELECT ROW_NUMBER() OVER (ORDER BY r.nro) AS num, r.caso, escenario, esperado, obtenido, estado
FROM resultados r ORDER BY r.nro;

SELECT caso, COUNT(*) AS escenarios,
       SUM(estado = 'APROBADO') AS aprobados,
       SUM(estado = 'FALLIDO')  AS fallidos
FROM resultados GROUP BY caso ORDER BY caso;

-- Se deshacen todos los cambios de prueba
ROLLBACK;
DROP PROCEDURE IF EXISTS prueba_generar_pedido;
SET SQL_SAFE_UPDATES = 1;
