package dao;

import conexion.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelo.Cliente;
import modelo.EstadoCliente;

public class ClienteDAO {

    private static final String SELECT_CLIENTES = "SELECT dui_cliente, nombre_completo_cliente, correo_cliente, "
            + "direccion_cliente, estado_cliente FROM clientes";
    private static final String CONSULTAR_TODOS = SELECT_CLIENTES + " ORDER BY nombre_completo_cliente";
    private static final String BUSCAR_POR_DUI = SELECT_CLIENTES + " WHERE dui_cliente = ?";
    private static final String BUSCAR_POR_NOMBRE = SELECT_CLIENTES
            + " WHERE nombre_completo_cliente LIKE ? ORDER BY nombre_completo_cliente";
    private static final String INSERTAR = "INSERT INTO clientes (dui_cliente, nombre_completo_cliente, "
            + "correo_cliente, direccion_cliente, estado_cliente) VALUES (?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR = "UPDATE clientes SET nombre_completo_cliente = ?, correo_cliente = ?, "
            + "direccion_cliente = ?, estado_cliente = ? WHERE dui_cliente = ?";
    private static final String CAMBIAR_ESTADO = "UPDATE clientes SET estado_cliente = ? WHERE dui_cliente = ?";
    private static final String CONSULTAR_TELEFONOS = "SELECT telefono_cliente FROM telefonos_clientes "
            + "WHERE dui_cliente = ? ORDER BY id_telefono";
    private static final String INSERTAR_TELEFONO = "INSERT INTO telefonos_clientes (dui_cliente, telefono_cliente) "
            + "VALUES (?, ?)";
    private static final String ELIMINAR_TELEFONOS = "DELETE FROM telefonos_clientes WHERE dui_cliente = ?";

    public boolean insertar(Cliente cliente) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            //Cliente y teléfonos se guardan en una sola transacción
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(INSERTAR)) {
                ps.setString(1, cliente.getDuiCliente());
                ps.setString(2, cliente.getNombreCompletoCliente());
                ps.setString(3, cliente.getCorreoCliente());
                ps.setString(4, cliente.getDireccionCliente());
                ps.setString(5, cliente.getEstadoCliente().name());
                boolean insertado = ps.executeUpdate() > 0;
                guardarTelefonos(con, cliente);
                con.commit();
                return insertado;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<Cliente> consultarTodos() throws SQLException {
        return consultar(CONSULTAR_TODOS);
    }

    public Cliente buscarPorDui(String dui) throws SQLException {
        List<Cliente> clientes = consultar(BUSCAR_POR_DUI, dui);
        return clientes.isEmpty() ? null : clientes.get(0);
    }

    public List<Cliente> buscarPorNombre(String nombre) throws SQLException {
        return consultar(BUSCAR_POR_NOMBRE, "%" + nombre.trim() + "%");
    }

    public boolean existeDui(String dui) throws SQLException {
        return buscarPorDui(dui) != null;
    }

    public boolean actualizar(Cliente cliente) throws SQLException {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(ACTUALIZAR);
                 PreparedStatement psTelefonos = con.prepareStatement(ELIMINAR_TELEFONOS)) {
                ps.setString(1, cliente.getNombreCompletoCliente());
                ps.setString(2, cliente.getCorreoCliente());
                ps.setString(3, cliente.getDireccionCliente());
                ps.setString(4, cliente.getEstadoCliente().name());
                ps.setString(5, cliente.getDuiCliente());
                boolean actualizado = ps.executeUpdate() > 0;
                if (actualizado) {
                    //Los teléfonos anteriores se reemplazan por los que trae el cliente
                    psTelefonos.setString(1, cliente.getDuiCliente());
                    psTelefonos.executeUpdate();
                    guardarTelefonos(con, cliente);
                }
                con.commit();
                return actualizado;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public boolean cambiarEstado(String dui, EstadoCliente estado) throws SQLException {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(CAMBIAR_ESTADO)) {
            ps.setString(1, estado.name());
            ps.setString(2, dui);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Cliente> consultar(String sql, String... parametros) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setString(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(new Cliente(
                            rs.getString("dui_cliente"),
                            rs.getString("nombre_completo_cliente"),
                            rs.getString("correo_cliente"),
                            rs.getString("direccion_cliente"),
                            EstadoCliente.valueOf(rs.getString("estado_cliente"))));
                }
            }
            cargarTelefonos(con, clientes);
        }
        return clientes;
    }

    private void cargarTelefonos(Connection con, List<Cliente> clientes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTAR_TELEFONOS)) {
            for (Cliente cliente : clientes) {
                ps.setString(1, cliente.getDuiCliente());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cliente.agregarTelefono(rs.getString("telefono_cliente"));
                    }
                }
            }
        }
    }

    private void guardarTelefonos(Connection con, Cliente cliente) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR_TELEFONO)) {
            for (String telefono : cliente.getTelefonos()) {
                ps.setString(1, cliente.getDuiCliente());
                ps.setString(2, telefono);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
