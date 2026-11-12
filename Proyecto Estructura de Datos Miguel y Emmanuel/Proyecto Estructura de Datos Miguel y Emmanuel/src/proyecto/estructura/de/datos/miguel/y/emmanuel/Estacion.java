package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Estación (vértice) de la red de transporte.
 *
 * <p>Una estación está <b>activa</b> (operativa) o <b>cerrada</b>. El cierre es
 * temporal: la estación sigue formando parte del grafo y de la tabla hash, y
 * conserva sus conexiones, pero los algoritmos de análisis no la consideran.
 * Eliminarla de la red es una operación distinta, que realiza {@link Grafo}.</p>
 *
 * <p>La identidad de una estación es su {@code id}, dos estaciones con el mismo
 * identificador son iguales según {@link #equals(Object)}.</p>
 */
public class Estacion {

    /** Identificador único e inmutable; es la clave de búsqueda de la estación. */
    private final String id;

    /** Nombre de la estación. */
    private String nombre;

    /** Línea a la que pertenece la estación. */
    private String linea;

    /** Zona de la ciudad en la que se ubica la estación. */
    private String zona;

    /** Capacidad de la estación en pasajeros por hora; nunca es negativa. */
    private int capacidadPasajerosHora;

    /** Estado operativo: {@code true} si está activa, {@code false} si está cerrada. */
    private boolean activa;

    /**
     * Posición de la estación en las estructuras internas de {@link Grafo}.
     * Solo el grafo la asigna al incorporar la estación, vale -1 mientras la
     * estación no pertenece a ningún grafo.
     */
    private int indice;

    /**
     * Crea una estación activa que todavía no pertenece a ningún grafo.
     *
     * <p>Los textos se almacenan sin espacios en los extremos; un texto nulo se
     * almacena como cadena vacía.</p>
     *
     * @param id identificador único; no puede ser nulo ni vacío
     * @param nombre nombre de la estación
     * @param linea línea a la que pertenece
     * @param zona zona de la ciudad en la que se ubica
     * @param capacidadPasajerosHora capacidad en pasajeros por hora, no puede
     *        ser negativa
     * @throws IllegalArgumentException si el identificador es nulo o vacío, o si
     *         la capacidad es negativa. Los datos que provienen de fuentes
     *         externas (entrada del usuario, archivos) deben validarse antes de
     *         construir la estación.
     */
    public Estacion(String id, String nombre, String linea, String zona,
                    int capacidadPasajerosHora) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El id de la estación no puede estar vacío");
        }
        if (capacidadPasajerosHora < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa");
        }
        this.id = id.trim();
        this.nombre = nombre == null ? "" : nombre.trim();
        this.linea = linea == null ? "" : linea.trim();
        this.zona = zona == null ? "" : zona.trim();
        this.capacidadPasajerosHora = capacidadPasajerosHora;
        this.activa = true;
        this.indice = -1;
    }

    /**
     * Obtiene el identificador único de la estación.
     *
     * @return el identificador, que nunca cambia
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene el nombre de la estación.
     *
     * @return el nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Cambia el nombre de la estación.
     *
     * @param nombre nuevo nombre, un valor nulo se almacena como cadena vacía
     */
    public void setNombre(String nombre) {
        this.nombre = nombre == null ? "" : nombre.trim();
    }

    /**
     * Obtiene la línea a la que pertenece la estación.
     *
     * @return la línea
     */
    public String getLinea() {
        return linea;
    }

    /**
     * Cambia la línea de la estación.
     *
     * @param linea nueva línea, un valor nulo se almacena como cadena vacía
     */
    public void setLinea(String linea) {
        this.linea = linea == null ? "" : linea.trim();
    }

    /**
     * Obtiene la zona de la ciudad en la que se ubica la estación.
     *
     * @return la zona
     */
    public String getZona() {
        return zona;
    }

    /**
     * Cambia la zona de la estación.
     *
     * @param zona nueva zona, un valor nulo se almacena como cadena vacía
     */
    public void setZona(String zona) {
        this.zona = zona == null ? "" : zona.trim();
    }

    /**
     * Obtiene la capacidad de la estación en pasajeros por hora.
     *
     * @return la capacidad
     */
    public int getCapacidadPasajerosHora() {
        return capacidadPasajerosHora;
    }

    /**
     * Cambia la capacidad de la estación.
     *
     * @param capacidadPasajerosHora nueva capacidad, si es negativa, el cambio
     *        se ignora
     */
    public void setCapacidadPasajerosHora(int capacidadPasajerosHora) {
        if (capacidadPasajerosHora >= 0) {
            this.capacidadPasajerosHora = capacidadPasajerosHora;
        }
    }

    /**
     * Indica si la estación está operativa.
     *
     * @return {@code true} si está activa, {@code false} si está cerrada
     */
    public boolean estaActiva() {
        return activa;
    }

    /**
     * Cierra o reabre la estación.
     *
     * @param activa {@code false} para cerrarla, {@code true} para reabrirla
     */
    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    /**
     * Obtiene la posición de la estación en las estructuras internas del grafo.
     *
     * @return la posición, o -1 si la estación no pertenece a ningún grafo
     */
    public int getIndice() {
        return indice;
    }

    /**
     * Establece la posición de la estación en las estructuras internas del
     * grafo. Está destinado a uso exclusivo de {@link Grafo}.
     *
     * @param indice nueva posición, o -1 si la estación deja de pertenecer al grafo
     */
    public void setIndice(int indice) {
        this.indice = indice;
    }

    /**
     * Compara esta estación con otro objeto. Dos estaciones son iguales si
     * tienen el mismo identificador.
     *
     * @param obj objeto con el que se compara
     * @return {@code true} si {@code obj} es una estación con el mismo identificador
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Estacion)) {
            return false;
        }
        Estacion otra = (Estacion) obj;
        return id.equals(otra.id);
    }

    /**
     * Calcula un código hash coherente con {@link #equals(Object)}, basado en el
     * identificador.
     *
     * @return el código hash de la estación
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Devuelve una representación textual con el nombre y el identificador, que
     * indica además si la estación está cerrada.
     *
     * @return el texto, por ejemplo {@code "Chacao (CHA)"} o
     *         {@code "Chacao (CHA) [CERRADA]"}
     */
    @Override
    public String toString() {
        return nombre + " (" + id + ")" + (activa ? "" : " [CERRADA]");
    }
}
