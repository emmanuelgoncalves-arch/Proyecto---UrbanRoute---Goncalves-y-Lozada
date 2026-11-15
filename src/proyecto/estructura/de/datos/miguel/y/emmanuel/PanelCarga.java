package proyecto.estructura.de.datos.miguel.y.emmanuel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Panel para cargar la red desde un archivo CSV y guardarla de nuevo.
 *
 * <p>Este panel es el único responsable de la interacción con el usuario en
 * todo lo relacionado con archivos: abre los selectores de archivo, pide
 * confirmaciones y muestra los resultados y errores. La lectura y escritura del
 * contenido las hace {@link GestorArchivo}, que no conoce ninguna ventana y solo
 * recibe un {@link File} ya elegido.</p>
 *
 * <p>La red ({@link Grafo}) no es propia del panel: la entrega
 * {@link MenuPrincipal} con {@link #setGrafo(Grafo)} y es la misma que usan los
 * demás paneles. Por eso, al cargar un archivo no se crea otro grafo: se llena
 * uno auxiliar y su contenido se pasa al compartido con
 * {@link Grafo#reemplazarContenido(Grafo)}.</p>
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class PanelCarga extends javax.swing.JPanel {

    /** Carpeta, relativa al proyecto, en la que se abren los selectores si aún no hay archivo fuente. */
    private static final String CARPETA_DATOS = "datos";

    /** Red de transporte compartida con los demás paneles. */
    private Grafo grafo;

    /** Gestor que lee y escribe el archivo de la red. */
    private final GestorArchivo gestor = new GestorArchivo();

    /** Crea el panel; la red se asocia después con {@link #setGrafo(Grafo)}. */
    public PanelCarga() {
        initComponents();
        acomodarComponentes();
        estadoArchivo.setEditable(false);
    }

    /**
     * Asocia el panel a la red de transporte compartida.
     *
     * @param grafo red que se carga y se guarda desde este panel
     */
    public void setGrafo(Grafo grafo) {
        this.grafo = grafo;
    }

    /**
     * Carga el archivo maestro al iniciar la aplicación y muestra el resultado
     * en el área de estado. Si no se puede cargar, lo indica en esa misma área.
     */
    public void cargarMaestroInicial() {
        if (grafo == null) {
            return;
        }
        try {
            // Igual que al cargar un archivo elegido por el usuario: se llena un grafo
            // auxiliar y solo si el archivo es válido reemplaza a la red en memoria.
            Grafo nuevo = new Grafo();
            String resumen = gestor.cargarArchivoMaestro(nuevo);
            grafo.reemplazarContenido(nuevo);
            mostrarEstado(resumen);
        } catch (IOException ex) {
            mostrarEstado("No se pudo cargar el archivo maestro (" + GestorArchivo.ARCHIVO_MAESTRO + "):\n"
                    + ex.getMessage() + "\n\nUse \"Cargar Archivo\" para elegir otro.");
        }
    }

    /**
     * Muestra un texto en el área de estado.
     *
     * @param texto mensaje que se muestra
     */
    private void mostrarEstado(String texto) {
        estadoArchivo.setText(texto);
        estadoArchivo.setCaretPosition(0);
    }

    /**
     * Abre el selector para elegir el archivo que se va a cargar.
     *
     * <p>Es {@code protected} para que una prueba automática pueda sustituirlo y
     * no abrir ventanas.</p>
     *
     * @return el archivo elegido, o {@code null} si el usuario canceló
     */
    protected File elegirArchivoParaCargar() {
        JFileChooser selector = crearSelector("Seleccionar archivo de la red de transporte");
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return selector.getSelectedFile();
        }
        return null;
    }

    /**
     * Abre el selector para elegir dónde guardar la red. Si el nombre escrito no
     * tiene extensión se le agrega {@code .csv}.
     *
     * <p>Es {@code protected} por la misma razón que
     * {@link #elegirArchivoParaCargar()}.</p>
     *
     * @return el archivo de destino, o {@code null} si el usuario canceló
     */
    protected File elegirDestinoParaGuardar() {
        JFileChooser selector = crearSelector("Guardar la red de transporte como");
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File destino = selector.getSelectedFile();
        if (destino.getName().indexOf('.') < 0) {
            destino = new File(destino.getParentFile(), destino.getName() + ".csv");
        }
        return destino;
    }

    /**
     * Crea un selector de archivos que acepta {@code .csv} y {@code .txt}. Se
     * abre en la carpeta del archivo fuente si ya hay uno y, si no, en la
     * carpeta {@link #CARPETA_DATOS} cuando existe.
     *
     * @param titulo título de la ventana del selector
     * @return el selector configurado
     */
    private JFileChooser crearSelector(String titulo) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle(titulo);
        selector.setFileFilter(new FileNameExtensionFilter("Archivos CSV o de texto", "csv", "txt"));
        File fuente = gestor.getArchivoFuente();
        File carpetaDatos = new File(CARPETA_DATOS);
        if (fuente != null && fuente.getAbsoluteFile().getParentFile() != null) {
            selector.setCurrentDirectory(fuente.getAbsoluteFile().getParentFile());
        } else if (carpetaDatos.isDirectory()) {
            selector.setCurrentDirectory(carpetaDatos);
        }
        return selector;
    }

    /**
     * Avisa que al cargar un archivo nuevo se perderán los datos que hay en
     * memoria y permite guardarlos antes.
     *
     * <p>Es {@code protected} para que una prueba automática pueda sustituirlo.</p>
     *
     * @return {@code true} si se puede continuar con la carga
     */
    protected boolean confirmarSobrescritura() {
        if (grafo.getNumEstaciones() == 0) {
            return true;
        }
        Object[] opciones = {"Guardar y cargar", "Cargar sin guardar", "Cancelar"};
        int respuesta = JOptionPane.showOptionDialog(this,
                "Hay una red en memoria. Al cargar un archivo nuevo se sobrescribirán los datos actuales.\n"
                + "Se recomienda guardarlos antes de continuar.",
                "Guardar antes de cargar", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, opciones, opciones[0]);
        if (respuesta == 0) {
            return guardarRepositorio();
        }
        return respuesta == 1;
    }

    /**
     * Guarda la red en el archivo fuente; si todavía no hay uno, pide dónde
     * guardarla.
     *
     * @return {@code true} si la red quedó guardada
     */
    private boolean guardarRepositorio() {
        if (grafo.getNumEstaciones() == 0) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "La red en memoria está vacía; guardarla dejaría el archivo sin datos.\n¿Desea continuar?",
                    "Red vacía", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        try {
            if (gestor.hayArchivoFuente()) {
                gestor.guardar(grafo);
            } else {
                File destino = elegirDestinoParaGuardar();
                if (destino == null) {
                    return false;
                }
                gestor.guardarComo(destino, grafo);
            }
            mostrarEstado("Repositorio actualizado: " + gestor.getArchivoFuente().getName());
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + ex.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
        botonCargarArchivo = new javax.swing.JButton();
        botonActualizarRepo = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        estadoArchivo = new javax.swing.JTextArea();

        jLabel1.setText("Gestión de Archivos y Datos de Red");

        botonCargarArchivo.setText("Cargar Archivo ");
        botonCargarArchivo.addActionListener(this::botonCargarArchivoActionPerformed);

        botonActualizarRepo.setText("Guardar Cambios");
        botonActualizarRepo.addActionListener(this::botonActualizarRepoActionPerformed);

        estadoArchivo.setColumns(20);
        estadoArchivo.setRows(5);
        jScrollPane1.setViewportView(estadoArchivo);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(botonCargarArchivo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(botonActualizarRepo))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(101, 101, 101)
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(79, 79, 79)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(87, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(59, 59, 59)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(botonCargarArchivo)
                    .addComponent(botonActualizarRepo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 70, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Reemplaza la distribución de componentes que genera el diseñador de
     * NetBeans, que coloca cada componente en una posición fija y por eso se
     * descuadra al cambiar el tamaño de la ventana. Aquí se reutilizan los mismos
     * componentes, pero se acomodan con administradores de distribución
     * ({@code BorderLayout}, {@code FlowLayout}) que se adaptan al espacio
     * disponible. Está fuera de la zona generada, así que el diseñador no la
     * sobrescribe.
     */
    private void acomodarComponentes() {
        removeAll();
        setLayout(new BorderLayout(0, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));
        jLabel1.setFont(jLabel1.getFont().deriveFont(Font.BOLD, 16f));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        add(jLabel1, BorderLayout.NORTH);

        // Botones arriba, centrados; el área de estado ocupa todo el espacio restante.
        javax.swing.JPanel botones = new javax.swing.JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        botones.add(botonCargarArchivo);
        botones.add(botonActualizarRepo);
        javax.swing.JPanel centro = new javax.swing.JPanel(new BorderLayout(0, 10));
        centro.add(botones, BorderLayout.NORTH);
        centro.add(jScrollPane1, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // Los mensajes largos (por ejemplo, las advertencias) se parten en varias líneas.
        estadoArchivo.setLineWrap(true);
        estadoArchivo.setWrapStyleWord(true);
        estadoArchivo.setMargin(new Insets(6, 8, 6, 8));
    }

    private void botonCargarArchivoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonCargarArchivoActionPerformed
        if (grafo == null) {
            mostrarEstado("No hay una red asociada a este panel.");
            return;
        }
        if (!confirmarSobrescritura()) {
            return;
        }
        File archivo = elegirArchivoParaCargar();
        if (archivo == null) {
            return;
        }
        try {
            // Se carga primero en un grafo auxiliar: si el archivo no es válido, la red
            // en memoria queda intacta; si lo es, reemplaza a la actual.
            Grafo nuevo = new Grafo();
            String resumen = gestor.cargar(archivo, nuevo);
            grafo.reemplazarContenido(nuevo);
            mostrarEstado(resumen);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el archivo:\n" + ex.getMessage()
                    + "\n\nLa red en memoria no se modificó.", "Error de carga", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_botonCargarArchivoActionPerformed

    private void botonActualizarRepoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonActualizarRepoActionPerformed
        if (grafo == null) {
            mostrarEstado("No hay una red asociada a este panel.");
            return;
        }
        guardarRepositorio();
    }//GEN-LAST:event_botonActualizarRepoActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton botonActualizarRepo;
    private javax.swing.JButton botonCargarArchivo;
    private javax.swing.JTextArea estadoArchivo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
