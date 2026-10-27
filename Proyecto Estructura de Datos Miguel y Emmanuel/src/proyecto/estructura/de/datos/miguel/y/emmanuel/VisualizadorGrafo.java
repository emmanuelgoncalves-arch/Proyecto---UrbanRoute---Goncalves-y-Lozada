package proyecto.estructura.de.datos.miguel.y.emmanuel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

/**
 * Encargado de renderizar la red de transporte utilizando GraphStream.
 */
public class VisualizadorGrafo {

    // Hoja de estilos CSS integrada para GraphStream
    private static final String ESTILO_GRAFO = 
        "graph {" +
        "   canvas-color: white;" +
        "}" +
        "node {" +
        "   fill-color: #2980b9;" +
        "   size: 20px;" +
        "   text-size: 14px;" +
        "   text-color: #2c3e50;" +
        "   text-background-mode: plain;" +
        "   text-background-color: #ecf0f1;" +
        "   text-padding: 4px;" +
        "   text-alignment: under;" +
        "   stroke-mode: plain;" +
        "   stroke-color: #34495e;" +
        "   stroke-width: 2px;" +
        "}" +
        "node.cerrada {" +
        "   fill-color: #e74c3c;" + // Rojo para estaciones marcadas como CERRADAS
        "}" +
        "edge {" +
        "   fill-color: #95a5a6;" +
        "   size: 2px;" +
        "   text-size: 12px;" +
        "   text-color: #34495e;" +
        "   text-background-mode: plain;" +
        "   text-background-color: white;" +
        "}";

    /**
     * Construye y despliega una ventana independiente con el grafo visualizado.
     * 
     * @param grafoPersonalizado La estructura de datos principal de la red
     */
    public void mostrarVentana(Grafo grafoPersonalizado) {
        if (grafoPersonalizado == null || grafoPersonalizado.getNumEstaciones() == 0) {
            System.err.println("El grafo está vacío o es nulo. No hay nada que visualizar.");
            return;
        }

        // Configuración requerida para inicializar el motor de renderizado Swing en GraphStream
        System.setProperty("org.graphstream.ui", "swing");

        Graph graphStream = new SingleGraph("Red de Transporte Urbano");
        graphStream.setAttribute("ui.stylesheet", ESTILO_GRAFO);
        graphStream.setAttribute("ui.antialias");

        // Convertir Estaciones a Nodos de GraphStream
        ListaEnlazada<Estacion> estaciones = grafoPersonalizado.getEstaciones();
        Nodo<Estacion> actualEstacion = estaciones.getCabeza();

        while (actualEstacion != null) {
            Estacion e = actualEstacion.getDato();
            
            // Se usa el ID como identificador único en GraphStream
            Node nodoUI = graphStream.addNode(e.getId());
            
            // Se muestra el nombre como etiqueta visual
            nodoUI.setAttribute("ui.label", e.getNombre());

            // Si la estación no está activa, aplicamos la clase CSS "cerrada"
            if (!e.estaActiva()) {
                nodoUI.setAttribute("ui.class", "cerrada");
            }
            
            actualEstacion = actualEstacion.getSiguiente();
        }

        // Convertir Aristas a Conexiones de GraphStream
        ListaEnlazada<Arista> conexiones = grafoPersonalizado.getConexiones();
        Nodo<Arista> actualArista = conexiones.getCabeza();

        while (actualArista != null) {
            Arista a = actualArista.getDato();
            String idOrigen = a.getOrigen().getId();
            String idDestino = a.getDestino().getId();
            
            // Generar un ID único para la arista
            String idArista = idOrigen + "-" + idDestino;

            // Validar que la arista no exista previamente (GraphStream lanza error con duplicados)
            if (graphStream.getEdge(idArista) == null && graphStream.getEdge(idDestino + "-" + idOrigen) == null) {
                // Parámetro 'false' indica que la conexión es bidireccional (no dirigida)
                Edge edgeUI = graphStream.addEdge(idArista, idOrigen, idDestino, false);
                edgeUI.setAttribute("ui.label", a.getTiempoMin() + " min");
            }

            actualArista = actualArista.getSiguiente();
        }

        // Renderizar
        Viewer viewer = graphStream.display();
        
        // HIDE_ONLY asegura que al cerrar la ventana del visualizador no se cierre todo el programa (tu MenuPrincipal)
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
        
        // Evita que los nodos se superpongan
        viewer.enableAutoLayout();
    }
}