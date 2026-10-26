package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Nodo de una lista enlazada: contiene un dato y la referencia al nodo siguiente.
 *
 * <p>Los nodos son la unidad básica con la que {@link ListaEnlazada} enlaza sus
 * elementos. El último nodo de una lista tiene {@code null} como siguiente. Se
 * expone para que los clientes puedan recorrer una lista con
 * {@link ListaEnlazada#getCabeza()} y {@link #getSiguiente()}.</p>
 *
 * @param <T> tipo del dato almacenado
 */
public class Nodo<T> {

    /** Información almacenada en el nodo. */
    private T dato;

    /** Nodo que sigue en la lista, o {@code null} si este es el último. */
    private Nodo<T> siguiente;

    /**
     * Crea un nodo sin nodo siguiente.
     *
     * @param dato información que almacena el nodo
     */
    public Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    /**
     * Obtiene el dato almacenado en el nodo.
     *
     * @return el dato del nodo
     */
    public T getDato() {
        return dato;
    }

    /**
     * Obtiene el nodo que sigue a este en la lista.
     *
     * @return el nodo siguiente, o {@code null} si este es el último
     */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /**
     * Reemplaza el dato almacenado en el nodo.
     *
     * @param dato nuevo dato
     */
    public void setDato(T dato) {
        this.dato = dato;
    }

    /**
     * Establece el nodo al que apunta este.
     *
     * @param siguiente nuevo nodo siguiente, o {@code null} para marcar el final
     */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}
