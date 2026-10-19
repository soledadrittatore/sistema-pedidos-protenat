USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Workbench bloquea por defecto los UPDATE/DELETE sin clave en el WHERE.
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- Pedido de prueba que se borra en B2 (se registra igual que en A1).
INSERT INTO pedido (id_cliente, id_condicion, fecha, total)
SELECT 2, id_condicion, NOW(), 0 FROM condicion_comercial WHERE activo = 1;
SET @id_nuevo = LAST_INSERT_ID();
INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
SELECT @id_nuevo, id_producto, 2, precio, 2 * precio FROM producto WHERE codigo = 'COL-006';
INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
SELECT @id_nuevo, id_producto, 1, precio, 1 * precio FROM producto WHERE codigo = 'COL-007';


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


ROLLBACK;
SET SQL_SAFE_UPDATES = 1;
