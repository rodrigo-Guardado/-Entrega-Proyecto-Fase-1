package vista;

import dao.ReporteDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;
import modelo.Cuenta;
import modelo.Transaccion;
import modelo.TipoTransaccion;

/** Módulo visual de los ocho reportes solicitados en la Fase 2. */
public class GestionReportes extends JPanel {
    private enum TipoReporte {
        CUENTAS_ACTIVAS("Cuentas activas"),
        CUENTAS_BLOQUEADAS("Cuentas bloqueadas"),
        CLIENTES("Clientes registrados"),
        HISTORIAL("Historial de transacciones"),
        POR_CUENTA("Transacciones por cuenta"),
        TOTAL_DEPOSITADO("Total depositado"),
        TOTAL_RETIRADO("Total retirado"),
        TOTAL_TRANSFERIDO("Total transferido");

        private final String etiqueta;
        TipoReporte(String etiqueta) { this.etiqueta = etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final JComboBox<TipoReporte> tipo = new JComboBox<>(TipoReporte.values());
    private final JTextField cuenta = new JTextField(16);
    private final JLabel resumen = new JLabel("Seleccione un reporte y presione «Generar reporte».");
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public GestionReportes() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Reporte:"));
        filtros.add(tipo);
        filtros.add(new JLabel("Número de cuenta (para reporte por cuenta):"));
        filtros.add(cuenta);
        JButton generar = new JButton("Generar reporte");
        JButton limpiar = new JButton("Limpiar");
        Estilos.boton(generar);
        Estilos.boton(limpiar);
        filtros.add(generar);
        filtros.add(limpiar);

        tabla.setRowHeight(24);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        inferior.add(resumen, BorderLayout.WEST);

        add(filtros, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(inferior, BorderLayout.SOUTH);

        generar.addActionListener(e -> generarReporte());
        limpiar.addActionListener(e -> {
            cuenta.setText("");
            modelo.setRowCount(0);
            modelo.setColumnCount(0);
            resumen.setText("Seleccione un reporte y presione «Generar reporte».");
        });
        tipo.addActionListener(e -> cuenta.setEnabled(tipo.getSelectedItem() == TipoReporte.POR_CUENTA));
        cuenta.setEnabled(false);
    }

    private void generarReporte() {
        try {
            TipoReporte seleccionado = (TipoReporte) tipo.getSelectedItem();
            switch (seleccionado) {
                case CUENTAS_ACTIVAS -> cargarCuentas(reporteDAO.cuentasActivas(), "Cuentas activas");
                case CUENTAS_BLOQUEADAS -> cargarCuentas(reporteDAO.cuentasBloqueadas(), "Cuentas bloqueadas");
                case CLIENTES -> cargarClientes(reporteDAO.clientesRegistrados());
                case HISTORIAL -> cargarTransacciones(reporteDAO.historial(), "Historial de transacciones");
                case POR_CUENTA -> {
                    if (cuenta.getText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Ingrese el número de cuenta para consultar sus movimientos.");
                    }
                    cargarTransacciones(reporteDAO.transaccionesPorCuenta(cuenta.getText().trim()),
                            "Transacciones de la cuenta " + cuenta.getText().trim());
                }
                case TOTAL_DEPOSITADO -> cargarTotal(reporteDAO.totalDepositado(), "Total depositado");
                case TOTAL_RETIRADO -> cargarTotal(reporteDAO.totalRetirado(), "Total retirado");
                case TOTAL_TRANSFERIDO -> cargarTotal(reporteDAO.totalTransferido(), "Total transferido");
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No fue posible generar el reporte. Verifique MySQL.\nDetalle: " + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarCuentas(List<Cuenta> lista, String titulo) {
        columnas("Número de cuenta", "DUI cliente", "Tipo", "Saldo", "Fecha apertura", "Estado");
        for (Cuenta c : lista) fila(c.getNumeroCuenta(), c.getDuiCliente(), c.getTipoCuenta(),
                c.getSaldo(), c.getFechaApertura(), c.getEstadoCuenta());
        resumen.setText(titulo + ": " + lista.size() + " registro(s).");
    }

    private void cargarClientes(List<Cliente> lista) {
        columnas("DUI", "Nombre completo", "Correo", "Dirección", "Estado");
        for (Cliente c : lista) fila(c.getDuiCliente(), c.getNombreCompletoCliente(),
                c.getCorreoCliente(), c.getDireccionCliente(), c.getEstadoCliente());
        resumen.setText("Clientes registrados: " + lista.size() + " registro(s).");
    }

    private void cargarTransacciones(List<Transaccion> lista, String titulo) {
        columnas("ID", "Tipo", "Monto", "Fecha y hora", "Cuenta origen", "Cuenta destino", "Descripción");
        for (Transaccion t : lista) fila(t.getIdTransaccion(), t.getTipoTransaccion(), t.getMonto(),
                t.getFechaHora(), t.getNumeroCuentaOrigen(), t.getNumeroCuentaDestino(), t.getDescripcion());
        resumen.setText(titulo + ": " + lista.size() + " movimiento(s).");
    }

    private void cargarTotal(BigDecimal total, String titulo) {
        columnas("Concepto", "Total ($)");
        fila(titulo, total);
        resumen.setText(titulo + ": $" + total);
    }

    private void columnas(String... nombres) {
        modelo.setRowCount(0);
        modelo.setColumnCount(0);
        for (String nombre : nombres) modelo.addColumn(nombre);
    }

    private void fila(Object... valores) { modelo.addRow(valores); }
}
