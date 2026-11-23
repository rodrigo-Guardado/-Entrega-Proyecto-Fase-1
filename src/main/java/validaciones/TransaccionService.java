package validaciones;

import dao.CuentaDAO;
import dao.TransaccionDAO;
import excepciones.CuentaInactivaException;
import excepciones.CuentaNoEncontradaException;
import excepciones.MontoInvalidoException;
import excepciones.SaldoInsuficienteException;
import java.math.BigDecimal;
import java.sql.SQLException;
import modelo.Cuenta;
import modelo.EstadoCuenta;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reglas de negocio de depósitos, retiros y transferencias.
 * Valida TODO antes de pedirle al DAO que modifique saldos. Esta clase no ejecuta SQL ni
 * calcula saldos: eso lo hace TransaccionDAO (Integrante 4) dentro de su transacción JDBC.
 *
 * Log4J: INFO = operación exitosa, WARN = rechazo por regla de negocio, ERROR = fallo SQL.
 */
public class TransaccionService {
    private static final Logger LOGGER = LogManager.getLogger(TransaccionService.class);

    private final CuentaDAO cuentaDAO;
    private final TransaccionDAO transaccionDAO;

    public TransaccionService(CuentaDAO cuentaDAO, TransaccionDAO transaccionDAO) {
        this.cuentaDAO = cuentaDAO;
        this.transaccionDAO = transaccionDAO;
    }

    // ------------------------------------------------------------------ operaciones

    public Transaccion depositar(String numeroCuenta, BigDecimal monto)
            throws CuentaNoEncontradaException, CuentaInactivaException, MontoInvalidoException, SQLException {
        return depositar(numeroCuenta, monto, "Depósito");
    }

    public Transaccion depositar(String numeroCuenta, BigDecimal monto, String descripcion)
            throws CuentaNoEncontradaException, CuentaInactivaException, MontoInvalidoException, SQLException {
        try {
            Cuenta cuenta = obtenerCuenta(numeroCuenta);
            validarCuentaActiva(cuenta);
            validarMonto(monto);

            Transaccion transaccion = new Transaccion(TipoTransaccion.DEPOSITO, monto, null, numeroCuenta, descripcion);
            transaccionDAO.registrarDeposito(transaccion);
            LOGGER.info("DEPOSITO exitoso: cuenta={}, monto={}", numeroCuenta, monto);
            return transaccion;
        } catch (CuentaNoEncontradaException | CuentaInactivaException | MontoInvalidoException e) {
            LOGGER.warn("DEPOSITO rechazado: cuenta={}, monto={}, motivo={}", numeroCuenta, monto, e.getMessage());
            throw e;
        } catch (SQLException e) {
            LOGGER.error("DEPOSITO fallido por error de base de datos: cuenta={}, monto={}", numeroCuenta, monto, e);
            throw e;
        }
    }

    public Transaccion retirar(String numeroCuenta, BigDecimal monto)
            throws CuentaNoEncontradaException, CuentaInactivaException, MontoInvalidoException,
            SaldoInsuficienteException, SQLException {
        return retirar(numeroCuenta, monto, "Retiro");
    }

    public Transaccion retirar(String numeroCuenta, BigDecimal monto, String descripcion)
            throws CuentaNoEncontradaException, CuentaInactivaException, MontoInvalidoException,
            SaldoInsuficienteException, SQLException {
        try {
            Cuenta cuenta = obtenerCuenta(numeroCuenta);
            validarCuentaActiva(cuenta);
            validarMonto(monto);
            validarSaldo(cuenta, monto);

            Transaccion transaccion = new Transaccion(TipoTransaccion.RETIRO, monto, numeroCuenta, null, descripcion);
            transaccionDAO.registrarRetiro(transaccion);
            LOGGER.info("RETIRO exitoso: cuenta={}, monto={}", numeroCuenta, monto);
            return transaccion;
        } catch (CuentaNoEncontradaException | CuentaInactivaException | MontoInvalidoException
                 | SaldoInsuficienteException e) {
            LOGGER.warn("RETIRO rechazado: cuenta={}, monto={}, motivo={}", numeroCuenta, monto, e.getMessage());
            throw e;
        } catch (SQLException e) {
            LOGGER.error("RETIRO fallido por error de base de datos: cuenta={}, monto={}", numeroCuenta, monto, e);
            throw e;
        }
    }

    public Transaccion transferir(String cuentaOrigen, String cuentaDestino, BigDecimal monto)
            throws CuentaNoEncontradaException, CuentaInactivaException, MontoInvalidoException,
            SaldoInsuficienteException, SQLException {
        return transferir(cuentaOrigen, cuentaDestino, monto, "Transferencia");
    }

    public Transaccion transferir(String cuentaOrigen, String cuentaDestino, BigDecimal monto, String descripcion)
            throws CuentaNoEncontradaException, CuentaInactivaException, MontoInvalidoException,
            SaldoInsuficienteException, SQLException {
        try {
            Cuenta origen = obtenerCuenta(cuentaOrigen);
            Cuenta destino = obtenerCuenta(cuentaDestino);
            validarCuentaActiva(origen);
            validarCuentaActiva(destino);
            validarMonto(monto);
            validarSaldo(origen, monto);

            Transaccion transaccion = new Transaccion(TipoTransaccion.TRANSFERENCIA, monto,
                    cuentaOrigen, cuentaDestino, descripcion);
            transaccionDAO.registrarTransferencia(transaccion);
            LOGGER.info("TRANSFERENCIA exitosa: origen={}, destino={}, monto={}", cuentaOrigen, cuentaDestino, monto);
            return transaccion;
        } catch (CuentaNoEncontradaException | CuentaInactivaException | MontoInvalidoException
                 | SaldoInsuficienteException e) {
            LOGGER.warn("TRANSFERENCIA rechazada: origen={}, destino={}, monto={}, motivo={}",
                    cuentaOrigen, cuentaDestino, monto, e.getMessage());
            throw e;
        } catch (SQLException e) {
            LOGGER.error("TRANSFERENCIA fallida por error de base de datos: origen={}, destino={}, monto={}",
                    cuentaOrigen, cuentaDestino, monto, e);
            throw e;
        }
    }

    // ------------------------------------------------------------------ validaciones

    /** Busca la cuenta; si no existe lanza CuentaNoEncontradaException. */
    private Cuenta obtenerCuenta(String numeroCuenta) throws CuentaNoEncontradaException, SQLException {
        Cuenta cuenta = (numeroCuenta == null) ? null : cuentaDAO.buscarPorNumero(numeroCuenta.trim());
        if (cuenta == null) {
            throw new CuentaNoEncontradaException(numeroCuenta);
        }
        return cuenta;
    }

    /** La cuenta debe estar ACTIVA (INACTIVA y BLOQUEADA no permiten operar). */
    public static void validarCuentaActiva(Cuenta cuenta) throws CuentaInactivaException {
        if (cuenta.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new CuentaInactivaException(cuenta.getNumeroCuenta(), cuenta.getEstadoCuenta());
        }
    }

    /** El monto debe existir y ser mayor que cero. */
    public static void validarMonto(BigDecimal monto) throws MontoInvalidoException {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException(monto);
        }
    }

    /** El saldo de la cuenta debe cubrir el monto (saldo >= monto). */
    public static void validarSaldo(Cuenta cuenta, BigDecimal monto) throws SaldoInsuficienteException {
        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(cuenta.getNumeroCuenta(), cuenta.getSaldo(), monto);
        }
    }
}
