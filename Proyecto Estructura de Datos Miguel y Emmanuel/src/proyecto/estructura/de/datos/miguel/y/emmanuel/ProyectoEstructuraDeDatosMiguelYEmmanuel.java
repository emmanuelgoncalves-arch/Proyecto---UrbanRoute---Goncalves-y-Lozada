/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Punto de entrada de la aplicación. Crea la red de transporte y abre la
 * ventana principal, que la comparte con los paneles.
 *
 * @author Windows 10
 */
public class ProyectoEstructuraDeDatosMiguelYEmmanuel {

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
        }

        // La red de transporte comienza vacía y es compartida por todos los paneles.
        Grafo grafo = new Grafo();

        java.awt.EventQueue.invokeLater(() -> new MenuPrincipal(grafo).setVisible(true));
    }
}
