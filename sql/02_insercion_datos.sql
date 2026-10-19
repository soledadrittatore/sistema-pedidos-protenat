USE bd_protenat;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2) INSERCIÓN DE DATOS DE PRUEBA
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


