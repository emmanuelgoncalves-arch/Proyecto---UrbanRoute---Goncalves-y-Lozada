package proyecto.estructura.de.datos.miguel.y.emmanuel;

/**
 * Tabla de dispersión (tabla hash) que indexa las estaciones de la red por su
 * identificador, de modo que puedan localizarse sin recorrer todas las
 * estaciones.
 *
 * <p><b>Estructura.</b> La tabla es un arreglo de casillas (<i>slots</i>). Cada
 * casilla contiene una {@link ListaEnlazada} de estaciones, que se crea la
 * primera vez que la casilla se usa. Para guardar o buscar una estación, la
 * función de dispersión transforma su identificador en un índice del arreglo, y
 * la operación se realiza únicamente sobre la lista de esa casilla.</p>
 *
 * <p><b>Función de dispersión.</b> Cada carácter del identificador se pondera
 * según su posición en el texto, se suman los resultados y la suma se reduce con
 * el operador módulo por el tamaño del arreglo. Como peso se utiliza una
 * potencia de una base fija ({@value #BASE}), de modo que el primer carácter
 * pesa más que el segundo, y así sucesivamente:</p>
 * <pre>
 * indice = ( c1*B^(n-1) + c2*B^(n-2) + ... + cn ) % tamañoDelArreglo
 * </pre>
 * <p>Una suma simple de los códigos de los caracteres produce el mismo valor
 * para identificadores formados por las mismas letras en distinto orden (por
 * ejemplo {@code "AB"} y {@code "BA"}); ponderar cada carácter según su posición
 * evita esa coincidencia. Se usan potencias de una base, y no la posición como
 * multiplicador (1, 2, 3...), porque con multiplicadores lineales las sumas de
 * identificadores parecidos (como {@code "E1001"}, {@code "E1002"}...) quedan en
 * un rango muy estrecho y se agrupan en pocas casillas; con potencias de una
 * base los valores se reparten por todo el arreglo. El cálculo se hace con el
 * método de Horner, en una sola pasada por el texto, y el módulo se aplica en
 * cada paso, de modo que nunca se desborda, por largo que sea el identificador.
 * El tamaño del arreglo es siempre un número primo, porque un módulo primo
 * reduce la posibilidad de que identificadores distintos obtengan el mismo
 * índice.</p>
 *
 * <p><b>Colisiones.</b> Una colisión ocurre cuando dos identificadores distintos
 * producen el mismo índice. Se resuelven con encadenamiento separado
 * (<i>separate chaining</i>, también llamado <i>open hashing</i>): las
 * estaciones que colisionan se guardan en la misma lista, una a continuación de
 * otra. Buscar consiste en calcular el índice y recorrer solo la lista de esa
 * casilla, comparando identificadores.</p>
 *
 * <p><b>Por qué se redimensiona.</b> El costo de una búsqueda es el de recorrer
 * la lista de la casilla elegida; si las claves se reparten de forma uniforme,
 * ese costo depende únicamente del promedio de estaciones por lista, que es la
 * cantidad de estaciones dividida entre la cantidad de casillas. Con un arreglo
 * de tamaño fijo, ese promedio crece de forma proporcional al número de
 * estaciones: a medida que la red crece, las listas se alargan y la búsqueda
 * deja de ser constante para acercarse al peor caso, en el que todas las
 * estaciones están en una misma lista y buscar equivale a recorrerlas todas, es
 * decir, O(n). Aunque el encadenamiento separado tolera tener más estaciones que
 * casillas, el promedio de estaciones por lista debe mantenerse acotado para
 * conservar el costo constante. Por eso, cuando ese promedio supera
 * {@code 0,75}, el arreglo se agranda hasta el primer número primo mayor que el
 * doble de su tamaño y todas las estaciones se vuelven a ubicar: el índice de
 * cada una depende del tamaño del arreglo, por lo que debe recalcularse.
 * Duplicar el tamaño hace que este trabajo ocurra pocas veces, y su costo se
 * reparte entre las inserciones anteriores.</p>
 *
 * <p><b>Complejidad.</b> Con una distribución uniforme, {@code insertar},
 * {@code buscar}, {@code contiene} y {@code eliminar} cuestan O(1) en promedio.
 * En el peor caso (todas las estaciones en la misma lista) cuestan O(n). La
 * inserción que dispara el redimensionamiento cuesta O(n), pero, al repartirse
 * entre las inserciones anteriores, el costo promedio por inserción sigue siendo
 * constante.</p>
 *
 * <p>Las claves distinguen mayúsculas de minúsculas, y los espacios en los
 * extremos del identificador se ignoran. La clase no es segura para su uso
 * concurrente.</p>
 */
public class TablaHash {

    /** Base de las potencias que ponderan la posición de cada carácter en la función de dispersión. */
    private static final int BASE = 31;

    /** Cantidad inicial de casillas; es un número primo. */
    private static final int CASILLAS_INICIALES = 101;

    /**
     * Numerador del promedio máximo de estaciones por lista (3/4) a partir del
     * cual se redimensiona la tabla.
     */
    private static final int PROMEDIO_MAXIMO_NUMERADOR = 3;

    /** Denominador del promedio máximo de estaciones por lista. */
    private static final int PROMEDIO_MAXIMO_DENOMINADOR = 4;

    /**
     * Casillas de la tabla. Una posición vale {@code null} mientras ninguna
     * estación haya sido asignada a ella.
     */
    private ListaEnlazada<Estacion>[] casillas;

    /** Cantidad de estaciones almacenadas. */
    private int tamano;

    /** Crea una tabla vacía con la cantidad inicial de casillas. */
    public TablaHash() {
        this.casillas = crearArregloDeCasillas(CASILLAS_INICIALES);
        this.tamano = 0;
    }

    /**
     * Crea un arreglo de casillas. Java no permite crear arreglos de tipos
     * genéricos directamente ({@code new ListaEnlazada<Estacion>[n]}), por lo
     * que se crea el arreglo del tipo sin parametrizar y se convierte. La
     * conversión es segura porque el arreglo es privado y solo almacena
     * {@code ListaEnlazada<Estacion>}.
     *
     * @param cantidad cantidad de posiciones del arreglo
     * @return un arreglo con todas las posiciones en {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ListaEnlazada<Estacion>[] crearArregloDeCasillas(int cantidad) {
        return (ListaEnlazada<Estacion>[]) new ListaEnlazada[cantidad];
    }

    // ------------------------------------------------------------------
    // Operaciones públicas
    // ------------------------------------------------------------------

    /**
     * Inserta una estación utilizando su identificador como clave. Si tras la
     * inserción el promedio de estaciones por lista supera el máximo, la tabla se
     * redimensiona.
     *
     * @param e estación a insertar
     * @return {@code false} si la estación es nula o ya existe una con el mismo
     *         identificador, {@code true} en caso contrario
     */
    public boolean insertar(Estacion e) {
        if (e == null || contiene(e.getId())) {
            return false;
        }
        agregarEnLista(casillas, e);
        tamano++;
        if (tamano * PROMEDIO_MAXIMO_DENOMINADOR > casillas.length * PROMEDIO_MAXIMO_NUMERADOR) {
            redimensionar();
        }
        return true;
    }

    /**
     * Busca una estación por su identificador. Calcula el índice con la función
     * de dispersión y recorre únicamente la lista de la casilla correspondiente.
     *
     * @param id identificador buscado; se ignoran los espacios en los extremos
     * @return la estación, o {@code null} si no existe o el identificador es nulo
     */
    public Estacion buscar(String id) {
        if (id == null) {
            return null;
        }
        String clave = id.trim();
        ListaEnlazada<Estacion> lista = casillas[indiceDe(clave, casillas.length)];
        if (lista == null) {
            return null;
        }
        Nodo<Estacion> actual = lista.getCabeza();
        while (actual != null) {
            if (actual.getDato().getId().equals(clave)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /**
     * Indica si existe una estación con el identificador dado.
     *
     * @param id identificador buscado; se ignoran los espacios en los extremos
     * @return {@code true} si existe
     */
    public boolean contiene(String id) {
        return buscar(id) != null;
    }

    /**
     * Elimina la estación con el identificador dado.
     *
     * @param id identificador de la estación; se ignoran los espacios en los extremos
     * @return la estación eliminada, o {@code null} si no existía
     */
    public Estacion eliminar(String id) {
        Estacion e = buscar(id);
        if (e == null) {
            return null;
        }
        casillas[indiceDe(e.getId(), casillas.length)].eliminar(e);
        tamano--;
        return e;
    }

    /**
     * Obtiene todas las estaciones registradas. No se garantiza ningún orden.
     *
     * @return una lista nueva con las estaciones; nunca es {@code null} y su
     *         modificación no afecta a la tabla
     */
    public ListaEnlazada<Estacion> obtenerTodas() {
        ListaEnlazada<Estacion> copia = new ListaEnlazada<>();
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) {
                continue;
            }
            Nodo<Estacion> actual = casillas[i].getCabeza();
            while (actual != null) {
                copia.insertarAlFinal(actual.getDato());
                actual = actual.getSiguiente();
            }
        }
        return copia;
    }

    /**
     * Obtiene la cantidad de estaciones registradas.
     *
     * @return el número de estaciones
     */
    public int getTamano() {
        return tamano;
    }

    /**
     * Indica si la tabla no contiene estaciones.
     *
     * @return {@code true} si está vacía
     */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /**
     * Obtiene la cantidad de casillas del arreglo interno.
     *
     * @return la cantidad de casillas, que siempre es un número primo
     */
    public int getCapacidad() {
        return casillas.length;
    }

    // ------------------------------------------------------------------
    // Funcionamiento interno
    // ------------------------------------------------------------------

    /**
     * Función de dispersión: calcula el índice de una clave en un arreglo del
     * tamaño indicado. Recorre la clave de izquierda a derecha y en cada
     * carácter multiplica el valor acumulado por la base y le suma el código del
     * carácter (método de Horner), reduciendo el resultado con el módulo del
     * tamaño en cada paso. El resultado equivale a ponderar cada carácter con
     * una potencia de la base según su posición.
     *
     * @param clave identificador, ya sin espacios en los extremos
     * @param tamanoArreglo cantidad de casillas del arreglo; debe ser mayor que 0
     * @return un índice entre 0 y {@code tamanoArreglo - 1}
     */
    private static int indiceDe(String clave, int tamanoArreglo) {
        long valor = 0;
        for (int i = 0; i < clave.length(); i++) {
            valor = (valor * BASE + clave.charAt(i)) % tamanoArreglo;
        }
        return (int) valor;
    }

    /**
     * Agrega una estación al final de la lista de la casilla que le corresponde
     * en un arreglo de casillas, creando la lista si la casilla todavía no la
     * tiene. No comprueba si el identificador ya está presente.
     *
     * @param destino arreglo de casillas en el que se agrega la estación
     * @param e estación que se agrega
     */
    private void agregarEnLista(ListaEnlazada<Estacion>[] destino, Estacion e) {
        int indice = indiceDe(e.getId(), destino.length);
        if (destino[indice] == null) {
            destino[indice] = new ListaEnlazada<>();
        }
        destino[indice].insertarAlFinal(e);
    }

    /**
     * Agranda el arreglo hasta el primer número primo mayor que el doble de su
     * tamaño actual y vuelve a ubicar todas las estaciones, ya que su índice
     * depende del tamaño del arreglo.
     */
    private void redimensionar() {
        ListaEnlazada<Estacion>[] nuevas = crearArregloDeCasillas(siguientePrimo(casillas.length * 2));
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) {
                continue;
            }
            Nodo<Estacion> actual = casillas[i].getCabeza();
            while (actual != null) {
                agregarEnLista(nuevas, actual.getDato());
                actual = actual.getSiguiente();
            }
        }
        casillas = nuevas;
    }

    /**
     * Obtiene el menor número primo mayor que el valor indicado.
     *
     * @param valor valor de partida
     * @return el primer primo estrictamente mayor que {@code valor}
     */
    private static int siguientePrimo(int valor) {
        int candidato = valor + 1;
        while (!esPrimo(candidato)) {
            candidato++;
        }
        return candidato;
    }

    /**
     * Indica si un número es primo, probando divisores hasta su raíz cuadrada.
     *
     * @param n número que se comprueba
     * @return {@code true} si {@code n} es primo
     */
    private static boolean esPrimo(int n) {
        if (n < 2) {
            return false;
        }
        for (int divisor = 2; (long) divisor * divisor <= n; divisor++) {
            if (n % divisor == 0) {
                return false;
            }
        }
        return true;
    }
}
