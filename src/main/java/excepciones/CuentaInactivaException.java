package excepciones;

import modelo.EstadoCuenta;

/** Se lanza cuando la cuenta existe pero su estado no es ACTIVA (INACTIVA o BLOQUEADA). */
public class CuentaInactivaException extends Exception {
    private final String numeroCuenta;
    private final EstadoCuenta estado;

    public CuentaInactivaException(String numeroCuenta, EstadoCuenta estado) {
        super("La cuenta " + numeroCuenta + " no está activa (estado: " + estado + ").");
        this.numeroCuenta = numeroCuenta;
        this.estado = estado;
    }

    public String getNumeroCuenta() { return numeroCuenta; }
    public EstadoCuenta getEstado() { return estado; }
}
