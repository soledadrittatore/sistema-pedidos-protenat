USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Workbench bloquea por defecto los UPDATE/DELETE sin clave en el WHERE.
SET SQL_SAFE_UPDATES = 0;


-- 4) ACTUALIZACIONES PROPIAS DE LOS CASOS DE USO

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


ROLLBACK;
SET SQL_SAFE_UPDATES = 1;
