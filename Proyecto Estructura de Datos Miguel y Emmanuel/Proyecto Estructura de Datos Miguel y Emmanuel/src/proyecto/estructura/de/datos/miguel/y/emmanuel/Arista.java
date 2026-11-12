package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Arista dirigida de la red de transporte, une una estación de origen con una
 * de destino y registra el tiempo de traslado entre ambas.
 *
 * <p>El grafo es no dirigido, por lo que cada conexión entre dos estaciones A y B
 * se representa con dos aristas simétricas: una en la lista de adyacencia de A
 * (origen A, destino B) y otra en la de B (origen B, destino A). Ambas deben
 * mantener el mismo tiempo; {@link Grafo} es el responsable de conservar esa
 * simetría.</p>
 */
public class Arista {

    /** Estación de partida de la arista. */
    private final Estacion origen;

    /** Estación de llegada de la arista. */
    private final Estacion destino;

    /** Tiempo de traslado en minutos; siempre mayor que 0. */
    private int tiempoMin;

    /**
     * Crea una arista.
     *
     * @param origen estación de partida; no puede ser nula
     * @param destino estación de llegada; no puede ser nula
     * @param tiempoMin tiempo de traslado en minutos; debe ser mayor que 0
     * @throws IllegalArgumentException si alguna estación es nula o el tiempo no
     *         es mayor que 0. Los datos que provienen de fuentes externas
     *         (entrada del usuario, archivos) deben validarse antes de construir
     *         la arista.
     */
    public Arista(Estacion origen, Estacion destino, int tiempoMin) {
        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Las estaciones de una arista no pueden ser nulas");
        }
        if (tiempoMin <= 0) {
            throw new IllegalArgumentException("El tiempo debe ser mayor que 0");
        }
        this.origen = origen;
        this.destino = destino;
        this.tiempoMin = tiempoMin;
    }

    /**
     * Obtiene la estación de partida.
     *
     * @return la estación de origen
     */
    public Estacion getOrigen() {
        return origen;
    }

    /**
     * Obtiene la estación de llegada.
     *
     * @return la estación de destino
     */
    public Estacion getDestino() {
        return destino;
    }

    /**
     * Obtiene el tiempo de traslado entre las dos estaciones.
     *
     * @return el tiempo en minutos
     */
    public int getTiempoMin() {
        return tiempoMin;
    }

    /**
     * Cambia el tiempo de traslado. Este método modifica solo esta arista; para
     * actualizar una conexión completa debe usarse
     * {@link Grafo#modificarConexion(String, String, int)}.
     *
     * @param tiempoMin nuevo tiempo en minutos; si no es mayor que 0, el cambio
     *        se ignora
     */
    public void setTiempoMin(int tiempoMin) {
        if (tiempoMin > 0) {
            this.tiempoMin = tiempoMin;
        }
    }

    /**
     * Dada una de las dos estaciones de la arista, obtiene la otra.
     *
     * @param e una de las estaciones de la arista
     * @return la estación del otro extremo, o {@code null} si {@code e} es nula
     *         o no pertenece a la arista
     */
    public Estacion getOtroExtremo(Estacion e) {
        if (e == null) {
            return null;
        }
        if (e.equals(origen)) {
            return destino;
        }
        if (e.equals(destino)) {
            return origen;
        }
        return null;
    }

    /**
     * Devuelve una representación textual con los identificadores de origen y
     * destino y el tiempo.
     *
     * @return el texto, por ejemplo {@code "CHA -> PZA_VZLA (3 min)"}
     */
    @Override
    public String toString() {
        return origen.getId() + " -> " + destino.getId() + " (" + tiempoMin + " min)";
    }
}
