# Sistema de Gestión de Pedidos PROTENAT

Prototipo de aplicación de escritorio para la gestión de pedidos directos de clientes comerciales de PROTENAT.

- Java 17 + Swing (interfaz gráfica)
- MySQL 8 (base `bd_protenat`) con JDBC (MySQL Connector/J)
- Maven (estructura del proyecto y dependencias) y JUnit 5 (pruebas)

## Estructura

```
src/main/java/com/protenat/pedidos
├── Main.java               punto de entrada: abre la ventana de acceso
├── presentacion/           ventanas Swing (InterfazLogin, InterfazPedido, InterfazPedidos, ...)
├── control/                ControlLogin, ControlPedido, ControlCondicionComercial
├── dominio/                Usuario, Cliente, PersonalAdministrativo, Pedido, DetallePedido,
│                           Producto, Linea, CondicionComercial, EstadoPedido, EstadoVerificacion
├── conexion/               ConexionBD (lee config.properties)
├── util/                   Validador, Formato, Ordenamiento, Registro (log)
└── excepciones/            ValidacionException, BaseDatosException
src/main/resources/config.properties   datos de conexión a MySQL
src/test/java/...                       pruebas JUnit de los casos CP02 a CP05
sql/                                    scripts de la base de datos
docs/capturas/                          capturas de la ejecución
```

# Pasos para ejecutarlo

# 1) Base de datos

1. Iniciar MySQL Server 8.
2. En MySQL Workbench, abrir y ejecutar `sql/00_script_completo.sql` (crea `bd_protenat` y carga los datos de prueba).
3. Ejecutar `sql/06_usuario_aplicacion.sql` (crea el usuario `protenat_app` que usa la aplicación).

# 3) JAR ejecutable

```
mvn package
java -jar target/SistemaPedidosPROTENAT.jar
```

## Usuarios de prueba

| Perfil | Correo | Clave |
|---|---|---|
| Cliente | estilonorte@correo.com | clave123 |
| Cliente | bellaimagen@correo.com | clave123 |
| Administrativo | admin@protenat.com.ar | admin123 |

Importe mínimo vigente: $ 80.000. Por ejemplo, 2 × COL-006 + 1 × COL-001 + 1 × COL-007 = $ 94.000 permite generar el pedido.
