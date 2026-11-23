package excepciones;

import java.math.BigDecimal;

/** Se lanza cuando el monto es nulo, cero o negativo. */
public class MontoInvalidoException extends Exception {
    private final BigDecimal monto;

    public MontoInvalidoException(BigDecimal monto) {
        super("El monto debe ser mayor que cero (valor recibido: " + monto + ").");
        this.monto = monto;
    }

    public BigDecimal getMonto() { return monto; }
}
