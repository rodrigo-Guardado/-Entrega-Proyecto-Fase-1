package vista;

import dao.ClienteDAO;
import dao.CuentaDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;
import modelo.Cuenta;
import modelo.EstadoCliente;
import modelo.EstadoCuenta;
import modelo.TipoCuenta;
import validaciones.Validaciones;

public class GestionCuentas extends JPanel {
    private final ClienteDAO clienteDAO;
    private final CuentaDAO cuentaDAO;
    private final JTextField numero = new JTextField(15);
    private final JTextField duiCliente = new JTextField(13);
    private final JComboBox<TipoCuenta> tipo = new JComboBox<>(TipoCuenta.values());
    private final JTextField saldo = new JTextField(12);
    private final JTextField busqueda = new JTextField(18);

    private final DefaultTableModel modelo = new DefaultTableModel(
        new Object[]{"Número de cuenta", "DUI del cliente", "Tipo", "Saldo", "Fecha de apertura", "Estado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public GestionCuentas(ClienteDAO clienteDAO, CuentaDAO cuentaDAO) {
        this.clienteDAO = clienteDAO;
        this.cuentaDAO = cuentaDAO;
        construirInterfaz();
        cargarTodas();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Registro de cuenta bancaria"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 7, 6, 7);
        g.fill = GridBagConstraints.HORIZONTAL;

        numero.setEditable(false);
        campo(formulario, g, 0, 0, "Número de cuenta:", numero);
        campo(formulario, g, 2, 0, "DUI del cliente:", duiCliente);
        campo(formulario, g, 0, 1, "Tipo de cuenta:", tipo);
        campo(formulario, g, 2, 1, "Saldo inicial:", saldo);

        JButton generar = new JButton("Generar número");
        JButton registrar = new JButton("Registrar cuenta");
        JButton limpiar = new JButton("Limpiar");
        Estilos.boton(generar); Estilos.boton(registrar); Estilos.boton(limpiar);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(generar); acciones.add(registrar); acciones.add(limpiar);

        JPanel arriba = new JPanel(new BorderLayout(5, 5));
        arriba.add(formulario, BorderLayout.CENTER);
        arriba.add(acciones, BorderLayout.SOUTH);
        add(arriba, BorderLayout.NORTH);

        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busquedaPanel.add(new JLabel("Buscar por número de cuenta o DUI:"));
        busquedaPanel.add(busqueda);
        JButton buscar = new JButton("Buscar");
        JButton todas = new JButton("Mostrar todas");
        Estilos.boton(buscar); Estilos.boton(todas);
        busquedaPanel.add(buscar); busquedaPanel.add(todas);

        tabla.setRowHeight(25);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(busquedaPanel, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        generar.addActionListener(e -> generarNumero());
        registrar.addActionListener(e -> registrar());
        limpiar.addActionListener(e -> limpiar());
        buscar.addActionListener(e -> buscar());
        todas.addActionListener(e -> cargarTodas());
    }

    private void campo(JPanel p, GridBagConstraints g, int x, int y,
                       String texto, java.awt.Component componente) {
        g.gridx = x; g.gridy = y; g.weightx = 0;
        p.add(new JLabel(texto), g);
        g.gridx = x + 1; g.weightx = 1;
        p.add(componente, g);
    }

    private void generarNumero() {
        try {
            numero.setText(cuentaDAO.generarNumeroCuenta());
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void registrar() {
        String dui = duiCliente.getText().trim();
        if (!Validaciones.esDuiValido(dui)) {
            aviso("Ingrese un DUI válido con formato 00000000-0.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (numero.getText().trim().isEmpty()) {
            aviso("Genere primero el número de cuenta.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(saldo.getText().trim());
        } catch (NumberFormatException ex) {
            aviso("Ingrese un saldo numérico válido.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String errorSaldo = Validaciones.validarSaldo(monto);
        if (errorSaldo != null) {
            aviso(errorSaldo, JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Cliente cliente = clienteDAO.buscarPorDui(dui);
            if (cliente == null) {
                aviso("No existe un cliente registrado con ese DUI.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Validaciones.esClienteActivo(cliente)) {
                aviso("Solo los clientes con estado ACTIVO pueden abrir cuentas.",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (cuentaDAO.existeNumeroCuenta(numero.getText().trim())) {
                aviso("El número de cuenta ya está registrado. Genere otro.", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Cuenta cuenta = new Cuenta(numero.getText().trim(), dui,
                    (TipoCuenta) tipo.getSelectedItem(), monto,
                    LocalDate.now(), EstadoCuenta.ACTIVA);
            if (cuentaDAO.insertar(cuenta)) {
                aviso("Cuenta registrada correctamente.", JOptionPane.INFORMATION_MESSAGE);
                cargarTodas();
                limpiar();
            } else {
                aviso("No fue posible registrar la cuenta.", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void buscar() {
        String criterio = busqueda.getText().trim();
        if (criterio.isEmpty()) {
            cargarTodas();
            return;
        }
        try {
            List<Cuenta> cuentas;
            if (criterio.toUpperCase().startsWith("CTA-")) {
                Cuenta c = cuentaDAO.buscarPorNumero(criterio);
                cuentas = new ArrayList<>();
                if (c != null) cuentas.add(c);
            } else if (Validaciones.esDuiValido(criterio)) {
                cuentas = cuentaDAO.buscarPorCliente(criterio);
            } else {
                aviso("Ingrese un número de cuenta (CTA-...) o un DUI válido.",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            mostrar(cuentas);
            if (cuentas.isEmpty()) aviso("No se encontraron cuentas.", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void cargarTodas() {
        try {
            mostrar(cuentaDAO.consultarTodas());
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void mostrar(List<Cuenta> cuentas) {
        modelo.setRowCount(0);
        for (Cuenta c : cuentas) {
            modelo.addRow(new Object[]{
                c.getNumeroCuenta(), c.getDuiCliente(), c.getTipoCuenta(),
                c.getSaldo(), c.getFechaApertura(), c.getEstadoCuenta()
            });
        }
    }

    private void limpiar() {
        numero.setText("");
        duiCliente.setText("");
        tipo.setSelectedItem(TipoCuenta.AHORRO);
        saldo.setText("");
    }

    private void aviso(String texto, int tipoMensaje) {
        JOptionPane.showMessageDialog(this, texto, "Gestión de cuentas", tipoMensaje);
    }

    private void errorBD(SQLException ex) {
        aviso("Ocurrió un error al trabajar con la base de datos:\n" + ex.getMessage(),
                JOptionPane.ERROR_MESSAGE);
    }
}
