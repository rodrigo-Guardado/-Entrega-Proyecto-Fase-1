# Reglas y diseño técnico — Fase 2 POO901

## Alcance

La Fase 2 amplía el sistema de escritorio de la Fase 1 con depósitos, retiros, transferencias, consulta de un movimiento, historial de movimientos, reportes, manejo de excepciones y registro de eventos con Log4J. Se mantienen operativas las funciones de clientes y cuentas de la Fase 1.

## Reglas de negocio

### Depósito

1. La cuenta debe existir.
2. La cuenta debe estar activa.
3. El monto debe ser mayor que cero.
4. Si se cumplen las reglas, el saldo de la cuenta aumenta por el monto depositado.
5. El movimiento debe registrarse en el historial.

### Retiro

1. La cuenta debe existir y estar activa.
2. El monto debe ser mayor que cero.
3. El saldo debe ser suficiente para cubrir el retiro.
4. Si se cumplen las reglas, el saldo disminuye por el monto retirado.
5. El movimiento debe registrarse en el historial.

### Transferencia

1. Las cuentas de origen y destino deben existir y ambas deben estar activas.
2. El monto debe ser mayor que cero.
3. La cuenta de origen debe tener saldo suficiente.
4. Si se cumplen las reglas, se descuenta el monto de origen y se suma a destino.
5. La transferencia se registra como movimiento con sus dos cuentas relacionadas.
6. La persistencia debe guardar ambos cambios como una sola operación: si falla una parte, ninguna de las dos debe quedar aplicada.

### Reportes solicitados

1. Cuentas activas.
2. Cuentas bloqueadas.
3. Clientes registrados.
4. Historial general de transacciones.
5. Transacciones por cuenta.
6. Total depositado.
7. Total retirado.
8. Total transferido.

## Excepciones y registro

La propuesta de diseño contempla excepciones de dominio para cuenta inexistente, cuenta inactiva, saldo insuficiente y monto inválido. Las clases y nombres concretos deben coincidir con lo que acuerde Integrante 3.

La aplicación registra operaciones exitosas en nivel INFO, rechazos por reglas de negocio en WARN y fallos técnicos o errores capturados en ERROR. Los mensajes deben incluir tipo de operación y datos identificadores necesarios para rastrear el evento, evitando registrar credenciales u otra información sensible.

## Compatibilidad con el proyecto de Fase 1 recibido

El proyecto actual está organizado como Maven en `POO901_Entrega_Final`, usa JDK 17 y conserva los paquetes sin prefijo institucional: `modelo`, `dao`, `conexion`, `validaciones`, `vista` y `principal`. La aplicación se inicia desde `principal.Main`; `vista.VentanaPrincipal` actualmente crea `ClienteDAO` y `CuentaDAO` y abre las pestañas de clientes y cuentas.

Los modelos reales usan `duiCliente`, `numeroCuenta`, `tipoCuenta`, `saldo` como `BigDecimal`, `fechaApertura` como `LocalDate` y `estadoCuenta`. El SQL principal mantiene `dui_cliente` como PK, `numero_cuenta` como PK y `telefonos_clientes.id_telefono` como `AUTO_INCREMENT`. La estructura actual no tiene todavía tabla de transacciones ni los DAO o vistas de Fase 2.

El `pom.xml` recibido solo declara MySQL Connector/J. Para que `log4j2.xml` escriba logs, el proyecto debe incorporar las dependencias de Log4J 2 compatibles con la versión que acuerde el equipo. Copien la configuración a `src/main/resources/log4j2.xml`; el archivo de log se resolverá bajo la carpeta `logs` relativa al directorio desde donde se ejecuta Maven/aplicación. Este entregable no cambia los archivos originales de Fase 1.
## Diseño UML actualizado

El archivo `diagrama_clases_fase2.puml` presenta las responsabilidades y relaciones principales: entidades Cliente, Cuenta y Transaccion; servicio para reglas de negocio; excepciones; DAOs transaccional y de reportes; y pantallas. Los métodos se muestran de forma conceptual para orientar el acuerdo de interfaces entre integrantes.

## Supuestos y puntos para coordinar

- La distribución asigna a Integrante 2 el esquema y columnas de `transacciones`; por ello, los atributos del movimiento en el diagrama son una propuesta conceptual. Deben ajustarse al SQL final.
- En Fase 1, el esquema actualizado que compartió el equipo usa `dui_cliente` como identificador y teléfonos en `telefonos_clientes`. Algunas partes del documento inicial todavía usan `codigo_cliente` y un teléfono dentro de clientes; para esta fase se debe preservar el SQL actualizado y alinear la documentación general con él.
- El requisito dice que una cuenta debe estar activa; se entiende que `BLOQUEADA` e `INACTIVA` no permiten operaciones. Confirmar que el equipo interprete así los estados.
- No se añade una regla para impedir transferencias a la misma cuenta, ni límites máximos, comisiones o reglas de redondeo porque no aparecen en los requisitos compartidos.
- Definir entre Integrantes 2, 3 y 4 los nombres exactos de tipos de movimiento, columnas, excepciones y métodos antes de integrar.
- La configuración entregada es para Log4J 2 (`log4j2.xml`). El equipo debe incluir la dependencia correspondiente. Si se decidió usar Log4J 1.x, la configuración y API deben cambiarse en conjunto.

## Configuración Log4J

`resources/log4j2.xml` configura la salida de consola y un archivo rotativo `logs/sistema_bancario.log`, con registro INFO y superiores. La rotación es diaria o al superar el tamaño configurado, y conserva archivos comprimidos históricos. Si la carpeta de ejecución es distinta, se debe ajustar la ruta del archivo.

## Propuesta de nombres para que el equipo pueda avanzar

La Fase 1 ya usa `BigDecimal` para dinero y los números de cuenta son `String` (`VARCHAR(25)`). Para la nueva entidad propongo `idTransaccion: long`, `tipoTransaccion: TipoTransaccion`, `monto: BigDecimal`, `fechaHora: LocalDateTime`, `numeroCuentaOrigen: String`, `numeroCuentaDestino: String` y `descripcion: String`. Los tipos serían `DEPOSITO`, `RETIRO` y `TRANSFERENCIA`. Para depósitos se ocupa el campo de destino, para retiros el de origen, y para transferencia ambos. Integrante 2 debe traducir estos nombres a las columnas SQL finales.

Propongo las excepciones `CuentaNoEncontradaException`, `CuentaInactivaException`, `SaldoInsuficienteException` y `MontoInvalidoException`. Integrante 3 puede seguir esos nombres al crear las clases para que DAO, servicio y pantallas se comuniquen sin esperar más decisiones. La transferencia debe quedar en una transacción JDBC con rollback si falla una de las dos actualizaciones.