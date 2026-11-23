package vista;

import dao.CuentaDAO;
import dao.TransaccionDAO;
import excepciones.CuentaInactivaException;
import excepciones.CuentaNoEncontradaException;
import excepciones.MontoInvalidoException;
import excepciones.SaldoInsuficienteException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Transaccion;
import validaciones.TransaccionService;

/** Interfaces Swing para depósitos, retiros, transferencias y consulta de movimientos. */
public class GestionTransacciones extends JPanel {
    private final TransaccionService servicio =
            new TransaccionService(new CuentaDAO(), new TransaccionDAO());

    private final JTextField cuentaDeposito = new JTextField(18);
    private final JTextField montoDeposito = new JTextField(12);
    private final JTextField descDeposito = new JTextField(22);
    private final JTextField cuentaRetiro = new JTextField(18);
    private final JTextField montoRetiro = new JTextField(12);
    private final JTextField descRetiro = new JTextField(22);
    private final JTextField origen = new JTextField(18);
    private final JTextField destino = new JTextField(18);
    private final JTextField montoTransferencia = new JTextField(12);
    private final JTextField descTransferencia = new JTextField(22);
    private final JTextField idMovimiento = new JTextField(10);
    private final JTextField cuentaFiltro = new JTextField(15);

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Tipo", "Monto", "Fecha y hora", "Cuenta origen", "Cuenta destino", "Descripción"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public GestionTransacciones() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JTabbedPane operaciones = new JTabbedPane();
        operaciones.addTab("Depósito", formularioDeposito());
        operaciones.addTab("Retiro", formularioRetiro());
        operaciones.addTab("Transferencia", formularioTransferencia());

        JPanel consultas = new JPanel(new BorderLayout(8, 8));
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("ID de movimiento:"));
        filtros.add(idMovimiento);
        JButton consultar = new JButton("Consultar movimiento");
        Estilos.boton(consultar);
        filtros.add(consultar);
        filtros.add(new JLabel("Cuenta:"));
        filtros.add(cuentaFiltro);
        JButton porCuenta = new JButton("Filtrar historial");
        JButton todo = new JButton("Ver historial completo");
        Estilos.boton(porCuenta);
        Estilos.boton(todo);
        filtros.add(porCuenta);
        filtros.add(todo);
        tabla.setRowHeight(24);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        consultas.add(filtros, BorderLayout.NORTH);
        consultas.add(new JScrollPane(tabla), BorderLayout.CENTER);

        operaciones.addTab("Movimientos", consultas);
        add(operaciones, BorderLayout.CENTER);

        consultar.addActionListener(e -> consultarMovimiento());
        porCuenta.addActionListener(e -> cargarPorCuenta());
        todo.addActionListener(e -> cargarHistorial());
        cargarHistorial();
    }

    private JPanel formularioDeposito() {
        JPanel panel = crearFormulario("Registrar depósito");
        GridBagConstraints g = restricciones();
        agregarCampo(panel, g, 0, "Número de cuenta:", cuentaDeposito);
        agregarCampo(panel, g, 1, "Monto ($):", montoDeposito);
        agregarCampo(panel, g, 2, "Descripción:", descDeposito);
        JButton ejecutar = new JButton("Realizar depósito");
        Estilos.boton(ejecutar);
        g.gridy = 3; g.gridx = 1;
        panel.add(ejecutar, g);
        ejecutar.addActionListener(e -> depositar());
        return panel;
    }

    private JPanel formularioRetiro() {
        JPanel panel = crearFormulario("Registrar retiro");
        GridBagConstraints g = restricciones();
        agregarCampo(panel, g, 0, "Número de cuenta:", cuentaRetiro);
        agregarCampo(panel, g, 1, "Monto ($):", montoRetiro);
        agregarCampo(panel, g, 2, "Descripción:", descRetiro);
        JButton ejecutar = new JButton("Realizar retiro");
        Estilos.boton(ejecutar);
        g.gridy = 3; g.gridx = 1;
        panel.add(ejecutar, g);
        ejecutar.addActionListener(e -> retirar());
        return panel;
    }

    private JPanel formularioTransferencia() {
        JPanel panel = crearFormulario("Transferir entre cuentas");
        GridBagConstraints g = restricciones();
        agregarCampo(panel, g, 0, "Cuenta de origen:", origen);
        agregarCampo(panel, g, 1, "Cuenta de destino:", destino);
        agregarCampo(panel, g, 2, "Monto ($):", montoTransferencia);
        agregarCampo(panel, g, 3, "Descripción:", descTransferencia);
        JButton ejecutar = new JButton("Realizar transferencia");
        Estilos.boton(ejecutar);
        g.gridy = 4; g.gridx = 1;
        panel.add(ejecutar, g);
        ejecutar.addActionListener(e -> transferir());
        return panel;
    }

    private JPanel crearFormulario(String titulo) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        return p;
    }

    private GridBagConstraints restricciones() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        return g;
    }

    private void agregarCampo(JPanel p, GridBagConstraints g, int fila, String etiqueta, JTextField campo) {
        g.gridy = fila;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(etiqueta), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(campo, g);
    }

    private BigDecimal leerMonto(JTextField campo) {
        String texto = campo.getText().trim().replace(',', '.');
        if (texto.isEmpty()) throw new NumberFormatException();
        BigDecimal valor = new BigDecimal(texto);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El monto no puede tener más de dos decimales.");
        }
        return valor;
    }

    private void depositar() {
        try {
            servicio.depositar(cuentaDeposito.getText().trim(), leerMonto(montoDeposito),
                    textoODefecto(descDeposito, "Depósito desde interfaz"));
            exito("Depósito realizado correctamente.");
            limpiarOperacion(cuentaDeposito, montoDeposito, descDeposito);
            cargarHistorial();
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void retirar() {
        try {
            servicio.retirar(cuentaRetiro.getText().trim(), leerMonto(montoRetiro),
                    textoODefecto(descRetiro, "Retiro desde interfaz"));
            exito("Retiro realizado correctamente.");
            limpiarOperacion(cuentaRetiro, montoRetiro, descRetiro);
            cargarHistorial();
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void transferir() {
        try {
            String cuentaA = origen.getText().trim();
            String cuentaB = destino.getText().trim();
            if (cuentaA.equalsIgnoreCase(cuentaB)) {
                throw new IllegalArgumentException("La cuenta de origen y destino deben ser diferentes.");
            }
            servicio.transferir(cuentaA, cuentaB, leerMonto(montoTransferencia),
                    textoODefecto(descTransferencia, "Transferencia desde interfaz"));
            exito("Transferencia realizada correctamente.");
            limpiarOperacion(origen, montoTransferencia, descTransferencia);
            destino.setText("");
            cargarHistorial();
        } catch (Exception ex) { mostrarError(ex); }
    }

    private String textoODefecto(JTextField campo, String defecto) {
        String texto = campo.getText().trim();
        return texto.isEmpty() ? defecto : texto;
    }

    private void consultarMovimiento() {
        try {
            String texto = idMovimiento.getText().trim();
            if (texto.isEmpty()) throw new IllegalArgumentException("Ingrese el ID del movimiento.");
            long id = Long.parseLong(texto);
            Transaccion t = new TransaccionDAO().buscarPorId(id);
            modelo.setRowCount(0);
            if (t == null) {
                aviso("No se encontró un movimiento con ese ID.", JOptionPane.INFORMATION_MESSAGE);
            } else agregarFila(t);
        } catch (NumberFormatException ex) {
            aviso("El ID debe ser un número entero válido.", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void cargarPorCuenta() {
        try {
            String cuenta = cuentaFiltro.getText().trim();
            if (cuenta.isEmpty()) throw new IllegalArgumentException("Ingrese el número de cuenta.");
            List<Transaccion> lista = new TransaccionDAO().listarHistorialPorCuenta(cuenta);
            mostrarLista(lista);
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void cargarHistorial() {
        try { mostrarLista(new TransaccionDAO().listarHistorial()); }
        catch (SQLException ex) { mostrarError(ex); }
    }

    private void mostrarLista(List<Transaccion> lista) {
        modelo.setRowCount(0);
        for (Transaccion t : lista) agregarFila(t);
    }

    private void agregarFila(Transaccion t) {
        modelo.addRow(new Object[]{t.getIdTransaccion(), t.getTipoTransaccion(), t.getMonto(),
                t.getFechaHora(), t.getNumeroCuentaOrigen(), t.getNumeroCuentaDestino(), t.getDescripcion()});
    }

    private void limpiarOperacion(JTextField cuenta, JTextField monto, JTextField descripcion) {
        cuenta.setText(""); monto.setText(""); descripcion.setText("");
    }

    private void mostrarError(Exception ex) {
        String mensaje;
        if (ex instanceof NumberFormatException) {
            mensaje = "Ingrese un monto numérico válido.";
        } else if (ex instanceof CuentaNoEncontradaException || ex instanceof CuentaInactivaException
                || ex instanceof MontoInvalidoException || ex instanceof SaldoInsuficienteException
                || ex instanceof IllegalArgumentException) {
            mensaje = ex.getMessage();
        } else if (ex instanceof SQLException) {
            mensaje = "No se pudo completar la operación. Verifique la conexión y los datos.\nDetalle: " + ex.getMessage();
        } else {
            mensaje = ex.getMessage() == null ? "Ocurrió un error inesperado." : ex.getMessage();
        }
        aviso(mensaje, JOptionPane.WARNING_MESSAGE);
    }

    private void exito(String mensaje) { aviso(mensaje, JOptionPane.INFORMATION_MESSAGE); }
    private void aviso(String mensaje, int tipo) {
        JOptionPane.showMessageDialog(this, mensaje, "Sistema Bancario", tipo);
    }
}
