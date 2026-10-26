package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Índice de estaciones por identificador.
 *
 * <p>Esta clase define el contrato público que utiliza {@link Grafo} para
 * registrar y localizar estaciones por su {@code id}. La estrategia de
 * almacenamiento es un detalle interno de la clase.</p>
 *
 * <p><b>Implementación actual:</b> las estaciones se guardan en una
 * {@link ListaEnlazada} y la búsqueda recorre la lista, por lo que
 * {@code buscar}, {@code contiene}, {@code insertar} y {@code eliminar} cuestan
 * O(n). Está previsto sustituirla por una tabla hash (función de dispersión y
 * resolución de colisiones) con costo promedio O(1), manteniendo las firmas
 * públicas para que {@code Grafo} no requiera cambios.</p>
 */
public class TablaHash {

    /** Almacenamiento interno de la implementación actual. */
    private final ListaEnlazada<Estacion> estaciones;

    /** Crea una tabla vacía. */
    public TablaHash() {
        this.estaciones = new ListaEnlazada<>();
    }

    /**
     * Inserta una estación utilizando su identificador como clave.
     *
     * @param e estación a insertar
     * @return {@code false} si la estación es nula o ya existe una con el mismo
     *         identificador; {@code true} en caso contrario
     */
    public boolean insertar(Estacion e) {
        if (e == null || contiene(e.getId())) {
            return false;
        }
        estaciones.insertarAlFinal(e);
        return true;
    }

    /**
     * Busca una estación por su identificador.
     *
     * @param id identificador buscado, se ignoran los espacios en los extremos
     * @return la estación, o {@code null} si no existe o el identificador es nulo
     */
    public Estacion buscar(String id) {
        if (id == null) {
            return null;
        }
        String limpio = id.trim();
        Nodo<Estacion> actual = estaciones.getCabeza();
        while (actual != null) {
            if (actual.getDato().getId().equals(limpio)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /**
     * Indica si existe una estación con el identificador dado.
     *
     * @param id identificador buscado; se ignoran los espacios en los extremos
     * @return {@code true} si existe
     */
    public boolean contiene(String id) {
        return buscar(id) != null;
    }

    /**
     * Elimina la estación con el identificador dado.
     *
     * @param id identificador de la estación; se ignoran los espacios en los extremos
     * @return la estación eliminada, o {@code null} si no existía
     */
    public Estacion eliminar(String id) {
        Estacion e = buscar(id);
        if (e != null) {
            estaciones.eliminar(e);
        }
        return e;
    }

    /**
     * Obtiene todas las estaciones registradas. No se garantiza ningún orden.
     *
     * @return una lista nueva con las estaciones, nunca es {@code null} y su
     *         modificación no afecta a la tabla
     */
    public ListaEnlazada<Estacion> obtenerTodas() {
        ListaEnlazada<Estacion> copia = new ListaEnlazada<>();
        Nodo<Estacion> actual = estaciones.getCabeza();
        while (actual != null) {
            copia.insertarAlFinal(actual.getDato());
            actual = actual.getSiguiente();
        }
        return copia;
    }

    /**
     * Obtiene la cantidad de estaciones registradas.
     *
     * @return el número de estaciones
     */
    public int getTamano() {
        return estaciones.getTamano();
    }

    /**
     * Indica si la tabla no contiene estaciones.
     *
     * @return {@code true} si está vacía
     */
    public boolean estaVacia() {
        return estaciones.estaVacia();
    }
}
