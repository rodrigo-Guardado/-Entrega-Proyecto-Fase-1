# Documento técnico — Fase 2 POO901 (aporte Integrante 1)

## Propósito

Este documento complementa el proyecto Maven de Fase 1 con el análisis necesario para incorporar depósitos, retiros, transferencias, consulta e historial de movimientos, reportes y bitácora. Se conserva el CRUD existente de clientes y la gestión de cuentas. La clase inicial sigue siendo `principal.Main` y la ventana principal `vista.VentanaPrincipal`.

## Casos de uso

### CU-F2-01 — Realizar depósito
**Actor:** Operador. **Precondiciones:** cuenta registrada y activa. **Flujo:** ingresa número de cuenta y monto; el sistema valida; suma el monto al saldo; registra el movimiento; informa el resultado. **Alternos:** cuenta inexistente, cuenta inactiva/bloqueada o monto no positivo; no se modifica el saldo y se informa el motivo.

### CU-F2-02 — Realizar retiro
**Actor:** Operador. **Precondiciones:** cuenta registrada y activa. **Flujo:** ingresa cuenta y monto; se valida que sea positivo y que el saldo alcance; se descuenta el monto; se registra movimiento; se informa resultado. **Alternos:** cuenta inexistente/no activa, monto inválido o saldo insuficiente; no hay cambio de saldo.

### CU-F2-03 — Realizar transferencia
**Actor:** Operador. **Precondiciones:** existen las cuentas origen y destino; ambas están activas. **Flujo:** ingresa origen, destino y monto; el sistema valida estados, monto y saldo; aplica el descuento y el abono; registra el movimiento; confirma el resultado. **Alterno técnico:** si falla una actualización, JDBC revierte toda la transferencia.

### CU-F2-04 — Consultar movimiento
**Actor:** Operador. Ingresa el identificador del movimiento; el sistema presenta sus datos o informa que no existe.

### CU-F2-05 — Consultar historial
**Actor:** Operador. Solicita el historial general o el de una cuenta; el sistema presenta los movimientos que correspondan.

### CU-F2-06 — Consultar reportes
**Actor:** Operador. Puede consultar cuentas activas, cuentas bloqueadas, clientes registrados, historial, movimientos por cuenta y los totales depositado, retirado y transferido.

## Diseño propuesto para coordinar

Para que el equipo pueda comenzar, se propone la siguiente interfaz conceptual:

- `modelo.Transaccion`: `long idTransaccion`, `TipoTransaccion tipoTransaccion`, `BigDecimal monto`, `LocalDateTime fechaHora`, `String numeroCuentaOrigen`, `String numeroCuentaDestino`, `String descripcion`.
- `modelo.TipoTransaccion`: `DEPOSITO`, `RETIRO`, `TRANSFERENCIA`.
- Paquete `excepciones`: `CuentaNoEncontradaException`, `CuentaInactivaException`, `SaldoInsuficienteException`, `MontoInvalidoException`.
- `validaciones.TransaccionService`: métodos `depositar`, `retirar` y `transferir`; valida las reglas y coordina con DAO.
- `dao.TransaccionDAO`: persiste operación, actualiza saldos y consulta movimientos; transferencia atómica con commit/rollback.
- `dao.ReporteDAO`: consultas de reportes.

Los nombres de columnas SQL de transacción aún los define Integrante 2. Este modelo es una propuesta para comenzar y debe mantenerse con `BigDecimal` para montos y `LocalDateTime` para fecha/hora. En depósitos se usa cuenta destino, en retiros cuenta origen y en transferencias ambas. Integrante 3 debe confirmar los constructores, firmas y tipos de excepciones antes de implementarlos.

## Reglas de negocio

Depósito: cuenta existente y activa; monto > 0; se incrementa el saldo y queda movimiento. Retiro: cuenta existente y activa; monto > 0; saldo suficiente; disminuye el saldo y queda movimiento. Transferencia: ambas cuentas existentes y activas; monto > 0; origen con saldo suficiente; se descuenta de origen y se abona a destino. Las reglas exactas son también descritas en `Reglas_y_Diseno_Fase2.md`.

No se han agregado reglas para comisiones, límites, una transferencia a la misma cuenta ni redondeos especiales porque no están en la consigna compartida.

## Configuración de Log4J

El archivo `src/main/resources/log4j2.xml` crea consola y archivo rotativo `logs/sistema_bancario.log`, con nivel raíz INFO. En el código de inicio se registra el arranque, una advertencia de interfaz y un error no controlado. `principal.DemoLog4j` emite ejemplos INFO, WARN y ERROR para que el equipo los ejecute desde el IDE y compruebe que la configuración está leyendo el recurso.

El `pom.xml` ahora declara `log4j-api` y `log4j-core` administrados por el BOM de Log4J 2.26.1. La documentación oficial de Apache indica el BOM para mantener sincronizados los módulos y Core como implementación de logging.

## Matriz de pruebas

`Casos_de_Prueba_Fase2.csv` contiene escenarios positivos, reglas rechazadas, consulta, historial, reportes y rollback. Son casos diseñados; el equipo debe ejecutarlos contra la base de datos cuando existan el SQL de transacciones y los DAO.

## Alineación con Fase 1

La copia mantiene los paquetes y clases existentes (`modelo.Cliente`, `modelo.Cuenta`, `dao.ClienteDAO`, `dao.CuentaDAO`, `conexion.ConexionBD`, vistas y `principal.Main`). No cambia el modelo SQL entregado; la tabla `transacciones` se añadirá cuando Integrante 2 cierre su diseño. Se debe integrar esta copia como base compartida con los demás aportes, cuidando no reemplazar cambios posteriores de otros integrantes.
