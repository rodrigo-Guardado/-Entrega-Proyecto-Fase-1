package validaciones;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import modelo.Cliente;
import modelo.EstadoCliente;

/** Reglas reutilizables para validar datos antes de enviarlos al DAO. */
public final class Validaciones {
    private static final Pattern DUI = Pattern.compile("^\\d{8}-\\d$");
    private static final Pattern TELEFONO = Pattern.compile("^\\d{8}$");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private Validaciones() { }

    public static boolean esTextoNoVacio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
    public static boolean esDuiValido(String dui) {
        return dui != null && DUI.matcher(dui.trim()).matches();
    }
    public static boolean esTelefonoValido(String telefono) {
        return telefono != null && TELEFONO.matcher(telefono.trim()).matches();
    }
    public static boolean esCorreoValido(String correo) {
        return correo != null && CORREO.matcher(correo.trim()).matches();
    }
    public static boolean esSaldoValido(BigDecimal saldo) {
        return saldo != null && saldo.compareTo(BigDecimal.ZERO) >= 0;
    }
    public static boolean esClienteActivo(Cliente cliente) {
        return cliente != null && cliente.getEstadoCliente() == EstadoCliente.ACTIVO;
    }

    /** Devuelve el primer problema encontrado o null si los campos del cliente son válidos. */
    public static String validarCliente(Cliente cliente) {
        if (cliente == null) return "El cliente es obligatorio.";
        if (!esDuiValido(cliente.getDuiCliente())) return "El DUI debe tener formato 00000000-0.";
        if (!esTextoNoVacio(cliente.getNombreCompletoCliente())) return "El nombre completo es obligatorio.";
        if (!esCorreoValido(cliente.getCorreoCliente())) return "El correo electrónico no tiene un formato válido.";
        if (!esTextoNoVacio(cliente.getDireccionCliente())) return "La dirección es obligatoria.";
        if (cliente.getEstadoCliente() == null) return "El estado del cliente es obligatorio.";
        for (String telefono : cliente.getTelefonos()) {
            if (!esTelefonoValido(telefono)) return "Cada teléfono debe tener 8 dígitos.";
        }
        return null;
    }

    /** Valida el saldo inicial; otras reglas de Cuenta corresponden también al DAO/flujo de negocio. */
    public static String validarSaldo(BigDecimal saldo) {
        return esSaldoValido(saldo) ? null : "El saldo debe ser un valor mayor o igual a cero.";
    }
}
