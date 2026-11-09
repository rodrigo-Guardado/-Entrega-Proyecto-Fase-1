package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/sistema_bancario";
    private static final String USUARIO = "root";
    //Cambiar CLAVE por la contraseña de MySQL de cada computadora
    private static final String CLAVE = "";

    private ConexionBD() {
    }

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    public static void cerrar(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.close();
        } catch (SQLException e) {
            System.err.println("No se pudo cerrar la conexión: " + e.getMessage());
        }
    }
}
