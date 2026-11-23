package pruebas;

import excepciones.CuentaInactivaException;
import excepciones.MontoInvalidoException;
import excepciones.SaldoInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import modelo.Cuenta;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import validaciones.TransaccionService;


public class PruebasReglasNegocio {
    private static int correctas = 0;
    private static int fallidas = 0;

    public static void main(String[] args) {
        Cuenta activa = cuenta("CTA-000001", "500.00", EstadoCuenta.ACTIVA);
        Cuenta inactiva = cuenta("CTA-000004", "300.00", EstadoCuenta.INACTIVA);
        Cuenta bloqueada = cuenta("CTA-000009", "300.00", EstadoCuenta.BLOQUEADA);

        // Cuenta activa / no activa (CP-03, CP-04, CP-10, CP-15, CP-16)
        esperarOk("Cuenta ACTIVA es válida", () -> TransaccionService.validarCuentaActiva(activa));
        esperarError("Cuenta INACTIVA se rechaza", CuentaInactivaException.class,
                () -> TransaccionService.validarCuentaActiva(inactiva));
        esperarError("Cuenta BLOQUEADA se rechaza", CuentaInactivaException.class,
                () -> TransaccionService.validarCuentaActiva(bloqueada));

        // Monto (CP-05, CP-06, CP-11, CP-18)
        esperarOk("Monto 0.01 es válido", () -> TransaccionService.validarMonto(new BigDecimal("0.01")));
        esperarError("Monto 0 se rechaza", MontoInvalidoException.class,
                () -> TransaccionService.validarMonto(BigDecimal.ZERO));
        esperarError("Monto negativo se rechaza", MontoInvalidoException.class,
                () -> TransaccionService.validarMonto(new BigDecimal("-10")));
        esperarError("Monto nulo se rechaza", MontoInvalidoException.class,
                () -> TransaccionService.validarMonto(null));

        // Saldo (CP-08, CP-17)
        esperarOk("Retirar menos que el saldo", () -> TransaccionService.validarSaldo(activa, new BigDecimal("499.99")));
        esperarOk("Retirar exactamente el saldo", () -> TransaccionService.validarSaldo(activa, new BigDecimal("500.00")));
        esperarError("Retirar más que el saldo se rechaza", SaldoInsuficienteException.class,
                () -> TransaccionService.validarSaldo(activa, new BigDecimal("500.01")));

        // Modelo Transaccion
        Transaccion t = new Transaccion(TipoTransaccion.TRANSFERENCIA, new BigDecimal("100.00"),
                "CTA-000001", "CTA-000002", "Transferencia de prueba");
        verificar("Transaccion nueva tiene id 0 y fecha", t.getIdTransaccion() == 0 && t.getFechaHora() != null);
        verificar("Transaccion guarda origen y destino",
                "CTA-000001".equals(t.getNumeroCuentaOrigen()) && "CTA-000002".equals(t.getNumeroCuentaDestino()));

        System.out.println("\nResultado: " + correctas + " correctas, " + fallidas + " fallidas.");
    }

    private static Cuenta cuenta(String numero, String saldo, EstadoCuenta estado) {
        return new Cuenta(numero, "01234567-8", TipoCuenta.AHORRO, new BigDecimal(saldo),
                LocalDate.of(2026, 11, 1), estado);
    }

    private interface Accion { void ejecutar() throws Exception; }

    private static void esperarOk(String nombre, Accion accion) {
        try {
            accion.ejecutar();
            verificar(nombre, true);
        } catch (Exception e) {
            verificar(nombre + " (lanzó " + e.getClass().getSimpleName() + ")", false);
        }
    }

    private static void esperarError(String nombre, Class<? extends Exception> esperada, Accion accion) {
        try {
            accion.ejecutar();
            verificar(nombre + " (no lanzó excepción)", false);
        } catch (Exception e) {
            verificar(nombre, esperada.isInstance(e));
        }
    }

    private static void verificar(String nombre, boolean ok) {
        System.out.println((ok ? "[OK]    " : "[FALLO] ") + nombre);
        if (ok) correctas++; else fallidas++;
    }
}
