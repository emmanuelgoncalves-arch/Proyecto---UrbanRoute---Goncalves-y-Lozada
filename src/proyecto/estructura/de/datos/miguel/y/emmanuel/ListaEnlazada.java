package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Lista simplemente enlazada genérica.
 *
 * <p>La lista mantiene una referencia al primer nodo (cabecera) y otra al
 * último (cola). Cada nodo guarda un dato y una referencia al nodo siguiente,
 * el último nodo apunta a {@code null}, que marca el final de la lista. Conservar
 * la referencia al último nodo permite insertar al final en tiempo constante.</p>
 *
 * <p>En Java, las variables de tipo objeto son referencias (el equivalente a los
 * apuntadores), {@code new} reserva memoria dinámica en el heap y el recolector
 * de basura la libera cuando ya no está referenciada. Por ello, eliminar un nodo
 * consiste en desenlazarlo de la lista, además se anula su referencia al
 * siguiente para no conservar enlaces residuales.</p>
 *
 * <p>Los datos se comparan con {@link Object#equals(Object)}. La clase no es
 * segura para su uso concurrente.</p>
 *
 * <p>Complejidad: la consulta de la cabecera y de la cola, el tamaño, la
 * inserción al inicio, al final y después de un nodo, y la eliminación del
 * primero son O(1). El acceso por posición, la búsqueda, la eliminación de un
 * dato y la inserción antes de un nodo son O(n).</p>
 *
 * @param <T> tipo de los datos almacenados
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class ListaEnlazada<T> {

    /** Primer nodo de la lista, o {@code null} si la lista está vacía. */
    private Nodo<T> cabeza;

    /** Último nodo de la lista, o {@code null} si la lista está vacía. */
    private Nodo<T> cola;

    /** Cantidad de elementos almacenados. */
    private int tamano;

    /** Crea una lista vacía. */
    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    // ------------------------------------------------------------------
    // Consultas básicas
    // ------------------------------------------------------------------

    /**
     * Indica si la lista no contiene elementos (primitiva EsVacío).
     *
     * @return {@code true} si la lista está vacía
     */
    public boolean estaVacia() {
        return cabeza == null;
    }

    /**
     * Obtiene la cantidad de elementos de la lista.
     *
     * @return el número de elementos
     */
    public int getTamano() {
        return tamano;
    }

    /**
     * Obtiene el primer nodo para recorrer la lista desde el exterior:
     * <pre>
     * Nodo&lt;T&gt; actual = lista.getCabeza();
     * while (actual != null) {
     *     T dato = actual.getDato();
     *     actual = actual.getSiguiente();
     * }
     * </pre>
     *
     * @return el primer nodo, o {@code null} si la lista está vacía
     */
    public Nodo<T> getCabeza() {
        return cabeza;
    }

    /**
     * Obtiene la posición inicial de la lista (primitiva Primero).
     *
     * @return el primer nodo, o {@code null} si la lista está vacía
     */
    public Nodo<T> primero() {
        return cabeza;
    }

    /**
     * Obtiene la posición final de la lista (primitiva Último).
     *
     * @return el último nodo, o {@code null} si la lista está vacía
     */
    public Nodo<T> ultimo() {
        return cola;
    }

    // ------------------------------------------------------------------
    // Acceso y búsqueda
    // ------------------------------------------------------------------

    /**
     * Lee el dato que ocupa una posición, contada desde cero (primitiva
     * Acceder). Recorre los nodos desde la cabecera, por lo que su costo es O(n).
     *
     * @param indice posición solicitada, entre 0 y {@code getTamano() - 1}
     * @return el dato de esa posición, o {@code null} si el índice no es válido
     */
    public T acceder(int indice) {
        if (indice < 0 || indice >= tamano) {
            return null;
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    /**
     * Busca el primer nodo que almacena un dato igual al indicado (primitiva
     * Buscar).
     *
     * @param dato dato buscado, comparado con {@code equals}
     * @return el primer nodo que lo contiene, o {@code null} si no está en la lista
     */
    public Nodo<T> buscar(T dato) {
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (iguales(actual.getDato(), dato)) {
                return actual;
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /**
     * Indica si la lista contiene un dato igual al indicado.
     *
     * @param dato dato buscado, comparado con {@code equals}
     * @return {@code true} si el dato está en la lista
     */
    public boolean contiene(T dato) {
        return buscar(dato) != null;
    }

    // ------------------------------------------------------------------
    // Inserción
    // ------------------------------------------------------------------

    /**
     * Inserta un dato antes del primer elemento. Costo O(1).
     *
     * @param dato dato a insertar
     */
    public void insertarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cabeza);
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamano++;
    }

    /**
     * Inserta un dato después del último elemento. Costo O(1), porque la lista
     * conserva la referencia al último nodo.
     *
     * @param dato dato a insertar
     */
    public void insertarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo; // lista vacía: el nuevo nodo es también la cabeza
        } else {
            cola.setSiguiente(nuevo); // se engancha detrás del último nodo actual
        }
        cola = nuevo; // en ambos casos el nuevo nodo pasa a ser el último
        tamano++;
    }

    /**
     * Inserta un dato inmediatamente después de un nodo de esta lista
     * (post-inserción). Costo O(1).
     *
     * @param referencia nodo de esta lista después del cual se inserta
     * @param dato dato a insertar
     * @return {@code false} si la referencia es {@code null}; {@code true} en
     *         caso contrario
     */
    public boolean insertarDespuesDe(Nodo<T> referencia, T dato) {
        if (referencia == null) {
            return false;
        }
        Nodo<T> nuevo = new Nodo<>(dato);
        // El orden importa: primero el nuevo apunta al que seguía a la referencia
        // y solo después la referencia apunta al nuevo; al revés se perdería el resto de la lista.
        nuevo.setSiguiente(referencia.getSiguiente());
        referencia.setSiguiente(nuevo);
        if (referencia == cola) {
            cola = nuevo; // se insertó al final: hay nuevo último
        }
        tamano++;
        return true;
    }

    /**
     * Inserta un dato inmediatamente antes de un nodo de esta lista
     * (pre-inserción). Al ser una lista simple, localiza el nodo anterior
     * recorriendo desde la cabecera, por lo que su costo es O(n).
     *
     * @param referencia nodo de esta lista antes del cual se inserta
     * @param dato dato a insertar
     * @return {@code false} si la referencia es {@code null} o no pertenece a
     *         la lista; {@code true} en caso contrario
     */
    public boolean insertarAntesDe(Nodo<T> referencia, T dato) {
        if (referencia == null || cabeza == null) {
            return false;
        }
        if (referencia == cabeza) {
            insertarAlInicio(dato);
            return true;
        }
        // La lista no tiene enlaces hacia atrás: para insertar antes de la referencia
        // se busca el nodo que la precede y se inserta DESPUÉS de ese. La comparación
        // != es de identidad (el mismo nodo), no de contenido.
        Nodo<T> anterior = cabeza;
        while (anterior != null && anterior.getSiguiente() != referencia) {
            anterior = anterior.getSiguiente();
        }
        if (anterior == null) {
            return false; // la referencia no pertenece a esta lista
        }
        return insertarDespuesDe(anterior, dato);
    }

    // ------------------------------------------------------------------
    // Eliminación
    // ------------------------------------------------------------------

    /**
     * Elimina el primer elemento de la lista. Costo O(1).
     *
     * @return el dato eliminado, o {@code null} si la lista estaba vacía
     */
    public T eliminarPrimero() {
        if (cabeza == null) {
            return null;
        }
        Nodo<T> eliminado = cabeza;
        cabeza = eliminado.getSiguiente(); // la cabeza pasa al segundo nodo
        if (cabeza == null) {
            cola = null; // era el único nodo: la lista queda vacía y no tiene último
        }
        eliminado.setSiguiente(null); // se corta el enlace para no dejar referencias colgando
        tamano--;
        return eliminado.getDato();
    }

    /**
     * Elimina la primera aparición de un dato (primitiva Eliminar).
     *
     * @param dato dato a eliminar, comparado con {@code equals}
     * @return {@code true} si el dato estaba en la lista y fue eliminado
     */
    public boolean eliminar(T dato) {
        // Se recorre guardando también el nodo anterior, porque para quitar un nodo
        // hay que cambiar el enlace del que lo precede (la lista solo avanza).
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (iguales(actual.getDato(), dato)) {
                // Se "salta" el nodo actual. Casos:
                //  - era la cabeza (anterior == null): la cabeza pasa al siguiente;
                //  - estaba en medio o al final: el anterior apunta al siguiente.
                if (anterior == null) {
                    cabeza = actual.getSiguiente();
                } else {
                    anterior.setSiguiente(actual.getSiguiente());
                }
                // Si era el último, el nuevo último es el anterior (null si la lista queda vacía).
                if (actual == cola) {
                    cola = anterior;
                }
                actual.setSiguiente(null);
                tamano--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Elimina todos los elementos. Los nodos se desenlazan uno a uno para que
     * ninguno conserve una referencia a otro.
     */
    public void vaciar() {
        Nodo<T> actual = cabeza;
        while (actual != null) {
            Nodo<T> siguiente = actual.getSiguiente();
            actual.setSiguiente(null);
            actual = siguiente;
        }
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /**
     * Compara dos datos con {@code equals}, admitiendo valores nulos.
     *
     * @param a primer dato
     * @param b segundo dato
     * @return {@code true} si ambos son {@code null} o si {@code a.equals(b)}
     */
    private boolean iguales(T a, T b) {
        // Se comprueba null primero porque a.equals(b) fallaría si a fuera null.
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
