package demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import modelo.Cliente;
import modelo.Cuenta;
import modelo.EstadoCliente;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;
import validaciones.Validaciones;

/** Ejemplo ejecutable de creación de objetos y uso de Validaciones. */
public class DemoIntegrante3 {
    public static void main(String[] args) {
        Cliente cliente = new Cliente("01234567-8", "Carlos Hernandez", "carlos@gmail.com",
                "San Salvador", EstadoCliente.ACTIVO, Arrays.asList("70123456", "71123456"));
        Cuenta cuenta = new Cuenta("CTA-000001", cliente.getDuiCliente(), TipoCuenta.AHORRO,
                new BigDecimal("500.00"), LocalDate.of(2026, 11, 1), EstadoCuenta.ACTIVA);

        String errorCliente = Validaciones.validarCliente(cliente);
        String errorSaldo = Validaciones.validarSaldo(cuenta.getSaldo());
        System.out.println(errorCliente == null ? "Cliente válido: " + cliente.getNombreCompletoCliente() : errorCliente);
        System.out.println(errorSaldo == null ? "Cuenta de ejemplo: " + cuenta.getNumeroCuenta() : errorSaldo);
        System.out.println("¿Puede abrir otra cuenta según la regla propuesta? " + Validaciones.esClienteActivo(cliente));
    }
}
