# Documento técnico — Fase 2 POO901 (aporte Integrante 4)

## 1. Alcance

Este aporte implementa la capa de persistencia de la Fase 2: `TransaccionDAO` registra depósitos, retiros y transferencias con control transaccional JDBC y consulta movimientos; `ReporteDAO` entrega los 8 reportes solicitados; `PruebasPersistenciaFase2` comprueba ambas clases contra MySQL. Se respetan los nombres acordados en el UML del Integrante 1, las columnas del script del Integrante 2 y el contrato que pidió el Integrante 3 para `TransaccionService`.

## 2. Clases entregadas

### `dao.TransaccionDAO`

| Método | Qué hace |
|---|---|
| `registrarDeposito(Transaccion)` | Abona a la cuenta destino e inserta el movimiento en una transacción JDBC. |
| `registrarRetiro(Transaccion)` | Debita la cuenta origen e inserta el movimiento en una transacción JDBC. |
| `registrarTransferencia(Transaccion)` | Debita el origen, abona el destino e inserta el movimiento; todo o nada. |
| `buscarPorId(long)` | Devuelve un movimiento o `null` si no existe. |
| `listarHistorial()` | Todos los movimientos, del más reciente al más antiguo. |
| `listarHistorialPorCuenta(String)` | Movimientos donde la cuenta es origen o destino. |

### `dao.ReporteDAO`

| Reporte | Método | Fuente |
|---|---|---|
| 1. Cuentas activas | `cuentasActivas()` | Consulta propia por `estado_cuenta` |
| 2. Cuentas bloqueadas | `cuentasBloqueadas()` | Consulta propia por `estado_cuenta` |
| 3. Clientes registrados | `clientesRegistrados()` | Reutiliza `ClienteDAO.consultarTodos()` (incluye teléfonos) |
| 4. Historial de transacciones | `historial()` | Reutiliza `TransaccionDAO.listarHistorial()` |
| 5. Transacciones por cuenta | `transaccionesPorCuenta(String)` | Reutiliza `TransaccionDAO.listarHistorialPorCuenta()` |
| 6. Total depositado | `totalDepositado()` | `totalPorTipo(DEPOSITO)` |
| 7. Total retirado | `totalRetirado()` | `totalPorTipo(RETIRO)` |
| 8. Total transferido | `totalTransferido()` | `totalPorTipo(TRANSFERENCIA)` |

`ReporteDAO` reutiliza los DAO existentes cuando ya tienen la consulta, para no duplicar SQL ni la carga de teléfonos.

## 3. Flujo transaccional de un registro

Los tres métodos `registrar...` siguen la misma secuencia dentro de `registrar(Transaccion, TipoTransaccion)`:

1. **Validar los datos sin abrir conexión**: tipo correcto, monto mayor que cero y con máximo dos decimales, cuentas que corresponden al tipo y, en transferencias, cuentas distintas. Si algo falla se lanza `SQLException` y se registra un WARN.
2. **Abrir la conexión y desactivar el autocommit** con `con.setAutoCommit(false)`.
3. **Actualizar saldos**: depósito → abonar destino; retiro → debitar origen; transferencia → debitar origen y luego abonar destino.
4. **Insertar el movimiento** en `transacciones` y leer el `id_transaccion` generado.
5. **Confirmar** con `con.commit()` y asignar el id con `setIdTransaccion`.
6. **Si cualquier paso del 3 al 5 falla**: `con.rollback()` deshace todo lo hecho en esa conexión.
7. **Cerrar la conexión siempre** en el bloque `finally`.

### Sentencias usadas

```sql
UPDATE cuentas SET saldo = saldo - ? WHERE numero_cuenta = ? AND estado_cuenta = 'ACTIVA' AND saldo >= ?;
UPDATE cuentas SET saldo = saldo + ? WHERE numero_cuenta = ? AND estado_cuenta = 'ACTIVA';
INSERT INTO transacciones (tipo_transaccion, monto, fecha_hora, cuenta_origen, cuenta_destino, descripcion)
VALUES (?, ?, ?, ?, ?, ?);
```

Si un `UPDATE` no afecta ninguna fila, la cuenta no existe, no está activa o no tiene saldo suficiente en ese instante; el DAO lanza `SQLException` y hace rollback.

## 4. Decisiones de diseño

**Transacción también en depósitos y retiros.** El enunciado exige commit/rollback en transferencias, pero un depósito o retiro también son dos sentencias (UPDATE + INSERT). Sin transacción, un fallo en el INSERT dejaría el saldo modificado sin movimiento que lo respalde.

**Rollback explícito.** Cuando una sentencia falla, InnoDB revierte solo esa sentencia, no las anteriores de la misma transacción. Y si el abono va a una cuenta inexistente, el UPDATE ni siquiera da error: simplemente afecta cero filas. En ambos casos el débito al origen sigue aplicado dentro de la transacción hasta que se llama `rollback()`. Las tres pruebas CP-19 lo comprueban.

**UPDATE condicionado.** `TransaccionService` valida saldo y estado antes de llamar al DAO, pero entre esa lectura y el UPDATE otra operación podría cambiar el saldo. La condición `AND saldo >= ?` se evalúa dentro de la misma sentencia que descuenta, con la fila bloqueada, así que el saldo nunca queda negativo. La prueba de 10 retiros simultáneos de 30.00 sobre una cuenta de 100.00 confirma que solo 3 se aplican y el saldo queda en 10.00. Además, funciona aunque la versión de MySQL no aplique las restricciones `CHECK` (MySQL anterior a 8.0.16 las ignora).

**Máximo dos decimales.** La columna es `DECIMAL(12,2)`. Se comprobó en MySQL que con 10.555 el saldo baja 10.55 pero el movimiento se guarda como 10.56; para evitar esa diferencia el DAO rechaza montos con más de dos decimales.

**Normalización de cuentas.** Los números de cuenta se recortan y se pasan a mayúsculas, como los genera `CuentaDAO` (`CTA-000001`). MySQL compara sin distinguir mayúsculas, así que sin esta normalización `CTA-000001` y `cta-000001` podrían pasar como cuentas distintas en una transferencia a la misma cuenta.

**Fecha al segundo.** `fecha_hora` es `DATETIME` sin fracciones; el DAO trunca la fecha a segundos y la deja en el objeto, para que lo que devuelve sea exactamente lo que quedó guardado.

**Patrón de recursos.** Las escrituras usan el patrón `try / catch con rollback / finally con cierre`, porque se necesita la conexión en el catch para revertir y un error al cerrar no debe reportar como fallido un movimiento ya confirmado. Las consultas usan `try-with-resources`, que cierra conexión, sentencia y resultados automáticamente.

**PreparedStatement en todo.** Ningún valor se concatena en el SQL: evita inyección SQL y maneja bien `BigDecimal`, fechas y valores nulos.

## 5. Registro en Log4J

| Nivel | Evento | Ejemplo de mensaje |
|---|---|---|
| INFO | Movimiento confirmado | `Commit del movimiento #9 (TRANSFERENCIA): origen=CTA-000001, destino=CTA-000002, monto=300.00` |
| INFO | Consultas y reportes | `Consulta del historial de la cuenta CTA-000001: 3 movimiento(s)` · `Reporte del total de DEPOSITO: 500.00` |
| WARN | Rollback aplicado | `Rollback de la operación TRANSFERENCIA: se deshicieron sus cambios y ningún saldo fue modificado` |
| WARN | Datos inválidos antes de conectar | `Operación DEPOSITO rechazada antes de abrir la conexión: El monto debe ser mayor que cero.` |
| ERROR | Fallo SQL o de conexión al registrar | `No se pudo registrar TRANSFERENCIA (...): Data too long for column 'descripcion' [SQLState 22001, código de error 1406]` |
| ERROR | Fallo SQL en consultas o reportes | Mensaje y traza completa de la excepción |

Al registrar movimientos, el DAO escribe el mensaje con SQLState y código de error sin la traza, porque `TransaccionService` ya registra la traza completa de la misma excepción. En consultas y reportes no hay otra capa intermedia, así que el DAO registra la traza.

## 6. Consultas SQL de los reportes

Pueden ejecutarse en MySQL Workbench para comparar con lo que muestra la aplicación.

Cuentas activas y cuentas bloqueadas:

```sql
SELECT numero_cuenta, dui_cliente, tipo_cuenta, saldo, fecha_apertura, estado_cuenta
FROM cuentas WHERE estado_cuenta = 'ACTIVA' ORDER BY numero_cuenta;

SELECT numero_cuenta, dui_cliente, tipo_cuenta, saldo, fecha_apertura, estado_cuenta
FROM cuentas WHERE estado_cuenta = 'BLOQUEADA' ORDER BY numero_cuenta;
```

Clientes registrados (con sus teléfonos, que `ClienteDAO` carga por cliente):

```sql
SELECT dui_cliente, nombre_completo_cliente, correo_cliente, direccion_cliente, estado_cliente
FROM clientes ORDER BY nombre_completo_cliente;

SELECT telefono_cliente FROM telefonos_clientes WHERE dui_cliente = '01234567-8' ORDER BY id_telefono;
```

Historial general y transacciones por cuenta:

```sql
SELECT id_transaccion, tipo_transaccion, monto, fecha_hora, cuenta_origen, cuenta_destino, descripcion
FROM transacciones ORDER BY fecha_hora DESC, id_transaccion DESC;

SELECT id_transaccion, tipo_transaccion, monto, fecha_hora, cuenta_origen, cuenta_destino, descripcion
FROM transacciones WHERE cuenta_origen = 'CTA-000001' OR cuenta_destino = 'CTA-000001'
ORDER BY fecha_hora DESC, id_transaccion DESC;
```

Totales depositado, retirado y transferido:

```sql
SELECT COALESCE(SUM(monto), 0) AS total FROM transacciones WHERE tipo_transaccion = 'DEPOSITO';
SELECT COALESCE(SUM(monto), 0) AS total FROM transacciones WHERE tipo_transaccion = 'RETIRO';
SELECT COALESCE(SUM(monto), 0) AS total FROM transacciones WHERE tipo_transaccion = 'TRANSFERENCIA';
```

`COALESCE` hace que el total sea `0.00` cuando todavía no hay movimientos de ese tipo, en lugar de `NULL`.

## 7. Pruebas de persistencia

`pruebas.PruebasPersistenciaFase2` crea un cliente de prueba con cinco cuentas (tres activas, una inactiva y una bloqueada), lleva en memoria el saldo y la cantidad de movimientos que debería tener cada cuenta y, después de cada caso, lo compara con lo que hay en MySQL. Así cada rechazo comprueba también que no cambió ningún saldo ni se insertó ningún movimiento.

| Bloque | Casos |
|---|---|
| Registro con commit | Depósito, retiro y transferencia; id generado; `buscarPorId` recupera todos los campos; normalización del número de cuenta. |
| Rollback | CP-19 con destino inexistente, destino bloqueado y fallo del INSERT después de mover ambos saldos; retiro mayor al saldo; depósito a cuenta inactiva; retiro de cuenta bloqueada. |
| Concurrencia | 10 retiros simultáneos de 30.00 sobre 100.00: 3 confirmados, 7 revertidos, saldo final 10.00. |
| Validación de datos | Monto cero, negativo, nulo o con tres decimales; misma cuenta (incluso en minúsculas); tipo equivocado; cuenta faltante; transacción nula. |
| Consultas | CP-20 y CP-21, orden del historial, cuenta sin movimientos, historial general. |
| Servicio + DAO | CP-01 a CP-18 ejecutados con `TransaccionService` del Integrante 3 sobre MySQL real. |
| Reportes | CP-22 a CP-27, incluido que cada total coincida con la suma del historial por tipo. |
| Limpieza | Se borran los datos de prueba y los totales vuelven a sus valores iniciales. |

Resultado esperado: `56 de 56 pruebas correctas`, seguido de los 8 reportes impresos con los datos actuales de la base.

## 8. Preguntas probables en la defensa

**¿Qué pasa si se cae la conexión a mitad de una transferencia?** Como el autocommit está desactivado, nada se confirma hasta `commit()`. Si la conexión se pierde antes, MySQL descarta la transacción y ningún saldo cambia. El DAO intenta el rollback, registra el error y lanza la excepción al servicio.

**¿Por qué no basta con que `TransaccionService` valide el saldo?** Porque entre la validación y el UPDATE puede ejecutarse otra operación sobre la misma cuenta. La condición dentro del UPDATE es la última barrera y no depende de lo que se leyó antes.

**¿Por qué el DAO lanza `SQLException` y no las excepciones personalizadas?** Fue el contrato acordado con el Integrante 3: el servicio lanza las excepciones de negocio y el DAO solo reporta problemas de persistencia. Los rechazos del DAO son una segunda barrera para datos que no deberían llegar.

**¿Por qué `BigDecimal` y no `double`?** `double` no representa exactamente valores como 0.10, y en dinero esos errores se acumulan. `BigDecimal` coincide con `DECIMAL(12,2)` de MySQL.

**¿Cómo se garantiza el cierre de recursos?** En consultas, `try-with-resources` cierra `ResultSet`, `PreparedStatement` y `Connection` aunque haya excepción. En registros, el `finally` cierra la conexión y registra un WARN si el cierre falla.
