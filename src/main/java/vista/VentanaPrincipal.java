package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import dao.ClienteDAO;
import dao.CuentaDAO;

public class VentanaPrincipal extends JFrame {
    public VentanaPrincipal() {
        setTitle("Banco Académico UDB | Sistema de Gestión Bancaria");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new java.awt.Dimension(920, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(Estilos.AZUL);
        encabezado.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JLabel titulo = new JLabel("BANCO ACADÉMICO UDB");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitulo = new JLabel("Sistema de Gestión Bancaria | Fase 1");
        subtitulo.setForeground(new Color(220, 230, 242));
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(new javax.swing.BoxLayout(bloque, javax.swing.BoxLayout.Y_AXIS));
        bloque.add(titulo);
        bloque.add(javax.swing.Box.createVerticalStrut(5));
        bloque.add(subtitulo);
        encabezado.add(bloque, BorderLayout.WEST);
        add(encabezado, BorderLayout.NORTH);

        ClienteDAO clienteDAO = new ClienteDAO();
        CuentaDAO cuentaDAO = new CuentaDAO();

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(new Font("SansSerif", Font.BOLD, 13));
        pestanas.addTab("  Gestión de clientes  ", new GestionClientes(clienteDAO));
        pestanas.addTab("  Gestión de cuentas  ", new GestionCuentas(clienteDAO, cuentaDAO));
        add(pestanas, BorderLayout.CENTER);

        JLabel pie = new JLabel("Universidad Don Bosco  |  POO901  |  Aplicación de escritorio",
                SwingConstants.CENTER);
        pie.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 5, 10, 5));
        pie.setForeground(new Color(105, 114, 126));
        add(pie, BorderLayout.SOUTH);
    }
}
