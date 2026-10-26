package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Red de transporte representada como un grafo ponderado y no dirigido, con
 * listas de adyacencia.
 *
 * <p><b>Representación.</b> Cada estación ocupa una posición (su
 * {@linkplain Estacion#getIndice() índice}) en dos arreglos paralelos,
 * {@code vertices[i]} guarda la estación y {@code adyacencia[i]} la lista
 * enlazada de aristas que salen de ella. Una conexión entre A y B se almacena
 * como dos aristas simétricas con el mismo tiempo, una en la lista de A y otra
 * en la de B.</p>
 *
 * <p><b>Estaciones cerradas.</b> Cerrar una estación solo cambia su estado, se
 * conserva en el grafo, en la tabla hash y con sus conexiones. Las consultas de
 * {@link #getGrado(Estacion)} y los algoritmos de análisis la ignoran.
 * {@link #eliminarEstacion(String)}, en cambio, la quita por completo.</p>
 *
 * <p><b>Consistencia.</b> Esta clase es la única que modifica de forma conjunta
 * el grafo y la {@link TablaHash}, toda estación presente en uno lo está en la
 * otra, y la búsqueda por identificador se delega en la tabla hash.</p>
 *
 * <p><b>Contrato de errores.</b> Los métodos que reciben datos externos no
 * lanzan excepciones, informan el resultado con {@code false} o {@code null}. Las
 * listas que devuelven nunca son {@code null}.</p>
 *
 * <p>Los algoritmos de análisis (ruta mínima, subredes y hubs) operan sobre esta
 * estructura y no forman parte de esta versión de la clase.</p>
 */
public class Grafo {

    /** Capacidad inicial de los arreglos internos, se duplica al llenarse. */
    private static final int CAPACIDAD_INICIAL = 16;

    /** Índice de estaciones por identificador, contiene las mismas estaciones que {@code vertices}. */
    private final TablaHash tabla;

    /** Estaciones del grafo, están ocupadas las posiciones de 0 a {@code numEstaciones - 1}. */
    private Estacion[] vertices;

    /** Lista de adyacencia de cada estación, en la misma posición que {@code vertices}. */
    private ListaEnlazada<Arista>[] adyacencia;

    /** Cantidad de estaciones almacenadas en los arreglos internos. */
    private int numEstaciones;

    /** Cantidad de conexiones, cada una se representa con dos aristas simétricas. */
    private int numConexiones;

    /** Crea un grafo vacío con su propia tabla hash. */
    public Grafo() {
        this(new TablaHash());
    }

    /**
     * Crea un grafo vacío que utiliza la tabla hash indicada.
     *
     * @param tabla tabla hash vacía, si es {@code null} se crea una nueva
     * @throws IllegalArgumentException si la tabla ya contiene estaciones
     */
    public Grafo(TablaHash tabla) {
        if (tabla != null && !tabla.estaVacia()) {
            throw new IllegalArgumentException("La tabla hash debe estar vacía");
        }
        this.tabla = tabla == null ? new TablaHash() : tabla;
        this.vertices = new Estacion[CAPACIDAD_INICIAL];
        this.adyacencia = crearArregloAdyacencia(CAPACIDAD_INICIAL);
        this.numEstaciones = 0;
        this.numConexiones = 0;
    }

    /**
     * Crea un arreglo de listas de aristas. Java no permite crear arreglos de
     * tipos genéricos directamente ({@code new ListaEnlazada<Arista>[n]}), por lo
     * que se crea el arreglo del tipo sin parametrizar y se convierte. La
     * conversión es segura porque el arreglo es privado y solo almacena
     * {@code ListaEnlazada<Arista>}.
     *
     * @param tamano cantidad de posiciones del arreglo
     * @return un arreglo con todas las posiciones en {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ListaEnlazada<Arista>[] crearArregloAdyacencia(int tamano) {
        return (ListaEnlazada<Arista>[]) new ListaEnlazada[tamano];
    }

    // ------------------------------------------------------------------
    // Estaciones
    // ------------------------------------------------------------------

    /**
     * Agrega una estación al grafo y a la tabla hash.
     *
     * @param e estación a agregar
     * @return {@code false} si la estación es nula o ya existe una con el mismo
     *         identificador, {@code true} en caso contrario
     */
    public boolean agregarEstacion(Estacion e) {
        if (e == null || tabla.contiene(e.getId())) {
            return false;
        }
        if (!tabla.insertar(e)) {
            return false;
        }
        if (numEstaciones == vertices.length) {
            ampliar();
        }
        vertices[numEstaciones] = e;
        adyacencia[numEstaciones] = new ListaEnlazada<>();
        e.setIndice(numEstaciones);
        numEstaciones++;
        return true;
    }

    /**
     * Elimina una estación: la quita de la tabla hash, borra todas sus
     * conexiones y la retira de los arreglos. Las estaciones restantes conservan
     * su orden relativo y se actualiza su índice.
     *
     * @param id identificador de la estación
     * @return {@code false} si la estación no existe, {@code true} en caso contrario
     */
    public boolean eliminarEstacion(String id) {
        Estacion e = buscarEstacion(id);
        if (e == null) {
            return false;
        }
        int posicion = e.getIndice();

        // Se retira de cada vecino la arista que llega a la estación eliminada.
        Nodo<Arista> nodo = adyacencia[posicion].getCabeza();
        while (nodo != null) {
            Estacion vecino = nodo.getDato().getDestino();
            quitarArista(vecino, e);
            numConexiones--;
            nodo = nodo.getSiguiente();
        }
        adyacencia[posicion].vaciar();

        tabla.eliminar(e.getId());

        // Se cierra el hueco desplazando una posición las estaciones siguientes.
        for (int i = posicion; i < numEstaciones - 1; i++) {
            vertices[i] = vertices[i + 1];
            adyacencia[i] = adyacencia[i + 1];
            vertices[i].setIndice(i);
        }
        vertices[numEstaciones - 1] = null;
        adyacencia[numEstaciones - 1] = null;
        numEstaciones--;
        e.setIndice(-1);
        return true;
    }

    /**
     * Cierra temporalmente una estación. La estación permanece en el grafo y en
     * la tabla hash, pero los algoritmos de análisis la ignoran. Cerrar una
     * estación que ya está cerrada no produce cambios.
     *
     * @param id identificador de la estación
     * @return {@code false} si la estación no existe, {@code true} en caso contrario
     */
    public boolean cerrarEstacion(String id) {
        Estacion e = buscarEstacion(id);
        if (e == null) {
            return false;
        }
        e.setActiva(false);
        return true;
    }

    /**
     * Reabre una estación cerrada. Reabrir una estación que ya está activa no
     * produce cambios.
     *
     * @param id identificador de la estación
     * @return {@code false} si la estación no existe, {@code true} en caso contrario
     */
    public boolean reabrirEstacion(String id) {
        Estacion e = buscarEstacion(id);
        if (e == null) {
            return false;
        }
        e.setActiva(true);
        return true;
    }

    /**
     * Busca una estación por su identificador, delegando en la tabla hash.
     *
     * @param id identificador buscado
     * @return la estación, o {@code null} si no existe
     */
    public Estacion buscarEstacion(String id) {
        return tabla.buscar(id);
    }

    // ------------------------------------------------------------------
    // Conexiones
    // ------------------------------------------------------------------

    /**
     * Agrega una conexión entre dos estaciones existentes, almacenando las dos
     * aristas simétricas. Se permite conectar estaciones cerradas, porque el
     * cierre es temporal y no descarta datos.
     *
     * @param idOrigen identificador de una de las estaciones
     * @param idDestino identificador de la otra estación
     * @param tiempoMin tiempo de traslado en minutos, debe ser mayor que 0
     * @return {@code false} si alguna estación no existe, si ambas son la misma,
     *         si el tiempo no es mayor que 0 o si la conexión ya existe,
     *         {@code true} en caso contrario
     */
    public boolean agregarConexion(String idOrigen, String idDestino, int tiempoMin) {
        if (tiempoMin <= 0) {
            return false;
        }
        Estacion a = buscarEstacion(idOrigen);
        Estacion b = buscarEstacion(idDestino);
        if (a == null || b == null || a.equals(b)) {
            return false;
        }
        if (buscarArista(a, b) != null) {
            return false;
        }
        adyacencia[a.getIndice()].insertarAlFinal(new Arista(a, b, tiempoMin));
        adyacencia[b.getIndice()].insertarAlFinal(new Arista(b, a, tiempoMin));
        numConexiones++;
        return true;
    }

    /**
     * Cambia el tiempo de una conexión existente en ambos sentidos.
     *
     * @param idOrigen identificador de una de las estaciones
     * @param idDestino identificador de la otra estación
     * @param nuevoTiempoMin nuevo tiempo en minutos, debe ser mayor que 0
     * @return {@code false} si la conexión no existe o el tiempo no es válido,
     *         {@code true} en caso contrario
     */
    public boolean modificarConexion(String idOrigen, String idDestino, int nuevoTiempoMin) {
        if (nuevoTiempoMin <= 0) {
            return false;
        }
        Estacion a = buscarEstacion(idOrigen);
        Estacion b = buscarEstacion(idDestino);
        if (a == null || b == null) {
            return false;
        }
        Arista ab = buscarArista(a, b);
        Arista ba = buscarArista(b, a);
        if (ab == null || ba == null) {
            return false;
        }
        ab.setTiempoMin(nuevoTiempoMin);
        ba.setTiempoMin(nuevoTiempoMin);
        return true;
    }

    /**
     * Elimina una conexión, retirando las dos aristas simétricas.
     *
     * @param idOrigen identificador de una de las estaciones
     * @param idDestino identificador de la otra estación
     * @return {@code false} si alguna estación o la conexión no existe,
     *         {@code true} en caso contrario
     */
    public boolean eliminarConexion(String idOrigen, String idDestino) {
        Estacion a = buscarEstacion(idOrigen);
        Estacion b = buscarEstacion(idDestino);
        if (a == null || b == null || buscarArista(a, b) == null) {
            return false;
        }
        quitarArista(a, b);
        quitarArista(b, a);
        numConexiones--;
        return true;
    }

    /**
     * Indica si dos estaciones están conectadas directamente.
     *
     * @param idA identificador de una de las estaciones
     * @param idB identificador de la otra estación
     * @return {@code true} si ambas existen y están conectadas
     */
    public boolean existeConexion(String idA, String idB) {
        Estacion a = buscarEstacion(idA);
        Estacion b = buscarEstacion(idB);
        return a != null && b != null && buscarArista(a, b) != null;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Obtiene la tabla hash que utiliza el grafo.
     *
     * @return la tabla hash
     */
    public TablaHash getTablaHash() {
        return tabla;
    }

    /**
     * Obtiene la cantidad de estaciones, incluidas las cerradas.
     *
     * @return el número de estaciones
     */
    public int getNumEstaciones() {
        return numEstaciones;
    }

    /**
     * Obtiene la cantidad de conexiones, incluidas las que involucran estaciones
     * cerradas. Cada conexión se cuenta una sola vez.
     *
     * @return el número de conexiones
     */
    public int getNumConexiones() {
        return numConexiones;
    }

    /**
     * Obtiene todas las estaciones, incluidas las cerradas, en el orden en que
     * fueron agregadas.
     *
     * @return una lista nueva; nunca es {@code null} y su modificación no afecta
     *         al grafo
     */
    public ListaEnlazada<Estacion> getEstaciones() {
        ListaEnlazada<Estacion> resultado = new ListaEnlazada<>();
        for (int i = 0; i < numEstaciones; i++) {
            resultado.insertarAlFinal(vertices[i]);
        }
        return resultado;
    }

    /**
     * Obtiene las aristas que salen de una estación, incluidas las que llegan a
     * estaciones cerradas.
     *
     * <p>La lista devuelta es una copia, pero sus aristas son las del grafo: no
     * deben modificarse directamente, sino mediante
     * {@link #modificarConexion(String, String, int)}.</p>
     *
     * @param e estación consultada
     * @return una lista nueva; nunca es {@code null} y está vacía si la estación
     *         es nula o no pertenece al grafo
     */
    public ListaEnlazada<Arista> getAristasDe(Estacion e) {
        ListaEnlazada<Arista> resultado = new ListaEnlazada<>();
        int posicion = posicionDe(e);
        if (posicion < 0) {
            return resultado;
        }
        Nodo<Arista> nodo = adyacencia[posicion].getCabeza();
        while (nodo != null) {
            resultado.insertarAlFinal(nodo.getDato());
            nodo = nodo.getSiguiente();
        }
        return resultado;
    }

    /**
     * Obtiene una arista por cada conexión (y no las dos simétricas), de modo que
     * cada conexión aparezca una sola vez al guardarla en archivo o dibujarla.
     *
     * @return una lista nueva, nunca es {@code null}
     */
    public ListaEnlazada<Arista> getConexiones() {
        ListaEnlazada<Arista> resultado = new ListaEnlazada<>();
        for (int i = 0; i < numEstaciones; i++) {
            Nodo<Arista> nodo = adyacencia[i].getCabeza();
            while (nodo != null) {
                Arista arista = nodo.getDato();
                if (arista.getOrigen().getIndice() < arista.getDestino().getIndice()) {
                    resultado.insertarAlFinal(arista);
                }
                nodo = nodo.getSiguiente();
            }
        }
        return resultado;
    }

    /**
     * Calcula el grado de una estación: la cantidad de conexiones con estaciones
     * activas. Una estación cerrada tiene grado 0.
     *
     * @param e estación consultada
     * @return el grado, o 0 si la estación es nula, está cerrada o no pertenece
     *         al grafo
     */
    public int getGrado(Estacion e) {
        int posicion = posicionDe(e);
        if (posicion < 0 || !vertices[posicion].estaActiva()) {
            return 0;
        }
        int grado = 0;
        Nodo<Arista> nodo = adyacencia[posicion].getCabeza();
        while (nodo != null) {
            if (nodo.getDato().getDestino().estaActiva()) {
                grado++;
            }
            nodo = nodo.getSiguiente();
        }
        return grado;
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares privados
    // ------------------------------------------------------------------

    /**
     * Obtiene la posición de una estación en los arreglos internos, verificando
     * que pertenezca a este grafo.
     *
     * @param e estación consultada
     * @return la posición, o -1 si la estación es nula o no pertenece al grafo
     */
    private int posicionDe(Estacion e) {
        if (e == null) {
            return -1;
        }
        int posicion = e.getIndice();
        if (posicion < 0 || posicion >= numEstaciones || !vertices[posicion].equals(e)) {
            return -1;
        }
        return posicion;
    }

    /**
     * Busca la arista que va de una estación a otra.
     *
     * @param desde estación de partida; debe pertenecer al grafo
     * @param hasta estación de llegada
     * @return la arista, o {@code null} si no existe
     */
    private Arista buscarArista(Estacion desde, Estacion hasta) {
        Nodo<Arista> nodo = adyacencia[desde.getIndice()].getCabeza();
        while (nodo != null) {
            if (nodo.getDato().getDestino().equals(hasta)) {
                return nodo.getDato();
            }
            nodo = nodo.getSiguiente();
        }
        return null;
    }

    /**
     * Quita de la lista de adyacencia de una estación la arista que llega a otra.
     * No actualiza el contador de conexiones, tarea que corresponde al llamador.
     *
     * @param desde estación cuya lista se modifica, debe pertenecer al grafo
     * @param hasta estación a la que llegaba la arista
     */
    private void quitarArista(Estacion desde, Estacion hasta) {
        Arista arista = buscarArista(desde, hasta);
        if (arista != null) {
            adyacencia[desde.getIndice()].eliminar(arista);
        }
    }

    /** Duplica la capacidad de los arreglos internos copiando sus elementos. */
    private void ampliar() {
        int nuevaCapacidad = vertices.length * 2;
        Estacion[] nuevosVertices = new Estacion[nuevaCapacidad];
        ListaEnlazada<Arista>[] nuevaAdyacencia = crearArregloAdyacencia(nuevaCapacidad);
        for (int i = 0; i < numEstaciones; i++) {
            nuevosVertices[i] = vertices[i];
            nuevaAdyacencia[i] = adyacencia[i];
        }
        vertices = nuevosVertices;
        adyacencia = nuevaAdyacencia;
    }
}
