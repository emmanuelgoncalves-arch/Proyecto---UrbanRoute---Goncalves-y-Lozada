package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Panel que muestra el grafo de la red de transporte dentro de la propia
 * aplicación, con las subredes o las líneas identificadas por color.
 *
 * <p>El dibujo se crea la primera vez que se muestra el panel y se sincroniza
 * con la red en memoria cada vez que se invoca {@link #refrescar()}, de modo que
 * refleja las altas, bajas y cierres hechos desde los demás paneles.</p>
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class PanelVisualizacion extends javax.swing.JPanel {

    /** Red de transporte compartida con los demás paneles. */
    private Grafo grafo;

    /** Dibujo del grafo; se crea la primera vez que se necesita. */
    private VisualizadorGrafo visualizador;

    /** Selector del criterio de color (subred o línea). */
    private final javax.swing.JComboBox<String> selectorModo = new javax.swing.JComboBox<>(
            new String[]{"Subred (zonas conectadas)", "Línea de transporte"});

    /** Casilla para escribir el tiempo de traslado sobre cada conexión. */
    private final javax.swing.JCheckBox casillaTiempos = new javax.swing.JCheckBox("Mostrar tiempos");

    /** Botón que vuelve a dibujar la red con los datos actuales. */
    private final javax.swing.JButton botonActualizar = new javax.swing.JButton("Actualizar");

    /** Leyenda de colores: una fila por elemento, en dos columnas. */
    private final javax.swing.JPanel panelLeyenda =
            new javax.swing.JPanel(new java.awt.GridLayout(0, 2, 16, 2));

    /** Mensaje informativo cuando no hay red que mostrar. */
    private final javax.swing.JLabel mensaje = new javax.swing.JLabel(" ");

    /**
     * Crea el panel y prepara los controles. El dibujo en sí se crea al mostrar
     * el panel por primera vez.
     */
    public PanelVisualizacion() {
        initComponents();
        construirControles();
    }

    /**
     * Asocia el panel a la red de transporte compartida.
     *
     * @param grafo red que se dibuja
     */
    public void setGrafo(Grafo grafo) {
        this.grafo = grafo;
    }

    /**
     * Distribuye en el contenedor del grafo la barra de controles, la zona de
     * dibujo y la leyenda, y registra los eventos de los controles.
     */
    private void construirControles() {
        // El diseño generado fija el ancho del contenedor a su tamaño preferido; se
        // reemplaza por un BorderLayout para que el dibujo ocupe todo el espacio
        // disponible y crezca con la ventana.
        removeAll();
        setLayout(new java.awt.BorderLayout(0, 4));
        jLabel1.setFont(jLabel1.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        add(jLabel1, java.awt.BorderLayout.NORTH);
        add(panelContenedorGrafo, java.awt.BorderLayout.CENTER);
        panelContenedorGrafo.setLayout(new java.awt.BorderLayout());

        javax.swing.JPanel barra = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        barra.add(new javax.swing.JLabel("Colorear por:"));
        barra.add(selectorModo);
        barra.add(casillaTiempos);
        barra.add(botonActualizar);
        barra.add(mensaje);
        panelContenedorGrafo.add(barra, java.awt.BorderLayout.NORTH);

        // La leyenda va en un área de altura fija con desplazamiento, para que muchas
        // subredes no deformen el panel.
        javax.swing.JScrollPane desplazamiento = new javax.swing.JScrollPane(panelLeyenda,
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        desplazamiento.setPreferredSize(new java.awt.Dimension(100, 74));
        desplazamiento.setBorder(javax.swing.BorderFactory.createTitledBorder("Leyenda"));
        panelContenedorGrafo.add(desplazamiento, java.awt.BorderLayout.SOUTH);

        selectorModo.addActionListener(evt -> refrescar());
        casillaTiempos.addActionListener(evt -> refrescar());
        botonActualizar.addActionListener(evt -> refrescar());
    }

    /**
     * Sincroniza el dibujo con la red en memoria. Crea el dibujo la primera vez,
     * vuelve a colorear según el criterio elegido y reconstruye la leyenda.
     */
    public void refrescar() {
        if (grafo == null) {
            return;
        }
        if (visualizador == null) {
            visualizador = new VisualizadorGrafo();
            panelContenedorGrafo.add(visualizador.getVista(), java.awt.BorderLayout.CENTER);
        }
        int modo = selectorModo.getSelectedIndex() == 1
                ? VisualizadorGrafo.POR_LINEA : VisualizadorGrafo.POR_SUBRED;
        ListaEnlazada<VisualizadorGrafo.EntradaLeyenda> leyenda =
                visualizador.actualizar(grafo, modo, casillaTiempos.isSelected());

        mensaje.setText(grafo.getNumEstaciones() == 0
                ? "La red está vacía: cargue un archivo desde \"Cargar Archivo\"."
                : grafo.getNumEstaciones() + " estaciones, " + grafo.getNumConexiones() + " conexiones");

        panelLeyenda.removeAll();
        Nodo<VisualizadorGrafo.EntradaLeyenda> nodo = leyenda.getCabeza();
        while (nodo != null) {
            VisualizadorGrafo.EntradaLeyenda entrada = nodo.getDato();
            panelLeyenda.add(crearEntradaLeyenda(entrada.getTexto(),
                    java.awt.Color.decode(entrada.getColorHex())));
            nodo = nodo.getSiguiente();
        }
        panelContenedorGrafo.revalidate();
        panelContenedorGrafo.repaint();
    }

    /**
     * Crea una fila de la leyenda: un cuadrito del color indicado seguido del texto.
     *
     * @param texto descripción del elemento (nombre de la subred o de la línea)
     * @param color color con el que se dibuja ese elemento en el grafo
     * @return el panel con el cuadrito de color y el texto
     */
    private javax.swing.JPanel crearEntradaLeyenda(String texto, java.awt.Color color) {
        // El cuadrito es una etiqueta vacía de 14x14 con el fondo pintado del color.
        javax.swing.JLabel muestra = new javax.swing.JLabel();
        muestra.setOpaque(true);
        muestra.setBackground(color);
        muestra.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.DARK_GRAY));
        muestra.setPreferredSize(new java.awt.Dimension(14, 14));

        javax.swing.JPanel fila = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        fila.add(muestra);
        fila.add(new javax.swing.JLabel(texto));
        return fila;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        panelContenedorGrafo = new javax.swing.JPanel();

        jLabel1.setText("Visualización Interactiva de la Red Multimodal");

        panelContenedorGrafo.setBorder(javax.swing.BorderFactory.createLineBorder(null));

        javax.swing.GroupLayout panelContenedorGrafoLayout = new javax.swing.GroupLayout(panelContenedorGrafo);
        panelContenedorGrafo.setLayout(panelContenedorGrafoLayout);
        panelContenedorGrafoLayout.setHorizontalGroup(
            panelContenedorGrafoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 375, Short.MAX_VALUE)
        );
        panelContenedorGrafoLayout.setVerticalGroup(
            panelContenedorGrafoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 264, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(79, 79, 79))
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(panelContenedorGrafo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelContenedorGrafo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel panelContenedorGrafo;
    // End of variables declaration//GEN-END:variables
}
