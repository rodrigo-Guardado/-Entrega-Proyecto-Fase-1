# Sistema de Gestión Bancaria Académica — POO901 Fase 1

## Contenido
- `modelo`: Cliente, Cuenta y enumeraciones (Integrante 3).
- `validaciones`: validaciones de DUI, teléfono, correo y saldo (Integrante 3).
- `conexion`: conexión JDBC a MySQL (Integrante 4).
- `dao`: ClienteDAO y CuentaDAO (Integrante 4).
- `vista`: VentanaPrincipal, GestionClientes, GestionCuentas y Estilos (Integrante 5).
- `principal.Main`: punto de entrada de la aplicación.
- `pruebas`: pruebas de persistencia entregadas por el Integrante 4.
- `database`: scripts SQL recibidos.

## Cómo abrir
1. Extraer el ZIP.
2. Abrir la carpeta raíz `POO901_Entrega_Final` en Visual Studio Code.
3. Tener JDK 17 o superior y la extensión Extension Pack for Java.
4. Abrir MySQL Workbench/phpMyAdmin y ejecutar el script SQL compatible de la carpeta `database`.
5. Abrir `src/main/java/conexion/ConexionBD.java` y colocar la contraseña local de MySQL en `CLAVE`.
6. Confirmar que el esquema se llame `sistema_bancario`, que MySQL esté en `localhost:3306` y que el usuario sea `root`, o ajustar esos valores.
7. Esperar que Maven descargue las dependencias desde `pom.xml`.
8. Ejecutar `principal.Main`.

## Funciones de la interfaz
### Clientes
- Registrar y listar clientes.
- Buscar por DUI o nombre.
- Actualizar datos.
- Cambiar estado.
- Manejar varios teléfonos separados por coma.

### Cuentas
- Generar número de cuenta.
- Registrar cuentas AHORRO o CORRIENTE.
- Validar que el cliente exista y esté ACTIVO.
- Validar saldo no negativo.
- Consultar cuentas y buscar por número o DUI.

## Aclaraciones de integración
- El esquema y el modelo real usan DUI como llave primaria del cliente; el código de cliente del primer borrador UML no aparece en el SQL ni en las clases Java entregadas.
- Los teléfonos se almacenan en `telefonos_clientes`.
- El saldo se maneja con `BigDecimal`.
- La interfaz no ejecuta SQL: invoca los DAO.
- Los datos de demostración incluidos en el script se conservan.
- Antes de entregar como funcional, se debe probar la conexión local y las operaciones desde la interfaz.

## Scripts SQL
`BD_cuentasBancariasPOO.sql` es el script incluido en el ZIP de Parte 4 y debe ser la referencia principal para la estructura usada por sus DAO.
`BD_cuentasBancariasPOO_entregado.sql` conserva además el archivo SQL individual enviado en el chat para comparación.
