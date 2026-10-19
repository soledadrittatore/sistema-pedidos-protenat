USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3) CONSULTAS

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


