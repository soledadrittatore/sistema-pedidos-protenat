# Sistema de Gestión de Pedidos PROTENAT

Prototipo de aplicación de escritorio para la gestión de pedidos directos de clientes comerciales de PROTENAT.
Seminario de Práctica Informática, Universidad Siglo 21. Autora: María Soledad Rittatore.

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
├── persistencia/           patrón DAO: interfaces UsuarioDAO, ProductoDAO, PedidoDAO,
│                           CondicionComercialDAO; clase abstracta DAOMySQL y sus
│                           implementaciones MySQL; FabricaDAO; ConexionBD (lee config.properties);
│                           archivos: ArchivoBorrador (pedido sin enviar) y ExportadorCSV
├── util/                   Validador, Formato, Ordenamiento, Registro (log)
└── excepciones/            ValidacionException, BaseDatosException, ArchivoException
src/main/resources/config.properties   datos de conexión a MySQL
src/test/java/...                       25 pruebas JUnit (CP02 a CP05, DAO en memoria, archivos)
sql/                                    scripts de la base de datos
docs/capturas/                          capturas de la ejecución
```

## Cómo ejecutarlo

### 1. Base de datos

1. Iniciar MySQL Server 8.
2. En MySQL Workbench, abrir y ejecutar `sql/00_script_completo.sql` (crea `bd_protenat` y carga los datos de prueba).
3. Ejecutar `sql/06_usuario_aplicacion.sql` (crea el usuario `protenat_app` que usa la aplicación).

Si preferís usar otro usuario o clave, cambiá `src/main/resources/config.properties`
o copiá ese archivo junto al JAR y editalo ahí.

### 2. Visual Studio Code

1. Instalar el JDK 17 y la extensión **Extension Pack for Java**.
2. `Archivo > Abrir carpeta...` y elegir la carpeta `sistema-pedidos-protenat` (la que tiene `pom.xml`).
3. Esperar a que VS Code termine de importar el proyecto Maven (descarga el driver de MySQL y JUnit).
4. Abrir `src/main/java/com/protenat/pedidos/Main.java` y hacer clic en **Run** sobre el método `main`.

Para correr las pruebas: panel **Testing** (ícono del matraz) > **Run Tests**, o en la terminal `mvn test`.

### 3. JAR ejecutable

```
mvn package
java -jar target/SistemaPedidosPROTENAT.jar
```

## Versión 2.0 (Entrega 4)

- **Patrón DAO.** Las clases de control ya no tienen SQL: usan las interfaces del paquete
  `persistencia`. `DAOMySQL` (abstracta) concentra la conexión, los parámetros, el recorrido
  del ResultSet, las transacciones y la conversión de `SQLException` en `BaseDatosException`.
- **Arreglos y ArrayList.** Los resultados de las consultas se guardan en `ArrayList`; los
  parámetros de las sentencias, las columnas del CSV, el conteo de pedidos por estado
  (`int[]`) y la búsqueda binaria del catálogo usan arreglos.
- **Archivos.** *Guardar borrador* guarda el pedido sin enviar en `borradores/` y se ofrece
  recuperarlo al volver a ingresar. *Exportar a CSV* guarda la bandeja de pedidos.
  Los errores técnicos se registran en `protenat.log`.

## Usuarios de prueba

| Perfil | Correo | Clave |
|---|---|---|
| Cliente | estilonorte@correo.com | clave123 |
| Cliente | bellaimagen@correo.com | clave123 |
| Administrativo | admin@protenat.com.ar | admin123 |

Importe mínimo vigente: $ 80.000. Por ejemplo, 2 × COL-006 + 1 × COL-001 + 1 × COL-007 = $ 94.000 permite generar el pedido.
