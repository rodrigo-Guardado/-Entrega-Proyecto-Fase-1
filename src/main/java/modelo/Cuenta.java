package modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representa una fila de cuentas; duiCliente referencia clientes.dui_cliente. */
public class Cuenta {
    private String numeroCuenta;
    private String duiCliente;
    private TipoCuenta tipoCuenta;
    private BigDecimal saldo;
    private LocalDate fechaApertura;
    private EstadoCuenta estadoCuenta;

    public Cuenta() { }

    public Cuenta(String numeroCuenta, String duiCliente, TipoCuenta tipoCuenta,
                  BigDecimal saldo, LocalDate fechaApertura, EstadoCuenta estadoCuenta) {
        this.numeroCuenta = numeroCuenta;
        this.duiCliente = duiCliente;
        this.tipoCuenta = tipoCuenta;
        this.saldo = saldo;
        this.fechaApertura = fechaApertura;
        this.estadoCuenta = estadoCuenta;
    }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
    public String getDuiCliente() { return duiCliente; }
    public void setDuiCliente(String duiCliente) { this.duiCliente = duiCliente; }
    public TipoCuenta getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(TipoCuenta tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public LocalDate getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDate fechaApertura) { this.fechaApertura = fechaApertura; }
    public EstadoCuenta getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(EstadoCuenta estadoCuenta) { this.estadoCuenta = estadoCuenta; }
}
