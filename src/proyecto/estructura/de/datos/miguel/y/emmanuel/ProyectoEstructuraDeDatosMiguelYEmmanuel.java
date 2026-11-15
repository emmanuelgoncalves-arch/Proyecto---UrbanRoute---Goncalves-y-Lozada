package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Punto de entrada de la aplicación. Crea la red de transporte y abre la
 * ventana principal, que la comparte con los paneles.
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class ProyectoEstructuraDeDatosMiguelYEmmanuel {

    /** Esta clase solo contiene el método {@code main}; no se instancia. */
    private ProyectoEstructuraDeDatosMiguelYEmmanuel() {
    }

    /**
     * Inicia la aplicación.
     *
     * @param args argumentos de la línea de comandos; no se utilizan
     */
    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            // Nimbus es solo estético: si no se puede aplicar, la aplicación sigue con
            // el aspecto por defecto de Java. Se ignora el error a propósito (no hay
            // consola donde informarlo y no afecta el funcionamiento).
        }

        // La red de transporte comienza vacía y es compartida por todos los paneles.
        Grafo grafo = new Grafo();

        java.awt.EventQueue.invokeLater(() -> new MenuPrincipal(grafo).setVisible(true));
    }
}
