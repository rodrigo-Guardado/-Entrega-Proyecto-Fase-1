package principal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import vista.Estilos;
import vista.VentanaPrincipal;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Estilos.aplicar();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("No se pudo aplicar el estilo del sistema: " + ex.getMessage());
            }
            new VentanaPrincipal().setVisible(true);
        });
    }
}
