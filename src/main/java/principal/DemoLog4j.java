package principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Emite ejemplos para comprobar la configuración Log4J2 desde el IDE. */
public final class DemoLog4j {
    private static final Logger LOGGER = LogManager.getLogger(DemoLog4j.class);

    private DemoLog4j() { }

    public static void main(String[] args) {
        LOGGER.info("Ejemplo INFO: inicio de una operación bancaria");
        LOGGER.warn("Ejemplo WARN: operación rechazada por una regla de negocio");
        try {
            throw new IllegalStateException("Ejemplo de error controlado para verificar el log");
        } catch (IllegalStateException ex) {
            LOGGER.error("Ejemplo ERROR: ocurrió un problema durante la operación", ex);
        }
    }
}
