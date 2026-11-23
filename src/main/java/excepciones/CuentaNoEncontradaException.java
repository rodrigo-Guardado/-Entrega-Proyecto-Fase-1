package excepciones;

/** Se lanza cuando el número de cuenta indicado no existe en la tabla cuentas. */
public class CuentaNoEncontradaException extends Exception {
    private final String numeroCuenta;

    public CuentaNoEncontradaException(String numeroCuenta) {
        super("La cuenta " + numeroCuenta + " no existe.");
        this.numeroCuenta = numeroCuenta;
    }

    public String getNumeroCuenta() { return numeroCuenta; }
}
