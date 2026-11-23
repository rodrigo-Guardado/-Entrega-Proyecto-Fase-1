package pruebas;

import conexion.ConexionBD;
import dao.ClienteDAO;
import dao.CuentaDAO;
import dao.ReporteDAO;
import dao.TransaccionDAO;
import excepciones.CuentaInactivaException;
import excepciones.CuentaNoEncontradaException;
import excepciones.MontoInvalidoException;
import excepciones.SaldoInsuficienteException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import modelo.Cliente;
import modelo.Cuenta;
import modelo.EstadoCliente;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import validaciones.TransaccionService;

public class PruebasPersistenciaFase2 {

    private static final String DUI_PRUEBA = "88888888-8";
    private static final String TELEFONO_PRUEBA = "77778888";
    private static final String CUENTA_A = "PRB-F2-ACTIVA-A";
    private static final String CUENTA_B = "PRB-F2-ACTIVA-B";
    private static final String CUENTA_INACTIVA = "PRB-F2-INACTIVA";
    private static final String CUENTA_BLOQUEADA = "PRB-F2-BLOQUEADA";
    private static final String CUENTA_CONCURRENCIA = "PRB-F2-CONCURRENCIA";
    private static final String CUENTA_INEXISTENTE = "PRB-F2-NO-EXISTE";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ClienteDAO clienteDAO = new ClienteDAO();
    private static final CuentaDAO cuentaDAO = new CuentaDAO();
    private static final TransaccionDAO transaccionDAO = new TransaccionDAO();
    private static final ReporteDAO reporteDAO = new ReporteDAO();
    private static final TransaccionService servicio = new TransaccionService(cuentaDAO, transaccionDAO);

    private static final Map<String, BigDecimal> saldosEsperados = new LinkedHashMap<>();
    private static final Map<String, Integer> movimientosEsperados = new LinkedHashMap<>();
    private static final Map<TipoTransaccion, BigDecimal> totalesIniciales = new EnumMap<>(TipoTransaccion.class);
    private static final Map<TipoTransaccion, BigDecimal> montosRegistrados = new EnumMap<>(TipoTransaccion.class);
    private static final List<Long> idsRegistrados = new ArrayList<>();
    private static int correctas;
    private static int total;

    public static void main(String[] args) {
        System.out.println("PRUEBAS DE PERSISTENCIA FASE 2: TransaccionDAO y ReporteDAO contra MySQL");
        try {
            limpiarDatosDePrueba();
            prepararDatosDePrueba();
            probarRegistroConCommit();
            probarRollback();
            probarRetirosSimultaneos();
            probarValidacionDeDatos();
            probarConsultas();
            probarServicioConMySQL();
            probarReportes();
        } catch (Exception e) {
            System.out.println("\nLas pruebas se detuvieron por un error inesperado: " + e);
        } finally {
            limpiarAlTerminar();
        }
        mostrarReportes();
        System.out.println("\nResultado: " + correctas + " de " + total + " pruebas correctas.");
        System.out.println("Bitácora generada en logs/sistema_bancario.log");
    }

    private static void prepararDatosDePrueba() throws SQLException {
        System.out.println("\nPreparación");
        for (TipoTransaccion tipo : TipoTransaccion.values()) {
            totalesIniciales.put(tipo, reporteDAO.totalPorTipo(tipo));
            montosRegistrados.put(tipo, BigDecimal.ZERO);
        }
        Cliente cliente = new Cliente(DUI_PRUEBA, "Cliente Prueba Fase 2", "fase2@prueba.com", "San Salvador",
                EstadoCliente.ACTIVO, List.of(TELEFONO_PRUEBA));
        boolean creados = clienteDAO.insertar(cliente)
                && crearCuenta(CUENTA_A, "1000.00", EstadoCuenta.ACTIVA)
                && crearCuenta(CUENTA_B, "200.00", EstadoCuenta.ACTIVA)
                && crearCuenta(CUENTA_INACTIVA, "300.00", EstadoCuenta.INACTIVA)
                && crearCuenta(CUENTA_BLOQUEADA, "400.00", EstadoCuenta.BLOQUEADA)
                && crearCuenta(CUENTA_CONCURRENCIA, "100.00", EstadoCuenta.ACTIVA);
        verificar("Cliente " + DUI_PRUEBA + " con 5 cuentas de prueba: 3 ACTIVA, 1 INACTIVA y 1 BLOQUEADA",
                creados && estadoEsperado());
    }

    private static void probarRegistroConCommit() throws SQLException {
        System.out.println("\nTransaccionDAO: registro con commit");
        Transaccion deposito = deposito(CUENTA_A, "250.00", "Depósito de prueba DAO");
        transaccionDAO.registrarDeposito(deposito);
        anotar(deposito);
        verificar("registrarDeposito asigna el id #" + deposito.getIdTransaccion() + " y deja " + CUENTA_A
                + " en " + saldosEsperados.get(CUENTA_A), deposito.getIdTransaccion() > 0 && estadoEsperado());

        Transaccion guardado = transaccionDAO.buscarPorId(deposito.getIdTransaccion());
        verificar("buscarPorId recupera tipo, monto, fecha, cuentas y descripción del depósito", guardado != null
                && guardado.getTipoTransaccion() == TipoTransaccion.DEPOSITO
                && guardado.getMonto().compareTo(deposito.getMonto()) == 0
                && guardado.getFechaHora().equals(deposito.getFechaHora())
                && guardado.getNumeroCuentaOrigen() == null
                && CUENTA_A.equals(guardado.getNumeroCuentaDestino())
                && deposito.getDescripcion().equals(guardado.getDescripcion()));

        Transaccion retiro = retiro(CUENTA_A, "100.50");
        transaccionDAO.registrarRetiro(retiro);
        anotar(retiro);
        verificar("registrarRetiro deja " + CUENTA_A + " en " + saldosEsperados.get(CUENTA_A),
                retiro.getIdTransaccion() > deposito.getIdTransaccion() && estadoEsperado());

        Transaccion transferencia = transferencia(CUENTA_A, CUENTA_B, "300.00", "Transferencia de prueba DAO");
        transaccionDAO.registrarTransferencia(transferencia);
        anotar(transferencia);
        verificar("registrarTransferencia descuenta de " + CUENTA_A + " (" + saldosEsperados.get(CUENTA_A)
                + ") y abona a " + CUENTA_B + " (" + saldosEsperados.get(CUENTA_B) + ") en un solo commit",
                transferencia.getIdTransaccion() > retiro.getIdTransaccion() && estadoEsperado());

        Transaccion escritaDistinto = deposito("  " + CUENTA_B.toLowerCase() + "  ", "25.00", null);
        transaccionDAO.registrarDeposito(escritaDistinto);
        anotar(escritaDistinto);
        verificar("registrarDeposito normaliza el número de cuenta (espacios y minúsculas) y acepta descripción nula",
                CUENTA_B.equals(escritaDistinto.getNumeroCuentaDestino()) && estadoEsperado());
    }

    private static void probarRollback() throws SQLException {
        System.out.println("\nTransaccionDAO: control transaccional con rollback");
        verificarRechazo("CP-19 Transferencia a una cuenta inexistente: se revierte el débito ya aplicado",
                SQLException.class, () -> transaccionDAO.registrarTransferencia(
                        transferencia(CUENTA_A, CUENTA_INEXISTENTE, "50.00", "Destino inexistente")));
        verificarRechazo("CP-19 Transferencia a una cuenta BLOQUEADA: rollback y ambos saldos intactos",
                SQLException.class, () -> transaccionDAO.registrarTransferencia(
                        transferencia(CUENTA_A, CUENTA_BLOQUEADA, "50.00", "Destino bloqueado")));
        verificarRechazo("CP-19 Falla el INSERT después de mover los dos saldos: rollback de ambos UPDATE",
                SQLException.class, () -> transaccionDAO.registrarTransferencia(
                        transferencia(CUENTA_A, CUENTA_B, "25.00", "X".repeat(251))));
        verificarRechazo("Retiro mayor al saldo: el UPDATE condicionado no permite saldos negativos",
                SQLException.class, () -> transaccionDAO.registrarRetiro(retiro(CUENTA_B, "10000.00")));
        verificarRechazo("Depósito a una cuenta INACTIVA: el saldo no cambia",
                SQLException.class, () -> transaccionDAO.registrarDeposito(deposito(CUENTA_INACTIVA, "10.00", null)));
        verificarRechazo("Retiro de una cuenta BLOQUEADA: el saldo no cambia",
                SQLException.class, () -> transaccionDAO.registrarRetiro(retiro(CUENTA_BLOQUEADA, "10.00")));
    }

    private static void probarRetirosSimultaneos() throws Exception {
        System.out.println("\nTransaccionDAO: 10 retiros simultáneos de 30.00 sobre una cuenta con 100.00");
        int intentos = 10;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(intentos);
        List<Future<Transaccion>> resultados = new ArrayList<>();
        for (int i = 0; i < intentos; i++) {
            resultados.add(hilos.submit(() -> {
                Transaccion retiro = retiro(CUENTA_CONCURRENCIA, "30.00");
                salida.await();
                transaccionDAO.registrarRetiro(retiro);
                return retiro;
            }));
        }
        salida.countDown();
        hilos.shutdown();
        int confirmados = 0;
        int rechazados = 0;
        for (Future<Transaccion> resultado : resultados) {
            try {
                anotar(resultado.get());
                confirmados++;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof SQLException) {
                    rechazados++;
                }
            }
        }
        verificar("Se confirman " + confirmados + " retiros, se revierten " + rechazados + " y el saldo queda en "
                + saldosEsperados.get(CUENTA_CONCURRENCIA) + " sin volverse negativo",
                confirmados == 3 && rechazados == 7 && estadoEsperado());
    }

    private static void probarValidacionDeDatos() throws SQLException {
        System.out.println("\nTransaccionDAO: validación de datos antes de abrir la conexión");
        verificarRechazo("Rechaza un depósito con monto cero", SQLException.class,
                () -> transaccionDAO.registrarDeposito(deposito(CUENTA_A, "0", null)));
        verificarRechazo("Rechaza un depósito con monto negativo", SQLException.class,
                () -> transaccionDAO.registrarDeposito(deposito(CUENTA_A, "-25.00", null)));
        verificarRechazo("Rechaza un retiro sin monto", SQLException.class,
                () -> transaccionDAO.registrarRetiro(new Transaccion(TipoTransaccion.RETIRO, null, CUENTA_A, null, null)));
        verificarRechazo("Rechaza un monto con más de dos decimales", SQLException.class,
                () -> transaccionDAO.registrarDeposito(deposito(CUENTA_A, "10.555", null)));
        verificarRechazo("Rechaza una transferencia hacia la misma cuenta", SQLException.class,
                () -> transaccionDAO.registrarTransferencia(transferencia(CUENTA_A, CUENTA_A, "10.00", null)));
        verificarRechazo("Rechaza la misma cuenta aunque el destino esté escrito en minúsculas", SQLException.class,
                () -> transaccionDAO.registrarTransferencia(
                        transferencia(CUENTA_A, CUENTA_A.toLowerCase(), "10.00", null)));
        verificarRechazo("Rechaza un movimiento cuyo tipo no corresponde al método", SQLException.class,
                () -> transaccionDAO.registrarDeposito(retiro(CUENTA_A, "10.00")));
        verificarRechazo("Rechaza un depósito sin cuenta destino", SQLException.class,
                () -> transaccionDAO.registrarDeposito(deposito("   ", "10.00", null)));
        verificarRechazo("Rechaza una transacción nula", SQLException.class,
                () -> transaccionDAO.registrarTransferencia(null));
    }

    private static void probarConsultas() throws SQLException {
        System.out.println("\nTransaccionDAO: consultas");
        verificar("CP-20 buscarPorId devuelve null cuando el movimiento no existe",
                transaccionDAO.buscarPorId(Integer.MAX_VALUE) == null);

        List<Transaccion> deA = transaccionDAO.listarHistorialPorCuenta(CUENTA_A);
        verificar("CP-21 listarHistorialPorCuenta devuelve solo los " + movimientosEsperados.get(CUENTA_A)
                + " movimientos de " + CUENTA_A, deA.size() == movimientosEsperados.get(CUENTA_A)
                && deA.stream().allMatch(t -> involucra(t, CUENTA_A)));
        verificar("El historial está ordenado del movimiento más reciente al más antiguo", estaOrdenado(deA));
        verificar("listarHistorialPorCuenta de una cuenta sin movimientos devuelve una lista vacía",
                transaccionDAO.listarHistorialPorCuenta(CUENTA_INACTIVA).isEmpty());

        List<Transaccion> historial = transaccionDAO.listarHistorial();
        verificar("listarHistorial devuelve todas las filas de transacciones e incluye los movimientos de prueba",
                historial.size() == contar("SELECT COUNT(*) FROM transacciones")
                && ids(historial).containsAll(idsRegistrados) && estaOrdenado(historial));
    }

    private static void probarServicioConMySQL() throws Exception {
        System.out.println("\nTransaccionService + TransaccionDAO contra MySQL (casos de prueba CP-01 a CP-18)");
        BigDecimal diez = new BigDecimal("10.00");

        Transaccion deposito = servicio.depositar(CUENTA_A, new BigDecimal("100.00"));
        anotar(deposito);
        verificar("CP-01 Depósito correcto: movimiento #" + deposito.getIdTransaccion() + " y " + CUENTA_A
                + " en " + saldosEsperados.get(CUENTA_A), deposito.getIdTransaccion() > 0 && estadoEsperado());
        verificarRechazo("CP-02 Depósito a una cuenta inexistente", CuentaNoEncontradaException.class,
                () -> servicio.depositar(CUENTA_INEXISTENTE, diez));
        verificarRechazo("CP-03 Depósito a una cuenta INACTIVA", CuentaInactivaException.class,
                () -> servicio.depositar(CUENTA_INACTIVA, diez));
        verificarRechazo("CP-04 Depósito a una cuenta BLOQUEADA", CuentaInactivaException.class,
                () -> servicio.depositar(CUENTA_BLOQUEADA, diez));
        verificarRechazo("CP-05 Depósito con monto cero", MontoInvalidoException.class,
                () -> servicio.depositar(CUENTA_A, BigDecimal.ZERO));
        verificarRechazo("CP-06 Depósito con monto negativo", MontoInvalidoException.class,
                () -> servicio.depositar(CUENTA_A, diez.negate()));

        Transaccion retiro = servicio.retirar(CUENTA_A, new BigDecimal("49.50"));
        anotar(retiro);
        verificar("CP-07 Retiro correcto: movimiento #" + retiro.getIdTransaccion() + " y " + CUENTA_A
                + " en " + saldosEsperados.get(CUENTA_A), retiro.getIdTransaccion() > 0 && estadoEsperado());
        verificarRechazo("CP-08 Retiro con saldo insuficiente", SaldoInsuficienteException.class,
                () -> servicio.retirar(CUENTA_B, saldosEsperados.get(CUENTA_B).add(new BigDecimal("0.01"))));
        verificarRechazo("CP-09 Retiro de una cuenta inexistente", CuentaNoEncontradaException.class,
                () -> servicio.retirar(CUENTA_INEXISTENTE, diez));
        verificarRechazo("CP-10 Retiro de una cuenta INACTIVA", CuentaInactivaException.class,
                () -> servicio.retirar(CUENTA_INACTIVA, diez));
        verificarRechazo("CP-10 Retiro de una cuenta BLOQUEADA", CuentaInactivaException.class,
                () -> servicio.retirar(CUENTA_BLOQUEADA, diez));
        verificarRechazo("CP-11 Retiro con monto no válido", MontoInvalidoException.class,
                () -> servicio.retirar(CUENTA_A, diez.negate()));

        Transaccion transferencia = servicio.transferir(CUENTA_A, CUENTA_B, new BigDecimal("400.00"));
        anotar(transferencia);
        verificar("CP-12 Transferencia correcta: movimiento #" + transferencia.getIdTransaccion() + ", "
                + CUENTA_A + " en " + saldosEsperados.get(CUENTA_A) + " y " + CUENTA_B + " en "
                + saldosEsperados.get(CUENTA_B), transferencia.getIdTransaccion() > 0 && estadoEsperado());
        verificarRechazo("CP-13 Transferencia con origen inexistente", CuentaNoEncontradaException.class,
                () -> servicio.transferir(CUENTA_INEXISTENTE, CUENTA_B, diez));
        verificarRechazo("CP-14 Transferencia con destino inexistente", CuentaNoEncontradaException.class,
                () -> servicio.transferir(CUENTA_A, CUENTA_INEXISTENTE, diez));
        verificarRechazo("CP-15 Transferencia con origen INACTIVA", CuentaInactivaException.class,
                () -> servicio.transferir(CUENTA_INACTIVA, CUENTA_A, diez));
        verificarRechazo("CP-15 Transferencia con origen BLOQUEADA", CuentaInactivaException.class,
                () -> servicio.transferir(CUENTA_BLOQUEADA, CUENTA_A, diez));
        verificarRechazo("CP-16 Transferencia con destino INACTIVA", CuentaInactivaException.class,
                () -> servicio.transferir(CUENTA_A, CUENTA_INACTIVA, diez));
        verificarRechazo("CP-16 Transferencia con destino BLOQUEADA", CuentaInactivaException.class,
                () -> servicio.transferir(CUENTA_A, CUENTA_BLOQUEADA, diez));
        verificarRechazo("CP-17 Transferencia con fondos insuficientes", SaldoInsuficienteException.class,
                () -> servicio.transferir(CUENTA_A, CUENTA_B, saldosEsperados.get(CUENTA_A).add(new BigDecimal("0.01"))));
        verificarRechazo("CP-18 Transferencia con monto no válido", MontoInvalidoException.class,
                () -> servicio.transferir(CUENTA_A, CUENTA_B, BigDecimal.ZERO));
    }

    private static void probarReportes() throws SQLException {
        System.out.println("\nReporteDAO (casos de prueba CP-22 a CP-27)");
        List<Cuenta> activas = reporteDAO.cuentasActivas();
        verificar("CP-22 cuentasActivas trae solo cuentas ACTIVA e incluye " + CUENTA_A + " y " + CUENTA_B,
                activas.size() == contar("SELECT COUNT(*) FROM cuentas WHERE estado_cuenta = 'ACTIVA'")
                && activas.stream().allMatch(c -> c.getEstadoCuenta() == EstadoCuenta.ACTIVA)
                && numeros(activas).containsAll(List.of(CUENTA_A, CUENTA_B)));

        List<Cuenta> bloqueadas = reporteDAO.cuentasBloqueadas();
        verificar("CP-23 cuentasBloqueadas trae solo cuentas BLOQUEADA e incluye " + CUENTA_BLOQUEADA,
                bloqueadas.size() == contar("SELECT COUNT(*) FROM cuentas WHERE estado_cuenta = 'BLOQUEADA'")
                && bloqueadas.stream().allMatch(c -> c.getEstadoCuenta() == EstadoCuenta.BLOQUEADA)
                && numeros(bloqueadas).contains(CUENTA_BLOQUEADA));

        List<Cliente> clientes = reporteDAO.clientesRegistrados();
        verificar("CP-24 clientesRegistrados trae todos los clientes, incluido " + DUI_PRUEBA + " con su teléfono",
                clientes.size() == contar("SELECT COUNT(*) FROM clientes")
                && clientes.stream().anyMatch(c -> c.getDuiCliente().equals(DUI_PRUEBA)
                        && c.getTelefonos().equals(List.of(TELEFONO_PRUEBA))));

        List<Transaccion> historial = reporteDAO.historial();
        verificar("CP-25 historial trae todas las filas de transacciones, incluidos los " + idsRegistrados.size()
                + " movimientos de prueba", historial.size() == contar("SELECT COUNT(*) FROM transacciones")
                && ids(historial).containsAll(idsRegistrados));

        List<Transaccion> deB = reporteDAO.transaccionesPorCuenta(CUENTA_B);
        verificar("CP-26 transaccionesPorCuenta trae los " + movimientosEsperados.get(CUENTA_B) + " movimientos de "
                + CUENTA_B + " y una lista vacía para " + CUENTA_BLOQUEADA,
                deB.size() == movimientosEsperados.get(CUENTA_B)
                && deB.stream().allMatch(t -> involucra(t, CUENTA_B))
                && reporteDAO.transaccionesPorCuenta(CUENTA_BLOQUEADA).isEmpty());

        BigDecimal depositado = reporteDAO.totalDepositado();
        BigDecimal retirado = reporteDAO.totalRetirado();
        BigDecimal transferido = reporteDAO.totalTransferido();
        verificar("CP-27 Los totales aumentaron exactamente lo registrado en la prueba (depositado +"
                + montosRegistrados.get(TipoTransaccion.DEPOSITO) + ", retirado +"
                + montosRegistrados.get(TipoTransaccion.RETIRO) + ", transferido +"
                + montosRegistrados.get(TipoTransaccion.TRANSFERENCIA) + ")",
                aumentoEsperado(TipoTransaccion.DEPOSITO, depositado)
                && aumentoEsperado(TipoTransaccion.RETIRO, retirado)
                && aumentoEsperado(TipoTransaccion.TRANSFERENCIA, transferido));
        verificar("CP-27 Cada total coincide con la suma del historial por tipo de movimiento",
                depositado.compareTo(sumar(historial, TipoTransaccion.DEPOSITO)) == 0
                && retirado.compareTo(sumar(historial, TipoTransaccion.RETIRO)) == 0
                && transferido.compareTo(sumar(historial, TipoTransaccion.TRANSFERENCIA)) == 0);
    }

    private static void limpiarAlTerminar() {
        System.out.println("\nLimpieza");
        try {
            limpiarDatosDePrueba();
            boolean totalesRestaurados = true;
            for (TipoTransaccion tipo : TipoTransaccion.values()) {
                totalesRestaurados &= totalesIniciales.get(tipo) == null
                        || reporteDAO.totalPorTipo(tipo).compareTo(totalesIniciales.get(tipo)) == 0;
            }
            verificar("Se eliminaron el cliente, las cuentas y los movimientos de prueba; los totales volvieron a sus valores iniciales",
                    contar("SELECT COUNT(*) FROM clientes WHERE dui_cliente = ?", DUI_PRUEBA) == 0
                    && contar("SELECT COUNT(*) FROM cuentas WHERE dui_cliente = ?", DUI_PRUEBA) == 0
                    && totalesRestaurados);
        } catch (SQLException e) {
            System.out.println("No se pudieron limpiar los datos de prueba: " + e.getMessage());
        }
    }

    private static void mostrarReportes() {
        System.out.println("\nREPORTES CON LOS DATOS ACTUALES DE LA BASE");
        try {
            System.out.println("\n1. Cuentas activas");
            List<Cuenta> activas = reporteDAO.cuentasActivas();
            imprimirCuentas(activas);
            System.out.println("\n2. Cuentas bloqueadas");
            imprimirCuentas(reporteDAO.cuentasBloqueadas());
            System.out.println("\n3. Clientes registrados");
            for (Cliente c : reporteDAO.clientesRegistrados()) {
                System.out.printf("   %-11s %-18s %-10s %s%n", c.getDuiCliente(), c.getNombreCompletoCliente(),
                        c.getEstadoCliente(), c.getTelefonos());
            }
            System.out.println("\n4. Historial de transacciones");
            imprimirMovimientos(reporteDAO.historial());
            if (!activas.isEmpty()) {
                String cuenta = activas.get(0).getNumeroCuenta();
                System.out.println("\n5. Transacciones de la cuenta " + cuenta);
                imprimirMovimientos(reporteDAO.transaccionesPorCuenta(cuenta));
            }
            System.out.println("\n6. Total depositado");
            System.out.println("   " + reporteDAO.totalDepositado());
            System.out.println("\n7. Total retirado");
            System.out.println("   " + reporteDAO.totalRetirado());
            System.out.println("\n8. Total transferido");
            System.out.println("   " + reporteDAO.totalTransferido());
        } catch (SQLException e) {
            System.out.println("No se pudieron generar los reportes: " + e.getMessage());
        }
    }

    private static void imprimirCuentas(List<Cuenta> cuentas) {
        if (cuentas.isEmpty()) {
            System.out.println("   (sin registros)");
        }
        for (Cuenta c : cuentas) {
            System.out.printf("   %-11s %-11s %-10s %10s  %s  %s%n", c.getNumeroCuenta(), c.getDuiCliente(),
                    c.getTipoCuenta(), c.getSaldo(), c.getFechaApertura(), c.getEstadoCuenta());
        }
    }

    private static void imprimirMovimientos(List<Transaccion> movimientos) {
        if (movimientos.isEmpty()) {
            System.out.println("   (sin registros)");
        }
        for (Transaccion t : movimientos) {
            System.out.printf("   #%-4d %-13s %10s  %s  %-11s -> %-11s %s%n", t.getIdTransaccion(),
                    t.getTipoTransaccion(), t.getMonto(), t.getFechaHora().format(FORMATO_FECHA),
                    valorOGuion(t.getNumeroCuentaOrigen()), valorOGuion(t.getNumeroCuentaDestino()),
                    valorOGuion(t.getDescripcion()));
        }
    }

    private static boolean crearCuenta(String numero, String saldo, EstadoCuenta estado) throws SQLException {
        saldosEsperados.put(numero, new BigDecimal(saldo));
        movimientosEsperados.put(numero, 0);
        return cuentaDAO.insertar(new Cuenta(numero, DUI_PRUEBA, TipoCuenta.AHORRO, new BigDecimal(saldo),
                LocalDate.now(), estado));
    }

    private static Transaccion deposito(String destino, String monto, String descripcion) {
        return new Transaccion(TipoTransaccion.DEPOSITO, new BigDecimal(monto), null, destino, descripcion);
    }

    private static Transaccion retiro(String origen, String monto) {
        return new Transaccion(TipoTransaccion.RETIRO, new BigDecimal(monto), origen, null, "Retiro de prueba DAO");
    }

    private static Transaccion transferencia(String origen, String destino, String monto, String descripcion) {
        return new Transaccion(TipoTransaccion.TRANSFERENCIA, new BigDecimal(monto), origen, destino, descripcion);
    }

    private static void anotar(Transaccion transaccion) {
        idsRegistrados.add(transaccion.getIdTransaccion());
        montosRegistrados.merge(transaccion.getTipoTransaccion(), transaccion.getMonto(), BigDecimal::add);
        if (transaccion.getNumeroCuentaOrigen() != null) {
            saldosEsperados.merge(transaccion.getNumeroCuentaOrigen(), transaccion.getMonto().negate(), BigDecimal::add);
            movimientosEsperados.merge(transaccion.getNumeroCuentaOrigen(), 1, Integer::sum);
        }
        if (transaccion.getNumeroCuentaDestino() != null) {
            saldosEsperados.merge(transaccion.getNumeroCuentaDestino(), transaccion.getMonto(), BigDecimal::add);
            movimientosEsperados.merge(transaccion.getNumeroCuentaDestino(), 1, Integer::sum);
        }
    }

    private static boolean estadoEsperado() throws SQLException {
        for (Map.Entry<String, BigDecimal> esperado : saldosEsperados.entrySet()) {
            String numero = esperado.getKey();
            Cuenta cuenta = cuentaDAO.buscarPorNumero(numero);
            int movimientos = contarMovimientos(numero);
            if (cuenta == null || cuenta.getSaldo().compareTo(esperado.getValue()) != 0
                    || movimientos != movimientosEsperados.get(numero)) {
                System.out.println("        " + numero + ": se esperaba saldo " + esperado.getValue() + " y "
                        + movimientosEsperados.get(numero) + " movimientos, MySQL tiene "
                        + (cuenta == null ? "la cuenta ausente" : "saldo " + cuenta.getSaldo())
                        + " y " + movimientos + " movimientos");
                return false;
            }
        }
        return true;
    }

    private static void verificarRechazo(String descripcion, Class<? extends Exception> esperada, Operacion operacion)
            throws SQLException {
        Exception lanzada = null;
        try {
            operacion.ejecutar();
        } catch (Exception e) {
            lanzada = e;
        }
        verificar(descripcion + " -> " + esperada.getSimpleName(), esperada.isInstance(lanzada) && estadoEsperado());
        System.out.println("        " + (lanzada == null ? "No se lanzó ninguna excepción"
                : lanzada.getClass().getSimpleName() + ": " + lanzada.getMessage()));
    }

    private static boolean aumentoEsperado(TipoTransaccion tipo, BigDecimal totalActual) {
        return totalActual.compareTo(totalesIniciales.get(tipo).add(montosRegistrados.get(tipo))) == 0;
    }

    private static BigDecimal sumar(List<Transaccion> movimientos, TipoTransaccion tipo) {
        return movimientos.stream()
                .filter(t -> t.getTipoTransaccion() == tipo)
                .map(Transaccion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean estaOrdenado(List<Transaccion> movimientos) {
        for (int i = 1; i < movimientos.size(); i++) {
            Transaccion anterior = movimientos.get(i - 1);
            Transaccion actual = movimientos.get(i);
            int comparacion = anterior.getFechaHora().compareTo(actual.getFechaHora());
            if (comparacion < 0 || (comparacion == 0 && anterior.getIdTransaccion() < actual.getIdTransaccion())) {
                return false;
            }
        }
        return true;
    }

    private static boolean involucra(Transaccion transaccion, String numeroCuenta) {
        return numeroCuenta.equals(transaccion.getNumeroCuentaOrigen())
                || numeroCuenta.equals(transaccion.getNumeroCuentaDestino());
    }

    private static List<Long> ids(List<Transaccion> movimientos) {
        return movimientos.stream().map(Transaccion::getIdTransaccion).toList();
    }

    private static List<String> numeros(List<Cuenta> cuentas) {
        return cuentas.stream().map(Cuenta::getNumeroCuenta).toList();
    }

    private static String valorOGuion(String valor) {
        return valor == null ? "-" : valor;
    }

    private static int contarMovimientos(String numeroCuenta) throws SQLException {
        return contar("SELECT COUNT(*) FROM transacciones WHERE cuenta_origen = ? OR cuenta_destino = ?",
                numeroCuenta, numeroCuenta);
    }

    private static int contar(String sql, String... parametros) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void limpiarDatosDePrueba() throws SQLException {
        String[] sentencias = {
            "DELETE FROM transacciones WHERE cuenta_origen IN (SELECT numero_cuenta FROM cuentas WHERE dui_cliente = ?) "
                    + "OR cuenta_destino IN (SELECT numero_cuenta FROM cuentas WHERE dui_cliente = ?)",
            "DELETE FROM cuentas WHERE dui_cliente = ?",
            "DELETE FROM telefonos_clientes WHERE dui_cliente = ?",
            "DELETE FROM clientes WHERE dui_cliente = ?"
        };
        try (Connection con = ConexionBD.getConexion()) {
            for (String sql : sentencias) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    long parametros = sql.chars().filter(c -> c == '?').count();
                    for (int i = 1; i <= parametros; i++) {
                        ps.setString(i, DUI_PRUEBA);
                    }
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void verificar(String descripcion, boolean resultado) {
        total++;
        if (resultado) {
            correctas++;
        }
        System.out.println((resultado ? "[OK]    " : "[FALLO] ") + descripcion);
    }

    private interface Operacion {
        void ejecutar() throws Exception;
    }
}
