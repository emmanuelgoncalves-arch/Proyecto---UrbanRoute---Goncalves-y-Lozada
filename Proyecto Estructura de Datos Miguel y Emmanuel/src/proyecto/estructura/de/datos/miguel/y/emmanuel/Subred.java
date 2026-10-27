package proyecto.estructura.de.datos.miguel.y.emmanuel;
/**
 * Un grupo de estaciones conectadas entre sí
 * y desconectadas del resto.
 */
public class Subred {

    /** Número de la subred (1, 2, 3...). */
    private final int numero;

    /** Estaciones que forman la subred. */
    private final ListaEnlazada<Estacion> estaciones = new ListaEnlazada<>();

    /** Suma de la capacidad (pasajeros por hora) de todas sus estaciones. */
    private int capacidadAcumulada;

    /**
     * Crea una subred vacía.
     *
     * @param numero número correlativo de la subred
     */
    public Subred(int numero) {
        this.numero = numero;
    }
    /**
     * Agrega una estación a la subred y acumula su capacidad.
     *
     * @param e estación a agregar; si es nula no se hace nada
     */
    public void agregarEstacion(Estacion e) {
        if (e == null) {
            return;
        }
        estaciones.insertarAlFinal(e);
        capacidadAcumulada += e.getCapacidadPasajerosHora();
    }
    /**
     * Indica si una estación pertenece a la subred
     *
     * @param e estación a buscar
     * @return {@code true} si la estación pertenece a la subred
     */
    public boolean contiene(Estacion e) {
        return e != null && estaciones.contiene(e);
    }
    /**
     * Obtiene el número de la subred.
     *
     * @return el número de la subred
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Obtiene la cantidad de estaciones de la subred.
     *
     * @return el número de estaciones
     */
    public int getNumEstaciones() {
        return estaciones.getTamano();
    }

    /**
     * Obtiene la capacidad total de la subred.
     *
     * @return la suma de la capacidad (pasajeros por hora) de sus estaciones
     */
    public int getCapacidadAcumulada() {
        return capacidadAcumulada;
    }

    /**
     * Obtiene las estaciones de la subred. La lista es la propia de la subred:
     * debe usarse solo para consultar.
     *
     * @return la lista de estaciones
     */
    public ListaEnlazada<Estacion> getEstaciones() {
        return estaciones;
    }

    /**
     * Devuelve un texto con el número, la cantidad de estaciones y la capacidad.
     *
     * @return por ejemplo {@code "Subred 1 (41 estaciones, 12345 pasajeros/hora)"}
     */
    @Override
    public String toString() {
        return "Subred " + numero + " (" + getNumEstaciones() + " estaciones, "
                + capacidadAcumulada + " pasajeros/hora)";
    }
}
