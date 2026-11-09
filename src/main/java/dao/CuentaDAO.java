package dao;

import conexion.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.Cuenta;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;

public class CuentaDAO {

    private static final String SELECT_CUENTAS = "SELECT numero_cuenta, dui_cliente, tipo_cuenta, saldo, "
            + "fecha_apertura, estado_cuenta FROM cuentas";
    private static final String CONSULTAR_TODAS = SELECT_CUENTAS + " ORDER BY numero_cuenta";
    private static final String BUSCAR_POR_NUMERO = SELECT_CUENTAS + " WHERE numero_cuenta = ?";
    private static final String BUSCAR_POR_CLIENTE = SELECT_CUENTAS + " WHERE dui_cliente = ? ORDER BY numero_cuenta";
    private static final String INSERTAR = "INSERT INTO cuentas (numero_cuenta, dui_cliente, tipo_cuenta, saldo, "
            + "fecha_apertura, estado_cuenta) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String ULTIMO_NUMERO = "SELECT MAX(CAST(SUBSTRING(numero_cuenta, 5) AS UNSIGNED)) "
            + "FROM cuentas WHERE numero_cuenta LIKE 'CTA-%'";

    public boolean insertar(Cuenta cuenta) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(INSERTAR)) {
            ps.setString(1, cuenta.getNumeroCuenta());
            ps.setString(2, cuenta.getDuiCliente());
            ps.setString(3, cuenta.getTipoCuenta().name());
            ps.setBigDecimal(4, cuenta.getSaldo());
            ps.setObject(5, cuenta.getFechaApertura());
            ps.setString(6, cuenta.getEstadoCuenta().name());
            return ps.executeUpdate() > 0;
        }
    }

    public List<Cuenta> consultarTodas() throws SQLException {
        return consultar(CONSULTAR_TODAS);
    }

    public Cuenta buscarPorNumero(String numeroCuenta) throws SQLException {
        List<Cuenta> cuentas = consultar(BUSCAR_POR_NUMERO, numeroCuenta);
        return cuentas.isEmpty() ? null : cuentas.get(0);
    }

    public List<Cuenta> buscarPorCliente(String duiCliente) throws SQLException {
        return consultar(BUSCAR_POR_CLIENTE, duiCliente);
    }

    public boolean existeNumeroCuenta(String numeroCuenta) throws SQLException {
        return buscarPorNumero(numeroCuenta) != null;
    }

    //Busca el número más alto con formato CTA-000000 y devuelve el siguiente
    public String generarNumeroCuenta() throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(ULTIMO_NUMERO);
             ResultSet rs = ps.executeQuery()) {
            long ultimo = rs.next() ? rs.getLong(1) : 0;
            return String.format("CTA-%06d", ultimo + 1);
        }
    }

    private List<Cuenta> consultar(String sql, String... parametros) throws SQLException {
        List<Cuenta> cuentas = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
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
        }
        return cuentas;
    }
}
