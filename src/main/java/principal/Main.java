package principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import vista.Estilos;
import vista.VentanaPrincipal;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        LOGGER.info("Inicio de la aplicación Sistema de Gestión Bancaria Académica");
        SwingUtilities.invokeLater(() -> {
            Estilos.aplicar();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.warn("No se pudo aplicar el estilo visual del sistema", ex);
            }
            try {
                new VentanaPrincipal().setVisible(true);
            } catch (RuntimeException ex) {
                LOGGER.error("No se pudo iniciar la ventana principal", ex);
                throw ex;
            }
        });
    }
}
