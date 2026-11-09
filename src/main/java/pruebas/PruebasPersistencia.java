package pruebas;

import conexion.ConexionBD;
import dao.ClienteDAO;
import dao.CuentaDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.Arrays;
import modelo.Cliente;
import modelo.Cuenta;
import modelo.EstadoCliente;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;

public class PruebasPersistencia {

    private static final String DUI_PRUEBA = "99999999-9";
    private static final String DUI_INEXISTENTE = "00000000-0";
    private static final ClienteDAO clienteDAO = new ClienteDAO();
    private static final CuentaDAO cuentaDAO = new CuentaDAO();
    private static int correctas;
    private static int total;

    public static void main(String[] args) {
        System.out.println("PRUEBAS DE PERSISTENCIA");
        try {
            probarConexion();
            limpiarDatosDePrueba();
            probarClienteDAO();
            probarCuentaDAO();
            limpiarDatosDePrueba();
            mostrarDatos();
            System.out.println("\nResultado: " + correctas + " de " + total + " pruebas correctas.");
        } catch (SQLException e) {
            System.out.println("\nLas pruebas se detuvieron por un error de base de datos: " + e.getMessage());
        }
    }

    private static void probarConexion() throws SQLException {
        System.out.println("\nConexionBD");
        Connection conexion = ConexionBD.getConexion();
        verificar("getConexion se conecta a la base " + conexion.getCatalog(), conexion.isValid(2));
        ConexionBD.cerrar(conexion);
        verificar("cerrar libera la conexión", conexion.isClosed());
    }

    private static void probarClienteDAO() throws SQLException {
        System.out.println("\nClienteDAO");
        Cliente cliente = new Cliente(DUI_PRUEBA, "Cliente Prueba DAO", "prueba@gmail.com", "La Libertad",
                EstadoCliente.ACTIVO, Arrays.asList("77777777", "78888888"));

        verificar("existeDui devuelve false antes de registrar", !clienteDAO.existeDui(DUI_PRUEBA));
        verificar("insertar guarda el cliente con sus teléfonos", clienteDAO.insertar(cliente));

        Cliente guardado = clienteDAO.buscarPorDui(DUI_PRUEBA);
        verificar("buscarPorDui recupera los datos y los 2 teléfonos", guardado != null
                && guardado.getNombreCompletoCliente().equals(cliente.getNombreCompletoCliente())
                && guardado.getCorreoCliente().equals(cliente.getCorreoCliente())
                && guardado.getTelefonos().equals(cliente.getTelefonos()));
        verificar("existeDui devuelve true después de registrar", clienteDAO.existeDui(DUI_PRUEBA));
        verificar("insertar rechaza un DUI repetido", esRechazada(() -> clienteDAO.insertar(cliente)));
        verificar("consultarTodos incluye al cliente nuevo", clienteDAO.consultarTodos().stream()
                .anyMatch(c -> c.getDuiCliente().equals(DUI_PRUEBA)));
        verificar("buscarPorNombre encuentra coincidencias parciales",
                clienteDAO.buscarPorNombre("prueba dao").size() == 1);

        cliente.setDireccionCliente("Santa Tecla");
        cliente.setTelefonos(Arrays.asList("79999999"));
        verificar("actualizar guarda los cambios", clienteDAO.actualizar(cliente));
        guardado = clienteDAO.buscarPorDui(DUI_PRUEBA);
        verificar("la dirección y los teléfonos quedaron actualizados",
                guardado.getDireccionCliente().equals("Santa Tecla")
                && guardado.getTelefonos().equals(cliente.getTelefonos()));

        verificar("cambiarEstado pasa el cliente a BLOQUEADO",
                clienteDAO.cambiarEstado(DUI_PRUEBA, EstadoCliente.BLOQUEADO)
                && clienteDAO.buscarPorDui(DUI_PRUEBA).getEstadoCliente() == EstadoCliente.BLOQUEADO);

        Cliente inexistente = new Cliente(DUI_INEXISTENTE, "No Existe", "no@existe.com", "Ninguna",
                EstadoCliente.ACTIVO);
        verificar("actualizar y cambiarEstado devuelven false si el DUI no existe",
                !clienteDAO.actualizar(inexistente)
                && !clienteDAO.cambiarEstado(DUI_INEXISTENTE, EstadoCliente.INACTIVO));
    }

    private static void probarCuentaDAO() throws SQLException {
        System.out.println("\nCuentaDAO");
        String numero = cuentaDAO.generarNumeroCuenta();
        verificar("generarNumeroCuenta propone un número libre (" + numero + ")",
                !cuentaDAO.existeNumeroCuenta(numero));

        Cuenta cuenta = new Cuenta(numero, DUI_PRUEBA, TipoCuenta.AHORRO, new BigDecimal("250.75"),
                LocalDate.now(), EstadoCuenta.ACTIVA);
        verificar("insertar guarda la cuenta", cuentaDAO.insertar(cuenta));

        Cuenta guardada = cuentaDAO.buscarPorNumero(numero);
        verificar("buscarPorNumero recupera tipo, saldo, fecha y estado", guardada != null
                && guardada.getTipoCuenta() == cuenta.getTipoCuenta()
                && guardada.getSaldo().compareTo(cuenta.getSaldo()) == 0
                && guardada.getFechaApertura().equals(cuenta.getFechaApertura())
                && guardada.getEstadoCuenta() == cuenta.getEstadoCuenta());
        verificar("existeNumeroCuenta devuelve true después de registrar", cuentaDAO.existeNumeroCuenta(numero));
        verificar("buscarPorCliente devuelve la cuenta del cliente",
                cuentaDAO.buscarPorCliente(DUI_PRUEBA).size() == 1);
        verificar("consultarTodas incluye la cuenta nueva", cuentaDAO.consultarTodas().stream()
                .anyMatch(c -> c.getNumeroCuenta().equals(numero)));
        verificar("insertar rechaza un número de cuenta repetido", esRechazada(() -> cuentaDAO.insertar(cuenta)));

        Cuenta sinCliente = new Cuenta(cuentaDAO.generarNumeroCuenta(), DUI_INEXISTENTE, TipoCuenta.CORRIENTE,
                BigDecimal.ZERO, LocalDate.now(), EstadoCuenta.ACTIVA);
        verificar("insertar rechaza una cuenta de un cliente que no existe",
                esRechazada(() -> cuentaDAO.insertar(sinCliente)));
    }

    private static void mostrarDatos() throws SQLException {
        System.out.println("\nClientes en la base de datos");
        for (Cliente c : clienteDAO.consultarTodos()) {
            System.out.printf("%-11s %-18s %-10s %s%n", c.getDuiCliente(), c.getNombreCompletoCliente(),
                    c.getEstadoCliente(), c.getTelefonos());
        }
        System.out.println("\nCuentas en la base de datos");
        for (Cuenta c : cuentaDAO.consultarTodas()) {
            System.out.printf("%-11s %-11s %-10s %10s  %s  %s%n", c.getNumeroCuenta(), c.getDuiCliente(),
                    c.getTipoCuenta(), c.getSaldo(), c.getFechaApertura(), c.getEstadoCuenta());
        }
    }

    //Borra lo que crean las pruebas para poder ejecutarlas varias veces
    private static void limpiarDatosDePrueba() throws SQLException {
        String[] sentencias = {
            "DELETE FROM cuentas WHERE dui_cliente = ?",
            "DELETE FROM telefonos_clientes WHERE dui_cliente = ?",
            "DELETE FROM clientes WHERE dui_cliente = ?"
        };
        try (Connection con = ConexionBD.getConexion()) {
            for (String sql : sentencias) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, DUI_PRUEBA);
                    ps.executeUpdate();
                }
            }
        }
    }

    //Devuelve true si MySQL rechaza la operación por llave primaria o foránea
    private static boolean esRechazada(OperacionBD operacion) throws SQLException {
        try {
            operacion.ejecutar();
            return false;
        } catch (SQLIntegrityConstraintViolationException e) {
            return true;
        }
    }

    private static void verificar(String descripcion, boolean resultado) {
        total++;
        if (resultado) {
            correctas++;
        }
        System.out.println((resultado ? "[OK]    " : "[FALLO] ") + descripcion);
    }

    private interface OperacionBD {
        void ejecutar() throws SQLException;
    }
}
