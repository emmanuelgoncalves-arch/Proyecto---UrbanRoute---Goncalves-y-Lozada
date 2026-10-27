package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Resultado del cálculo de la ruta de menor tiempo: el camino, de la estación
 * de origen a la de destino, y el tiempo total del trayecto.
 */
public class Ruta {

    /** Estaciones del trayecto, en orden, de origen a destino. */
    private final ListaEnlazada<Estacion> camino;

    /** Tiempo total del trayecto, en minutos. */
    private final int tiempoTotalMin;

    /**
     * Crea una ruta.
     *
     * @param camino estaciones en orden, de origen a destino; no puede ser nulo
     *        ni estar vacío
     * @param tiempoTotalMin tiempo total del trayecto, en minutos
     * @throws IllegalArgumentException si el camino es nulo o está vacío
     */
    public Ruta(ListaEnlazada<Estacion> camino, int tiempoTotalMin) {
        if (camino == null || camino.estaVacia()) {
            throw new IllegalArgumentException("Una ruta necesita al menos una estación");
        }
        this.camino = camino;
        this.tiempoTotalMin = tiempoTotalMin;
    }
    /**
     * Obtiene el camino completo.
     *
     * @return las estaciones del trayecto, de origen a destino
     */
    public ListaEnlazada<Estacion> getCamino() {
        return camino;
    }

    /**
     * Obtiene el tiempo total del trayecto.
     *
     * @return el tiempo en minutos
     */
    public int getTiempoTotalMin() {
        return tiempoTotalMin;
    }

    /**
     * Obtiene la estación de partida.
     *
     * @return la primera estación del camino
     */
    public Estacion getOrigen() {
        return camino.primero().getDato();
    }

    /**
     * Obtiene la estación de llegada.
     *
     * @return la última estación del camino
     */
    public Estacion getDestino() {
        return camino.ultimo().getDato();
    }
    /**
     * Obtiene la cantidad de estaciones del camino, incluidas la de origen y la
     * de destino.
     *
     * @return el número total de estaciones
     */
    public int getNumEstaciones() {
        return camino.getTamano();
    }

    /**
     * Obtiene la cantidad de estaciones intermedias, es decir, sin contar la de
     * origen ni la de destino.
     *
     * @return el número de estaciones intermedias
     */
    public int getNumEstacionesIntermedias() {
        return Math.max(0, camino.getTamano() - 2);
    }
    /**
     * Cuenta las veces que dos estaciones
     * consecutivas pertenecen a líneas distintas (trasbordos)
     *
     * @return el número de transbordos
     */
    public int getNumTransbordos() {
        int transbordos = 0;
        Estacion previa = null;
        Nodo<Estacion> nodo = camino.getCabeza();
        while (nodo != null) {
            Estacion actual = nodo.getDato();
            if (previa != null && !previa.getLinea().equals(actual.getLinea())) {
                transbordos++;
            }
            previa = actual;
            nodo = nodo.getSiguiente();
        }
        return transbordos;
    }
    /**
     * Describe el trayecto con los nombres de las estaciones.
     *
     * @return un texto como {@code "Propatria -> Perez Bonalde -> Plaza Sucre"}
     */
    public String describir() {
        StringBuilder texto = new StringBuilder();
        Nodo<Estacion> nodo = camino.getCabeza();
        while (nodo != null) {
            if (texto.length() > 0) {
                texto.append(" -> ");
            }
            texto.append(nodo.getDato().getNombre());
            nodo = nodo.getSiguiente();
        }
        return texto.toString();
    }

    @Override
    public String toString() {
        return describir() + " (" + tiempoTotalMin + " min)";
    }
}