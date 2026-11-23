package excepciones;

import java.math.BigDecimal;

/** Se lanza cuando el saldo de la cuenta no alcanza para cubrir un retiro o transferencia. */
public class SaldoInsuficienteException extends Exception {
    private final String numeroCuenta;
    private final BigDecimal saldoActual;
    private final BigDecimal montoSolicitado;

    public SaldoInsuficienteException(String numeroCuenta, BigDecimal saldoActual, BigDecimal montoSolicitado) {
        super("Saldo insuficiente en la cuenta " + numeroCuenta + ": saldo " + saldoActual
                + ", monto solicitado " + montoSolicitado + ".");
        this.numeroCuenta = numeroCuenta;
        this.saldoActual = saldoActual;
        this.montoSolicitado = montoSolicitado;
    }

    public String getNumeroCuenta() { return numeroCuenta; }
    public BigDecimal getSaldoActual() { return saldoActual; }
    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
}
