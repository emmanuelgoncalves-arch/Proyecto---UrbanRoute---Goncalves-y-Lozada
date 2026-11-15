package proyecto.estructura.de.datos.miguel.y.emmanuel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

/**
 * Dibuja la red de transporte con GraphStream dentro de un panel de la propia
 * aplicación (no abre ventanas aparte).
 *
 * <p><b>Papel de la clase.</b> GraphStream se usa únicamente para el
 * renderizado: la información de la red sigue
 * viviendo en {@link Grafo} y en {@link TablaHash}. Esta clase mantiene un grafo
 * de GraphStream que es solo un espejo para dibujar, y {@link #actualizar(Grafo, int, boolean)}
 * lo sincroniza con el estado actual de la red cada vez que se invoca.</p>
 *
 * <p><b>Identificación visual de componentes.</b> Las estaciones se colorean
 * según el modo elegido:</p>
 * <ul>
 *   <li>{@link #POR_SUBRED}: cada subred (grupo de estaciones conectadas entre sí
 *       y aisladas del resto, obtenido con {@link Grafo#detectarSubredes()}) tiene
 *       su propio color, de modo que se ve de inmediato qué zonas quedan
 *       incomunicadas tras un cierre.</li>
 *   <li>{@link #POR_LINEA}: cada línea de transporte tiene su propio color; las
 *       conexiones entre estaciones de líneas distintas (transbordos) se dibujan
 *       en gris oscuro.</li>
 * </ul>
 * <p>Las estaciones cerradas se dibujan siempre con relleno claro y borde rojo, y
 * sus conexiones en gris claro, porque los algoritmos las ignoran.</p>
 *
 * <p><b>Actualización.</b> La sincronización es incremental: los nodos que ya
 * existen se conservan, por lo que el dibujo no se reacomoda por completo cada
 * vez que cambia la red; solo se quitan los nodos eliminados, se agregan los
 * nuevos y se vuelven a crear las aristas.</p>
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class VisualizadorGrafo {

    /** Modo de color: una tonalidad por subred (componente conexo). */
    public static final int POR_SUBRED = 0;

    /** Modo de color: una tonalidad por línea de transporte. */
    public static final int POR_LINEA = 1;

    /** Colores (en hexadecimal) que se reparten entre subredes o líneas; si hay más grupos que colores, se repiten. */
    private static final String[] PALETA = {
        "#2980b9", "#27ae60", "#e67e22", "#8e44ad", "#16a085",
        "#c0392b", "#f1c40f", "#34495e", "#d35400", "#e84393"
    };

    /** Relleno de una estación cerrada. */
    public static final String COLOR_CERRADA = "#ecf0f1";

    /** Borde de una estación cerrada. */
    private static final String BORDE_CERRADA = "#e74c3c";

    /** Color de una conexión que une estaciones de grupos distintos (transbordo entre líneas). */
    private static final String COLOR_TRANSBORDO = "#566573";

    /** Color de una conexión que toca una estación cerrada. */
    private static final String COLOR_ARISTA_CERRADA = "#d5d8dc";

    /** Hoja de estilos de GraphStream; el color de cada nodo y arista se ajusta individualmente. */
    private static final String ESTILO_GRAFO =
        "graph { canvas-color: white; padding: 40px; }"
        + "node { fill-color: #2980b9; size: 10px; text-size: 11px; text-color: #2c3e50;"
        + " text-alignment: under; stroke-mode: plain; stroke-color: #34495e; stroke-width: 1px; }"
        + "edge { fill-color: #95a5a6; size: 2px; text-size: 10px; text-color: #7f8c8d; }";
    // Las etiquetas no llevan fondo: un fondo opaco taparía las aristas que pasan por debajo
    // cuando las estaciones quedan cerca unas de otras.

    /**
     * Elemento de la leyenda: un color y lo que representa.
     */
    public static class EntradaLeyenda {

        /** Texto que describe el color. */
        private final String texto;

        /** Color en formato hexadecimal, por ejemplo {@code "#2980b9"}. */
        private final String colorHex;

        /**
         * Crea una entrada de leyenda.
         *
         * @param texto descripción de lo que representa el color
         * @param colorHex color en formato hexadecimal
         */
        public EntradaLeyenda(String texto, String colorHex) {
            this.texto = texto;
            this.colorHex = colorHex;
        }

        /**
         * Obtiene el texto de la entrada.
         *
         * @return la descripción
         */
        public String getTexto() {
            return texto;
        }

        /**
         * Obtiene el color de la entrada.
         *
         * @return el color en formato hexadecimal
         */
        public String getColorHex() {
            return colorHex;
        }
    }

    /** Grafo de GraphStream que se usa únicamente para dibujar. */
    private final Graph graficoGs;

    /** Visor que dibuja el grafo de GraphStream y calcula la posición de los nodos. */
    private final SwingViewer visor;

    /** Componente Swing que muestra el dibujo y se inserta en el panel de la aplicación. */
    private final ViewPanel vista;

    /**
     * Crea el visor y su componente de dibujo. El cálculo automático de
     * posiciones se activa para que los nodos no se superpongan.
     */
    public VisualizadorGrafo() {
        System.setProperty("org.graphstream.ui", "swing");
        graficoGs = new SingleGraph("Red de Transporte Urbano");
        graficoGs.setAttribute("ui.stylesheet", ESTILO_GRAFO);
        graficoGs.setAttribute("ui.antialias");
        graficoGs.setAttribute("ui.quality");
        visor = new SwingViewer(graficoGs, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        visor.enableAutoLayout();
        // false: el dibujo se obtiene como componente y no en una ventana propia.
        vista = (ViewPanel) visor.addDefaultView(false);
    }

    /**
     * Obtiene el componente que muestra el dibujo, para insertarlo en un panel.
     *
     * @return el componente de dibujo
     */
    public ViewPanel getVista() {
        return vista;
    }

    /**
     * Libera los recursos del visor (hilos de dibujo y de posicionamiento).
     */
    public void cerrar() {
        visor.close();
    }

    /**
     * Sincroniza el dibujo con el estado actual de la red y devuelve la leyenda
     * de colores que corresponde.
     *
     * @param grafo red de transporte que se dibuja; si es nula se deja el dibujo vacío
     * @param modo {@link #POR_SUBRED} o {@link #POR_LINEA}; cualquier otro valor se trata como {@link #POR_SUBRED}
     * @param mostrarTiempos {@code true} para escribir el tiempo en minutos sobre cada conexión
     * @return las entradas de la leyenda; nunca es {@code null}
     */
    public ListaEnlazada<EntradaLeyenda> actualizar(Grafo grafo, int modo, boolean mostrarTiempos) {
        ListaEnlazada<EntradaLeyenda> leyenda = new ListaEnlazada<>();
        if (grafo == null) {
            vaciarDibujo();
            return leyenda;
        }

        // 1. Se quitan del dibujo las estaciones que ya no están en la red (esto
        //    también quita sus aristas). Se recorre al revés porque quitar un
        //    nodo cambia los índices de los siguientes.
        for (int i = graficoGs.getNodeCount() - 1; i >= 0; i--) {
            Node nodo = graficoGs.getNode(i);
            if (grafo.buscarEstacion(nodo.getId()) == null) {
                graficoGs.removeNode(nodo);
            }
        }
        // 2. Las aristas se vuelven a crear siempre: sus tiempos y su color pueden haber cambiado.
        while (graficoGs.getEdgeCount() > 0) {
            graficoGs.removeEdge(0);
        }

        // 3. Se decide el grupo (color) de cada estación; el arreglo se indexa con
        //    la posición de la estación en el grafo y -1 significa "sin grupo".
        int[] grupo = new int[grafo.getNumEstaciones()];
        for (int i = 0; i < grupo.length; i++) {
            grupo[i] = -1;
        }
        if (modo == POR_LINEA) {
            asignarGruposPorLinea(grafo, grupo, leyenda);
        } else {
            asignarGruposPorSubred(grafo, grupo, leyenda);
        }

        // 4. Nodos: se crean los nuevos y se actualiza la etiqueta y el color de todos.
        boolean hayCerradas = false;
        Nodo<Estacion> actual = grafo.getEstaciones().getCabeza();
        while (actual != null) {
            Estacion e = actual.getDato();
            Node nodo = graficoGs.getNode(e.getId());
            if (nodo == null) {
                nodo = graficoGs.addNode(e.getId());
            }
            nodo.setAttribute("ui.label", e.getNombre());
            if (e.estaActiva()) {
                nodo.setAttribute("ui.style", "fill-color: " + colorDeGrupo(grupo[e.getIndice()]) + ";");
            } else {
                hayCerradas = true;
                nodo.setAttribute("ui.style", "fill-color: " + COLOR_CERRADA + "; stroke-color: "
                        + BORDE_CERRADA + "; stroke-width: 3px;");
            }
            actual = actual.getSiguiente();
        }
        if (hayCerradas) {
            leyenda.insertarAlFinal(new EntradaLeyenda("Estación cerrada", COLOR_CERRADA));
        }

        // 5. Aristas: una por conexión. Se colorean con el grupo de sus extremos si
        //    es el mismo; un transbordo entre grupos distintos va en gris oscuro, y
        //    una conexión con una estación cerrada, en gris claro.
        Nodo<Arista> nodoArista = grafo.getConexiones().getCabeza();
        while (nodoArista != null) {
            Arista a = nodoArista.getDato();
            Estacion origen = a.getOrigen();
            Estacion destino = a.getDestino();
            String idArista = origen.getId() + "|" + destino.getId();
            Edge arista = graficoGs.addEdge(idArista, origen.getId(), destino.getId());
            if (mostrarTiempos) {
                arista.setAttribute("ui.label", a.getTiempoMin() + " min");
            }
            String color;
            if (!origen.estaActiva() || !destino.estaActiva()) {
                color = COLOR_ARISTA_CERRADA;
            } else if (grupo[origen.getIndice()] == grupo[destino.getIndice()]) {
                color = colorDeGrupo(grupo[origen.getIndice()]);
            } else {
                color = COLOR_TRANSBORDO;
            }
            arista.setAttribute("ui.style", "fill-color: " + color + ";");
            nodoArista = nodoArista.getSiguiente();
        }
        return leyenda;
    }

    /**
     * Quita todos los nodos del dibujo; las aristas se eliminan junto con ellos.
     */
    private void vaciarDibujo() {
        while (graficoGs.getNodeCount() > 0) {
            graficoGs.removeNode(0);
        }
    }

    /**
     * Obtiene el color del grupo indicado, repitiendo la paleta si hay más
     * grupos que colores.
     *
     * @param indiceGrupo posición del grupo (0, 1, 2...), o -1 si no tiene grupo
     * @return el color en formato hexadecimal
     */
    private String colorDeGrupo(int indiceGrupo) {
        if (indiceGrupo < 0) {
            return COLOR_TRANSBORDO;
        }
        return PALETA[indiceGrupo % PALETA.length];
    }

    /**
     * Asigna a cada estación activa el grupo de su subred y agrega a la leyenda
     * una entrada por subred. Las estaciones cerradas no pertenecen a ninguna.
     *
     * @param grafo red de transporte
     * @param grupo arreglo, indexado por la posición de la estación, que se llena
     * @param leyenda lista a la que se agregan las entradas
     */
    private void asignarGruposPorSubred(Grafo grafo, int[] grupo, ListaEnlazada<EntradaLeyenda> leyenda) {
        Nodo<Subred> nodoSubred = grafo.detectarSubredes().getCabeza();
        int indiceGrupo = 0;
        while (nodoSubred != null) {
            Subred subred = nodoSubred.getDato();
            Nodo<Estacion> nodoEstacion = subred.getEstaciones().getCabeza();
            while (nodoEstacion != null) {
                grupo[nodoEstacion.getDato().getIndice()] = indiceGrupo;
                nodoEstacion = nodoEstacion.getSiguiente();
            }
            String texto = "Subred " + subred.getNumero() + " (" + subred.getNumEstaciones()
                    + (subred.getNumEstaciones() == 1 ? " estación)" : " estaciones)");
            leyenda.insertarAlFinal(new EntradaLeyenda(texto, colorDeGrupo(indiceGrupo)));
            indiceGrupo++;
            nodoSubred = nodoSubred.getSiguiente();
        }
    }

    /**
     * Asigna a cada estación activa el grupo de su línea y agrega a la leyenda
     * una entrada por línea, en el orden en que aparecen en la red. Las líneas
     * se comparan sin distinguir mayúsculas.
     *
     * @param grafo red de transporte
     * @param grupo arreglo, indexado por la posición de la estación, que se llena
     * @param leyenda lista a la que se agregan las entradas
     */
    private void asignarGruposPorLinea(Grafo grafo, int[] grupo, ListaEnlazada<EntradaLeyenda> leyenda) {
        ListaEnlazada<String> lineas = new ListaEnlazada<>();
        Nodo<Estacion> actual = grafo.getEstaciones().getCabeza();
        while (actual != null) {
            Estacion e = actual.getDato();
            if (e.estaActiva()) {
                int posicion = posicionDeLinea(lineas, e.getLinea());
                if (posicion < 0) {
                    lineas.insertarAlFinal(e.getLinea());
                    posicion = lineas.getTamano() - 1;
                    String nombre = e.getLinea().isEmpty() ? "Sin línea" : e.getLinea();
                    leyenda.insertarAlFinal(new EntradaLeyenda(nombre, colorDeGrupo(posicion)));
                }
                grupo[e.getIndice()] = posicion;
            }
            actual = actual.getSiguiente();
        }
    }

    /**
     * Busca una línea en la lista de líneas ya vistas.
     *
     * @param lineas líneas ya registradas, sin repetidos
     * @param linea línea buscada
     * @return la posición de la línea en la lista, o -1 si no está
     */
    private int posicionDeLinea(ListaEnlazada<String> lineas, String linea) {
        int posicion = 0;
        Nodo<String> nodo = lineas.getCabeza();
        while (nodo != null) {
            if (nodo.getDato().equalsIgnoreCase(linea)) {
                return posicion;
            }
            posicion++;
            nodo = nodo.getSiguiente();
        }
        return -1;
    }
}
