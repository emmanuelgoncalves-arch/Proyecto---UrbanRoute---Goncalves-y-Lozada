package proyecto.estructura.de.datos.miguel.y.emmanuel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
/**
 * Ventana principal: un menú lateral y, a su derecha, el panel de la función elegida.
 *
 * <p><b>Cómo se conectan los paneles.</b> Esta clase crea una sola red
 * ({@link Grafo}) y se la entrega a cada panel con su método {@code setGrafo}.
 * Todos trabajan sobre ese mismo objeto, por lo que un cambio hecho en un panel
 * (por ejemplo, cerrar una estación en {@link PanelModificacion}) lo ven los
 * demás sin copiar datos. Como los paneles no se avisan entre sí, cada vez que
 * se muestra uno se llama a su {@code refrescar()} (ver {@link #cambiarVista}).</p>
 *
 * <p><b>Origen de los datos.</b> Solo {@link PanelCarga} lee y escribe archivos.
 * Al arrancar, carga el archivo maestro {@value GestorArchivo#ARCHIVO_MAESTRO}
 * en la red compartida; después el usuario puede cargar otro desde ese panel.</p>
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class MenuPrincipal extends javax.swing.JFrame {
    

    /** Administra qué panel se ve en {@link #panelContenedor}: solo uno a la vez. */
    private CardLayout cardLayout;

    /** Red de transporte compartida por todos los paneles. */
    private final Grafo grafo;

    // Paneles que muestran datos derivados de la red; se conservan como campos
    // para llamar a su refrescar() cada vez que se muestran (PanelCarga no
    // necesita refrescarse, por eso no está aquí).

    /** Panel para agregar, quitar y modificar estaciones y conexiones. */
    private PanelModificacion pModificacion;

    /** Panel que dibuja el grafo. */
    private PanelVisualizacion pVisualizacion;

    /** Panel para calcular la ruta más corta entre dos estaciones. */
    private PanelRutas pRutas;

    /** Panel que muestra las subredes y simula fallas. */
    private PanelSubredes pSubredes;

    /** Panel que lista los hubs de transferencia. */
    private PanelHubs pHubs;

    /** Crea la ventana con una red de transporte vacía. */
    public MenuPrincipal() {
        this(new Grafo());
    }

    /**
     * Crea la ventana principal.
     *
     * @param grafo red de transporte que utilizarán todos los paneles
     */
    public MenuPrincipal(Grafo grafo) {
        this.grafo = grafo;
        initComponents();
        initComponentsCustom();
    }

    /**
     * Crea los paneles, les entrega la red compartida, carga el archivo maestro
     * y los registra en el contenedor, cada uno con el nombre que usa
     * {@link #cambiarVista(String)}.
     */
    private void initComponentsCustom() {
        cardLayout = new CardLayout();
        panelContenedor.setLayout(cardLayout);

        PanelCarga pCarga = new PanelCarga();
        pModificacion = new PanelModificacion();
        pVisualizacion = new PanelVisualizacion();
        pSubredes = new PanelSubredes();
        pRutas = new PanelRutas();
        pHubs = new PanelHubs();

        // Todos los paneles reciben el mismo objeto Grafo (no una copia): así lo que
        // uno modifica lo ven los demás. Debe hacerse antes de mostrarlos.
        pModificacion.setGrafo(grafo);
        pVisualizacion.setGrafo(grafo);
        pRutas.setGrafo(grafo);
        pSubredes.setGrafo(grafo);
        pHubs.setGrafo(grafo);
        pCarga.setGrafo(grafo);
        // Llena la red compartida con el archivo maestro; se hace después de entregar
        // el grafo a todos los paneles para que ninguno vea una red a medias.
        pCarga.cargarMaestroInicial();
        
        // El diseñador de NetBeans deja los botones del menú pegados abajo con huecos
        // fijos. Se reacomodan arriba, en una sola columna de igual ancho.
        panelMenuLateral.removeAll();
        panelMenuLateral.setLayout(new BorderLayout());
        panelMenuLateral.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        JPanel columna = new JPanel(new GridLayout(0, 1, 0, 12));
        columna.setOpaque(false);
        columna.add(botonCargar);
        columna.add(botonModificar);
        columna.add(botonVisualizar);
        columna.add(botonSubredes);
        columna.add(botonRutas);
        columna.add(botonHubs);
        panelMenuLateral.add(columna, BorderLayout.NORTH);

        // Tamaño inicial de la ventana (el calculado por pack() resulta insuficiente
        // para los paneles), tamaño mínimo para que no se pueda encoger hasta
        // desordenar los paneles, y centrado en pantalla. Al agrandar la ventana, los
        // paneles se estiran con ella.
        setSize(900, 620);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);

        panelContenedor.add(pCarga, "CARGA");
        panelContenedor.add(pModificacion, "MODIFICAR");
        panelContenedor.add(pVisualizacion, "VISUALIZAR");
        panelContenedor.add(pSubredes, "SUBREDES");
        panelContenedor.add(pRutas, "RUTAS");
        panelContenedor.add(pHubs, "HUBS");
    }

    /**
     * Muestra el panel pedido, actualizándolo antes si muestra datos de la red.
     *
     * @param vista nombre con el que se registró el panel: {@code CARGA},
     *        {@code MODIFICAR}, {@code VISUALIZAR}, {@code SUBREDES},
     *        {@code RUTAS} o {@code HUBS}
     */
    public void cambiarVista(String vista) {
        // Se actualiza el panel antes de mostrarlo, ya que otro panel pudo haber
        // modificado la red.
        if (vista.equals("MODIFICAR")) {
            pModificacion.refrescar();
        } else if (vista.equals("VISUALIZAR")) {
            pVisualizacion.refrescar();
        } else if (vista.equals("RUTAS")) {
            pRutas.refrescar();
        } else if (vista.equals("HUBS")) {
            pHubs.refrescar();
        }
        cardLayout.show(panelContenedor, vista);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelMenuLateral = new javax.swing.JPanel();
        botonCargar = new javax.swing.JButton();
        botonModificar = new javax.swing.JButton();
        botonVisualizar = new javax.swing.JButton();
        botonSubredes = new javax.swing.JButton();
        botonRutas = new javax.swing.JButton();
        botonHubs = new javax.swing.JButton();
        panelContenedor = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        panelMenuLateral.setBackground(new java.awt.Color(204, 204, 204));

        botonCargar.setText("Cargar Archivo");
        botonCargar.addActionListener(this::botonCargarActionPerformed);

        botonModificar.setText("Modificar Red");
        botonModificar.addActionListener(this::botonModificarActionPerformed);

        botonVisualizar.setText("Ver Grafo");
        botonVisualizar.addActionListener(this::botonVisualizarActionPerformed);

        botonSubredes.setText("Subredes y Fallas");
        botonSubredes.addActionListener(this::botonSubredesActionPerformed);

        botonRutas.setText("Rutas Óptimas");
        botonRutas.addActionListener(this::botonRutasActionPerformed);

        botonHubs.setText("Hubs de Transferencia");
        botonHubs.addActionListener(this::botonHubsActionPerformed);

        javax.swing.GroupLayout panelMenuLateralLayout = new javax.swing.GroupLayout(panelMenuLateral);
        panelMenuLateral.setLayout(panelMenuLateralLayout);
        panelMenuLateralLayout.setHorizontalGroup(
            panelMenuLateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMenuLateralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMenuLateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(botonHubs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(botonRutas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(botonSubredes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(botonVisualizar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(botonModificar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(botonCargar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        panelMenuLateralLayout.setVerticalGroup(
            panelMenuLateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMenuLateralLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(botonCargar)
                .addGap(18, 18, 18)
                .addComponent(botonModificar)
                .addGap(18, 18, 18)
                .addComponent(botonVisualizar)
                .addGap(18, 18, 18)
                .addComponent(botonSubredes)
                .addGap(18, 18, 18)
                .addComponent(botonRutas)
                .addGap(18, 18, 18)
                .addComponent(botonHubs)
                .addGap(23, 23, 23))
        );

        getContentPane().add(panelMenuLateral, java.awt.BorderLayout.WEST);

        panelContenedor.setBorder(javax.swing.BorderFactory.createLineBorder(null));
        panelContenedor.setPreferredSize(new java.awt.Dimension(280, 360));
        panelContenedor.setLayout(new java.awt.CardLayout());
        getContentPane().add(panelContenedor, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void botonCargarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonCargarActionPerformed

        cambiarVista("CARGA");
    }//GEN-LAST:event_botonCargarActionPerformed

    private void botonModificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonModificarActionPerformed

        cambiarVista("MODIFICAR");
    }//GEN-LAST:event_botonModificarActionPerformed

    private void botonVisualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonVisualizarActionPerformed

        cambiarVista("VISUALIZAR");
    }//GEN-LAST:event_botonVisualizarActionPerformed

    private void botonSubredesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonSubredesActionPerformed

        cambiarVista("SUBREDES");
    }//GEN-LAST:event_botonSubredesActionPerformed

    private void botonRutasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonRutasActionPerformed

        cambiarVista("RUTAS");
    }//GEN-LAST:event_botonRutasActionPerformed

    private void botonHubsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonHubsActionPerformed

        cambiarVista("HUBS");
    }//GEN-LAST:event_botonHubsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton botonCargar;
    private javax.swing.JButton botonHubs;
    private javax.swing.JButton botonModificar;
    private javax.swing.JButton botonRutas;
    private javax.swing.JButton botonSubredes;
    private javax.swing.JButton botonVisualizar;
    private javax.swing.JPanel panelContenedor;
    private javax.swing.JPanel panelMenuLateral;
    // End of variables declaration//GEN-END:variables
}
