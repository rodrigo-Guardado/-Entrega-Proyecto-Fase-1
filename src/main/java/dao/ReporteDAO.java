package dao;

import conexion.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import modelo.Cliente;
import modelo.Cuenta;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReporteDAO {

    private static final Logger LOGGER = LogManager.getLogger(ReporteDAO.class);

    private static final String CUENTAS_POR_ESTADO = "SELECT numero_cuenta, dui_cliente, tipo_cuenta, saldo, "
            + "fecha_apertura, estado_cuenta FROM cuentas WHERE estado_cuenta = ? ORDER BY numero_cuenta";
    private static final String TOTAL_POR_TIPO = "SELECT COALESCE(SUM(monto), 0) AS total FROM transacciones "
            + "WHERE tipo_transaccion = ?";

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    public List<Cuenta> cuentasActivas() throws SQLException {
        return cuentasPorEstado(EstadoCuenta.ACTIVA);
    }

    public List<Cuenta> cuentasBloqueadas() throws SQLException {
        return cuentasPorEstado(EstadoCuenta.BLOQUEADA);
    }

    public List<Cliente> clientesRegistrados() throws SQLException {
        try {
            List<Cliente> clientes = clienteDAO.consultarTodos();
            LOGGER.info("Reporte de clientes registrados: {} registro(s)", clientes.size());
            return clientes;
        } catch (SQLException e) {
            LOGGER.error("Error SQL en el reporte de clientes registrados: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Transaccion> historial() throws SQLException {
        return transaccionDAO.listarHistorial();
    }

    public List<Transaccion> transaccionesPorCuenta(String numeroCuenta) throws SQLException {
        return transaccionDAO.listarHistorialPorCuenta(numeroCuenta);
    }

    public BigDecimal totalPorTipo(TipoTransaccion tipo) throws SQLException {
        Objects.requireNonNull(tipo, "El tipo de transacción es obligatorio.");
        BigDecimal total;
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(TOTAL_POR_TIPO)) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                total = rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            LOGGER.error("Error SQL en el reporte del total de {}: {}", tipo, e.getMessage(), e);
            throw e;
        }
        LOGGER.info("Reporte del total de {}: {}", tipo, total);
        return total;
    }

    public BigDecimal totalDepositado() throws SQLException {
        return totalPorTipo(TipoTransaccion.DEPOSITO);
    }

    public BigDecimal totalRetirado() throws SQLException {
        return totalPorTipo(TipoTransaccion.RETIRO);
    }

    public BigDecimal totalTransferido() throws SQLException {
        return totalPorTipo(TipoTransaccion.TRANSFERENCIA);
    }

    private List<Cuenta> cuentasPorEstado(EstadoCuenta estado) throws SQLException {
        List<Cuenta> cuentas = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(CUENTAS_POR_ESTADO)) {
            ps.setString(1, estado.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cuentas.add(new Cuenta(
                            rs.getString("numero_cuenta"),
                            rs.getString("dui_cliente"),
                            TipoCuenta.valueOf(rs.getString("tipo_cuenta")),
                            rs.getBigDecimal("saldo"),
                            rs.getObject("fecha_apertura", LocalDate.class),
                            EstadoCuenta.valueOf(rs.getString("estado_cuenta"))));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error SQL en el reporte de cuentas en estado {}: {}", estado, e.getMessage(), e);
            throw e;
        }
        LOGGER.info("Reporte de cuentas en estado {}: {} registro(s)", estado, cuentas.size());
        return cuentas;
    }
}
