package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Representa un registro de la tabla clientes y sus teléfonos relacionados. */
public class Cliente {
    private String duiCliente;
    private String nombreCompletoCliente;
    private String correoCliente;
    private String direccionCliente;
    private EstadoCliente estadoCliente;
    // telefonos_clientes es una tabla aparte. El DAO debe cargar/guardar la relación.
    private List<String> telefonos;

    public Cliente() {
        this.telefonos = new ArrayList<>();
    }

    public Cliente(String duiCliente, String nombreCompletoCliente, String correoCliente,
                   String direccionCliente, EstadoCliente estadoCliente) {
        this(duiCliente, nombreCompletoCliente, correoCliente, direccionCliente, estadoCliente, null);
    }

    public Cliente(String duiCliente, String nombreCompletoCliente, String correoCliente,
                   String direccionCliente, EstadoCliente estadoCliente, List<String> telefonos) {
        this.duiCliente = duiCliente;
        this.nombreCompletoCliente = nombreCompletoCliente;
        this.correoCliente = correoCliente;
        this.direccionCliente = direccionCliente;
        this.estadoCliente = estadoCliente;
        setTelefonos(telefonos);
    }

    public String getDuiCliente() { return duiCliente; }
    public void setDuiCliente(String duiCliente) { this.duiCliente = duiCliente; }
    public String getNombreCompletoCliente() { return nombreCompletoCliente; }
    public void setNombreCompletoCliente(String nombreCompletoCliente) { this.nombreCompletoCliente = nombreCompletoCliente; }
    public String getCorreoCliente() { return correoCliente; }
    public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }
    public String getDireccionCliente() { return direccionCliente; }
    public void setDireccionCliente(String direccionCliente) { this.direccionCliente = direccionCliente; }
    public EstadoCliente getEstadoCliente() { return estadoCliente; }
    public void setEstadoCliente(EstadoCliente estadoCliente) { this.estadoCliente = estadoCliente; }

    /** Devuelve una vista no modificable; use agregarTelefono para añadir teléfonos. */
    public List<String> getTelefonos() {
        if (telefonos == null) telefonos = new ArrayList<>();
        return Collections.unmodifiableList(telefonos);
    }

    public void setTelefonos(List<String> telefonos) {
        this.telefonos = telefonos == null ? new ArrayList<>() : new ArrayList<>(telefonos);
    }

    public void agregarTelefono(String telefono) {
        if (telefonos == null) telefonos = new ArrayList<>();
        telefonos.add(Objects.requireNonNull(telefono, "El teléfono no puede ser null"));
    }

    public boolean quitarTelefono(String telefono) {
        return telefonos != null && telefonos.remove(telefono);
    }
}
