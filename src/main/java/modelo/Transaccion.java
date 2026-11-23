package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa una fila de la tabla transacciones.
 * - DEPOSITO: solo usa numeroCuentaDestino (numeroCuentaOrigen es null).
 * - RETIRO: solo usa numeroCuentaOrigen (numeroCuentaDestino es null).
 * - TRANSFERENCIA: usa ambas cuentas.
 */
public class Transaccion {
    private long idTransaccion;
    private TipoTransaccion tipoTransaccion;
    private BigDecimal monto;
    private LocalDateTime fechaHora;
    private String numeroCuentaOrigen;
    private String numeroCuentaDestino;
    private String descripcion;

    public Transaccion() { }

    /** Constructor completo, pensado para armar el objeto al leerlo de MySQL. */
    public Transaccion(long idTransaccion, TipoTransaccion tipoTransaccion, BigDecimal monto,
                       LocalDateTime fechaHora, String numeroCuentaOrigen,
                       String numeroCuentaDestino, String descripcion) {
        this.idTransaccion = idTransaccion;
        this.tipoTransaccion = tipoTransaccion;
        this.monto = monto;
        this.fechaHora = fechaHora;
        this.numeroCuentaOrigen = numeroCuentaOrigen;
        this.numeroCuentaDestino = numeroCuentaDestino;
        this.descripcion = descripcion;
    }

    /** Constructor para una transacción nueva: el id lo genera MySQL (AUTO_INCREMENT). */
    public Transaccion(TipoTransaccion tipoTransaccion, BigDecimal monto,
                       String numeroCuentaOrigen, String numeroCuentaDestino, String descripcion) {
        this(0, tipoTransaccion, monto, LocalDateTime.now(),
                numeroCuentaOrigen, numeroCuentaDestino, descripcion);
    }

    public long getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(long idTransaccion) { this.idTransaccion = idTransaccion; }
    public TipoTransaccion getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(TipoTransaccion tipoTransaccion) { this.tipoTransaccion = tipoTransaccion; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getNumeroCuentaOrigen() { return numeroCuentaOrigen; }
    public void setNumeroCuentaOrigen(String numeroCuentaOrigen) { this.numeroCuentaOrigen = numeroCuentaOrigen; }
    public String getNumeroCuentaDestino() { return numeroCuentaDestino; }
    public void setNumeroCuentaDestino(String numeroCuentaDestino) { this.numeroCuentaDestino = numeroCuentaDestino; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return "Transaccion{id=" + idTransaccion + ", tipo=" + tipoTransaccion + ", monto=" + monto
                + ", fechaHora=" + fechaHora + ", origen=" + numeroCuentaOrigen
                + ", destino=" + numeroCuentaDestino + ", descripcion=" + descripcion + "}";
    }
}
