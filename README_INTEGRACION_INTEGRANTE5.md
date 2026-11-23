# Integración del Integrante 5 — POO901 Fase 2

Se incorporaron dos pantallas Swing nuevas:
- `GestionTransacciones`: depósitos, retiros, transferencias, consulta individual por ID e historial general/por cuenta.
- `GestionReportes`: los ocho reportes requeridos, con tablas y resumen de totales.

`VentanaPrincipal` conserva las pestañas de clientes y cuentas de Fase 1 y agrega Transacciones y Reportes.

La interfaz llama a `TransaccionService`, `TransaccionDAO` y `ReporteDAO`; no ejecuta SQL directamente.
También valida en la interfaz montos positivos, máximo dos decimales y que las cuentas de origen y destino sean distintas.

## Antes de ejecutar
1. Importar el proyecto Maven en NetBeans, IntelliJ o Eclipse.
2. Revisar `src/main/java/conexion/ConexionBD.java` y colocar el usuario/contraseña correctos de MySQL.
3. Ejecutar el script SQL actualizado de `database/BD_cuentasBancariaPOO.sql` en MySQL.
4. Verificar que MySQL esté iniciado y ejecutar `principal.Main`.

El archivo de Log4J se configura para escribir en `logs/sistema_bancario.log`.
