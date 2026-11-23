package dao;

import conexion.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TransaccionDAO {

    private static final Logger LOGGER = LogManager.getLogger(TransaccionDAO.class);

    private static final String ABONAR = "UPDATE cuentas SET saldo = saldo + ? "
            + "WHERE numero_cuenta = ? AND estado_cuenta = 'ACTIVA'";
    private static final String DEBITAR = "UPDATE cuentas SET saldo = saldo - ? "
            + "WHERE numero_cuenta = ? AND estado_cuenta = 'ACTIVA' AND saldo >= ?";
    private static final String INSERTAR = "INSERT INTO transacciones (tipo_transaccion, monto, fecha_hora, "
            + "cuenta_origen, cuenta_destino, descripcion) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_TRANSACCIONES = "SELECT id_transaccion, tipo_transaccion, monto, "
            + "fecha_hora, cuenta_origen, cuenta_destino, descripcion FROM transacciones";
    private static final String ORDEN_RECIENTES = " ORDER BY fecha_hora DESC, id_transaccion DESC";
    private static final String BUSCAR_POR_ID = SELECT_TRANSACCIONES + " WHERE id_transaccion = ?";
    private static final String LISTAR_HISTORIAL = SELECT_TRANSACCIONES + ORDEN_RECIENTES;
    private static final String LISTAR_POR_CUENTA = SELECT_TRANSACCIONES
            + " WHERE cuenta_origen = ? OR cuenta_destino = ?" + ORDEN_RECIENTES;

    public void registrarDeposito(Transaccion transaccion) throws SQLException {
        registrar(transaccion, TipoTransaccion.DEPOSITO);
    }

    public void registrarRetiro(Transaccion transaccion) throws SQLException {
        registrar(transaccion, TipoTransaccion.RETIRO);
    }

    public void registrarTransferencia(Transaccion transaccion) throws SQLException {
        registrar(transaccion, TipoTransaccion.TRANSFERENCIA);
    }

    public Transaccion buscarPorId(long idTransaccion) throws SQLException {
        List<Transaccion> encontradas = consultar("del movimiento #" + idTransaccion, BUSCAR_POR_ID, idTransaccion);
        return encontradas.isEmpty() ? null : encontradas.get(0);
    }

    public List<Transaccion> listarHistorial() throws SQLException {
        return consultar("del historial general", LISTAR_HISTORIAL);
    }

    public List<Transaccion> listarHistorialPorCuenta(String numeroCuenta) throws SQLException {
        String cuenta = normalizarCuenta(numeroCuenta);
        if (cuenta == null) {
            return new ArrayList<>();
        }
        return consultar("del historial de la cuenta " + cuenta, LISTAR_POR_CUENTA, cuenta, cuenta);
    }

    private void registrar(Transaccion transaccion, TipoTransaccion tipo) throws SQLException {
        prepararMovimiento(transaccion, tipo);
        Connection con = null;
        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);
            aplicarSaldos(con, transaccion);
            long idGenerado = insertar(con, transaccion);
            con.commit();
            transaccion.setIdTransaccion(idGenerado);
        } catch (SQLException e) {
            revertir(con, tipo, e);
            LOGGER.error("No se pudo registrar {} ({}): {}", tipo, resumen(transaccion), detalle(e));
            throw e;
        } catch (RuntimeException e) {
            revertir(con, tipo, e);
            LOGGER.error("Error inesperado al registrar {} ({})", tipo, resumen(transaccion), e);
            throw e;
        } finally {
            cerrar(con);
        }
        LOGGER.info("Commit del movimiento #{} ({}): {}", transaccion.getIdTransaccion(), tipo, resumen(transaccion));
    }

    private void prepararMovimiento(Transaccion transaccion, TipoTransaccion tipo) throws SQLException {
        if (transaccion != null) {
            transaccion.setNumeroCuentaOrigen(normalizarCuenta(transaccion.getNumeroCuentaOrigen()));
            transaccion.setNumeroCuentaDestino(normalizarCuenta(transaccion.getNumeroCuentaDestino()));
        }
        String error = validarMovimiento(transaccion, tipo);
        if (error != null) {
            LOGGER.warn("Operación {} rechazada antes de abrir la conexión: {}", tipo, error);
            throw new SQLException(error);
        }
        LocalDateTime fechaHora = transaccion.getFechaHora() == null ? LocalDateTime.now() : transaccion.getFechaHora();
        transaccion.setFechaHora(fechaHora.truncatedTo(ChronoUnit.SECONDS));
    }

    private String validarMovimiento(Transaccion transaccion, TipoTransaccion tipo) {
        if (transaccion == null) {
            return "La transacción es obligatoria.";
        }
        if (transaccion.getTipoTransaccion() != tipo) {
            return "Se esperaba un movimiento de tipo " + tipo + " y se recibió " + transaccion.getTipoTransaccion() + ".";
        }
        BigDecimal monto = transaccion.getMonto();
        if (monto == null || monto.signum() <= 0) {
            return "El monto debe ser mayor que cero.";
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            return "El monto no puede tener más de dos decimales.";
        }
        String origen = transaccion.getNumeroCuentaOrigen();
        String destino = transaccion.getNumeroCuentaDestino();
        if (tipo == TipoTransaccion.DEPOSITO && (destino == null || origen != null)) {
            return "Un depósito solo debe indicar la cuenta destino.";
        }
        if (tipo == TipoTransaccion.RETIRO && (origen == null || destino != null)) {
            return "Un retiro solo debe indicar la cuenta origen.";
        }
        if (tipo == TipoTransaccion.TRANSFERENCIA && (origen == null || destino == null)) {
            return "Una transferencia debe indicar la cuenta origen y la cuenta destino.";
        }
        if (tipo == TipoTransaccion.TRANSFERENCIA && origen.equals(destino)) {
            return "La cuenta origen y la cuenta destino deben ser diferentes.";
        }
        return null;
    }

    private void aplicarSaldos(Connection con, Transaccion transaccion) throws SQLException {
        switch (transaccion.getTipoTransaccion()) {
            case DEPOSITO -> abonar(con, transaccion.getNumeroCuentaDestino(), transaccion.getMonto());
            case RETIRO -> debitar(con, transaccion.getNumeroCuentaOrigen(), transaccion.getMonto());
            case TRANSFERENCIA -> {
                debitar(con, transaccion.getNumeroCuentaOrigen(), transaccion.getMonto());
                abonar(con, transaccion.getNumeroCuentaDestino(), transaccion.getMonto());
            }
        }
    }

    private void debitar(Connection con, String numeroCuenta, BigDecimal monto) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(DEBITAR)) {
            ps.setBigDecimal(1, monto);
            ps.setString(2, numeroCuenta);
            ps.setBigDecimal(3, monto);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se pudo debitar la cuenta " + numeroCuenta
                        + ": no existe, no está ACTIVA o su saldo es menor que " + monto + ".");
            }
        }
    }

    private void abonar(Connection con, String numeroCuenta, BigDecimal monto) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(ABONAR)) {
            ps.setBigDecimal(1, monto);
            ps.setString(2, numeroCuenta);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se pudo abonar a la cuenta " + numeroCuenta
                        + ": no existe o no está ACTIVA.");
            }
        }
    }

    private long insertar(Connection con, Transaccion transaccion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, transaccion.getTipoTransaccion().name());
            ps.setBigDecimal(2, transaccion.getMonto());
            ps.setObject(3, transaccion.getFechaHora());
            ps.setString(4, transaccion.getNumeroCuentaOrigen());
            ps.setString(5, transaccion.getNumeroCuentaDestino());
            ps.setString(6, transaccion.getDescripcion());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getLong(1);
                }
            }
        }
        throw new SQLException("MySQL no devolvió el id generado para el movimiento.");
    }

    private void revertir(Connection con, TipoTransaccion tipo, Exception causa) {
        if (con == null) {
            return;
        }
        try {
            con.rollback();
            LOGGER.warn("Rollback de la operación {}: se deshicieron sus cambios y ningún saldo fue modificado", tipo);
        } catch (SQLException e) {
            causa.addSuppressed(e);
            LOGGER.error("Falló el rollback de la operación {}", tipo, e);
        }
    }

    private void cerrar(Connection con) {
        if (con == null) {
            return;
        }
        try {
            con.close();
        } catch (SQLException e) {
            LOGGER.warn("No se pudo cerrar la conexión: {}", e.getMessage());
        }
    }

    private List<Transaccion> consultar(String descripcion, String sql, Object... parametros) throws SQLException {
        List<Transaccion> transacciones = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transacciones.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error SQL en la consulta {}: {}", descripcion, e.getMessage(), e);
            throw e;
        }
        LOGGER.info("Consulta {}: {} movimiento(s)", descripcion, transacciones.size());
        return transacciones;
    }

    private static Transaccion mapear(ResultSet rs) throws SQLException {
        return new Transaccion(
                rs.getLong("id_transaccion"),
                TipoTransaccion.valueOf(rs.getString("tipo_transaccion")),
                rs.getBigDecimal("monto"),
                rs.getObject("fecha_hora", LocalDateTime.class),
                rs.getString("cuenta_origen"),
                rs.getString("cuenta_destino"),
                rs.getString("descripcion"));
    }

    private static String normalizarCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return null;
        }
        return numeroCuenta.trim().toUpperCase(Locale.ROOT);
    }

    private static String detalle(SQLException e) {
        if (e.getSQLState() == null) {
            return e.getMessage();
        }
        return e.getMessage() + " [SQLState " + e.getSQLState() + ", código de error " + e.getErrorCode() + "]";
    }

    private static String resumen(Transaccion transaccion) {
        StringBuilder texto = new StringBuilder();
        if (transaccion.getNumeroCuentaOrigen() != null) {
            texto.append("origen=").append(transaccion.getNumeroCuentaOrigen()).append(", ");
        }
        if (transaccion.getNumeroCuentaDestino() != null) {
            texto.append("destino=").append(transaccion.getNumeroCuentaDestino()).append(", ");
        }
        return texto.append("monto=").append(transaccion.getMonto()).toString();
    }
}
