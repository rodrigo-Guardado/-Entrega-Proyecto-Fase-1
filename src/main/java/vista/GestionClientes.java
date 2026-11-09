package vista;

import dao.ClienteDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import modelo.EstadoCliente;
import validaciones.Validaciones;

public class GestionClientes extends JPanel {
    private final ClienteDAO clienteDAO;
    private final JTextField dui = new JTextField(13);
    private final JTextField nombre = new JTextField(22);
    private final JTextField correo = new JTextField(22);
    private final JTextField direccion = new JTextField(22);
    private final JTextField telefonos = new JTextField(22);
    private final JComboBox<EstadoCliente> estado = new JComboBox<>(EstadoCliente.values());
    private final JTextField busqueda = new JTextField(20);

    private final DefaultTableModel modelo = new DefaultTableModel(
        new Object[]{"DUI", "Nombre completo", "Teléfonos", "Correo", "Dirección", "Estado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public GestionClientes(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
        construirInterfaz();
        cargarTodos();
    }

    private void construirInterfaz() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Información del cliente"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 7, 6, 7);
        g.fill = GridBagConstraints.HORIZONTAL;

        agregarCampo(formulario, g, 0, 0, "DUI:", dui);
        agregarCampo(formulario, g, 2, 0, "Nombre completo:", nombre);
        agregarCampo(formulario, g, 0, 1, "Teléfono(s):", telefonos);
        agregarCampo(formulario, g, 2, 1, "Correo electrónico:", correo);
        agregarCampo(formulario, g, 0, 2, "Dirección:", direccion);
        agregarCampo(formulario, g, 2, 2, "Estado:", estado);

        JLabel ayuda = new JLabel("Para varios teléfonos, sepárelos con coma. Ejemplo: 70123456, 71123456");
        ayuda.setFont(ayuda.getFont().deriveFont(11f));
        g.gridx = 0; g.gridy = 3; g.gridwidth = 4;
        formulario.add(ayuda, g);

        JButton registrar = new JButton("Registrar cliente");
        JButton actualizar = new JButton("Guardar cambios");
        JButton cambiarEstado = new JButton("Cambiar estado");
        JButton limpiar = new JButton("Limpiar");
        Estilos.boton(registrar); Estilos.boton(actualizar);
        Estilos.boton(cambiarEstado); Estilos.boton(limpiar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(registrar); acciones.add(actualizar);
        acciones.add(cambiarEstado); acciones.add(limpiar);

        JPanel superior = new JPanel(new BorderLayout(5, 5));
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(acciones, BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);

        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busquedaPanel.add(new JLabel("Buscar por DUI o nombre:"));
        busquedaPanel.add(busqueda);
        JButton buscar = new JButton("Buscar");
        JButton todos = new JButton("Mostrar todos");
        Estilos.boton(buscar); Estilos.boton(todos);
        busquedaPanel.add(buscar); busquedaPanel.add(todos);

        tabla.setRowHeight(25);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        JPanel centro = new JPanel(new BorderLayout(6, 6));
        centro.add(busquedaPanel, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        registrar.addActionListener(e -> registrar());
        actualizar.addActionListener(e -> actualizar());
        cambiarEstado.addActionListener(e -> cambiarEstado());
        limpiar.addActionListener(e -> limpiar());
        buscar.addActionListener(e -> buscar());
        todos.addActionListener(e -> cargarTodos());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) cargarSeleccion();
        });
    }

    private void agregarCampo(JPanel p, GridBagConstraints g, int x, int y,
                              String texto, java.awt.Component componente) {
        g.gridx = x; g.gridy = y; g.gridwidth = 1; g.weightx = 0;
        p.add(new JLabel(texto), g);
        g.gridx = x + 1; g.weightx = 1;
        p.add(componente, g);
    }

    private List<String> leerTelefonos() {
        List<String> lista = new ArrayList<>();
        String contenido = telefonos.getText().trim();
        if (!contenido.isEmpty()) {
            for (String t : contenido.split(",")) {
                if (!t.trim().isEmpty()) lista.add(t.trim());
            }
        }
        return lista;
    }

    private Cliente leerFormulario() {
        return new Cliente(dui.getText().trim(), nombre.getText().trim(),
                correo.getText().trim(), direccion.getText().trim(),
                (EstadoCliente) estado.getSelectedItem(), leerTelefonos());
    }

    private boolean validarFormulario() {
        Cliente cliente = leerFormulario();
        String error = Validaciones.validarCliente(cliente);
        if (error != null) {
            mensaje(error, JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (cliente.getTelefonos().isEmpty()) {
            mensaje("Ingrese al menos un teléfono de contacto.", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void registrar() {
        if (!validarFormulario()) return;
        try {
            if (clienteDAO.existeDui(dui.getText().trim())) {
                mensaje("Ya existe un cliente registrado con ese DUI.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (clienteDAO.insertar(leerFormulario())) {
                mensaje("Cliente registrado correctamente.", JOptionPane.INFORMATION_MESSAGE);
                cargarTodos();
                limpiar();
            } else {
                mensaje("No fue posible registrar el cliente.", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void actualizar() {
        if (!validarFormulario()) return;
        try {
            if (clienteDAO.actualizar(leerFormulario())) {
                mensaje("Información del cliente actualizada.", JOptionPane.INFORMATION_MESSAGE);
                cargarTodos();
            } else {
                mensaje("No se encontró un cliente con ese DUI.", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void cambiarEstado() {
        if (!Validaciones.esDuiValido(dui.getText())) {
            mensaje("Seleccione un cliente o escriba un DUI válido.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (clienteDAO.cambiarEstado(dui.getText().trim(), (EstadoCliente) estado.getSelectedItem())) {
                mensaje("Estado del cliente actualizado.", JOptionPane.INFORMATION_MESSAGE);
                cargarTodos();
            } else {
                mensaje("No se encontró el cliente.", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void buscar() {
        String criterio = busqueda.getText().trim();
        if (criterio.isEmpty()) {
            cargarTodos();
            return;
        }
        try {
            List<Cliente> encontrados = new ArrayList<>();
            if (Validaciones.esDuiValido(criterio)) {
                Cliente c = clienteDAO.buscarPorDui(criterio);
                if (c != null) encontrados.add(c);
            } else {
                encontrados = clienteDAO.buscarPorNombre(criterio);
            }
            mostrar(encontrados);
            if (encontrados.isEmpty()) mensaje("No se encontraron clientes.", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void cargarTodos() {
        try {
            mostrar(clienteDAO.consultarTodos());
        } catch (SQLException ex) {
            errorBD(ex);
        }
    }

    private void mostrar(List<Cliente> clientes) {
        modelo.setRowCount(0);
        for (Cliente c : clientes) {
            modelo.addRow(new Object[]{
                c.getDuiCliente(), c.getNombreCompletoCliente(),
                String.join(", ", c.getTelefonos()), c.getCorreoCliente(),
                c.getDireccionCliente(), c.getEstadoCliente()
            });
        }
    }

    private void cargarSeleccion() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) return;
        int fila = tabla.convertRowIndexToModel(filaVista);
        dui.setText(valor(fila, 0));
        nombre.setText(valor(fila, 1));
        telefonos.setText(valor(fila, 2));
        correo.setText(valor(fila, 3));
        direccion.setText(valor(fila, 4));
        estado.setSelectedItem(EstadoCliente.valueOf(valor(fila, 5)));
        dui.setEditable(false); //El DUI es la llave primaria en la base de datos.
    }

    private String valor(int fila, int columna) {
        Object dato = modelo.getValueAt(fila, columna);
        return dato == null ? "" : dato.toString();
    }

    private void limpiar() {
        dui.setText(""); dui.setEditable(true);
        nombre.setText(""); telefonos.setText(""); correo.setText("");
        direccion.setText(""); estado.setSelectedItem(EstadoCliente.ACTIVO);
        tabla.clearSelection();
    }

    private void mensaje(String texto, int tipo) {
        JOptionPane.showMessageDialog(this, texto, "Gestión de clientes", tipo);
    }

    private void errorBD(SQLException ex) {
        mensaje("Ocurrió un error al trabajar con la base de datos:\n" + ex.getMessage(),
                JOptionPane.ERROR_MESSAGE);
    }
}
