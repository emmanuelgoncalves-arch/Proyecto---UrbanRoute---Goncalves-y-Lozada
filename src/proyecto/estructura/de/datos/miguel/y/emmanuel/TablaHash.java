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
 * <p><b>Decisiones de diseño.</b> La tabla combina una función hash que
 * convierte la clave en un índice con el módulo y encadenamiento separado para
 * resolver colisiones. Además, se tomaron estas decisiones propias:</p>
 * <ul>
 *   <li>calcular el hash con el método de Horner y la base 31;</li>
 *   <li>usar un tamaño primo;</li>
 *   <li>comenzar con 101 casillas;</li>
 *   <li>redimensionar al superar un promedio de 0,75 estaciones por lista,
 *   hasta el primer primo mayor que el doble del tamaño actual;</li>
 *   <li>crear las listas de las casillas solo cuando se necesitan.</li>
 * </ul>
 *
 * <p><b>Cómo se eligieron 31 y 101.</b> No salieron de una búsqueda
 * experimental: se eligieron por criterio y convención, y después se
 * comprobó que funcionan bien.</p>
 * <ul>
 *   <li><b>31 (la base):</b> es el valor que usa {@code String.hashCode} en Java,
 *   es un número primo pequeño y mayor que la cantidad de símbolos distintos
 *   que suelen tener los identificadores, y como el cálculo reduce con el módulo
 *   en cada paso, nunca desborda. Es una elección por convención: no hay una
 *   razón matemática que obligue a 31 y no a otro primo similar (ver la
 *   comprobación más abajo).</li>
 *   <li><b>101 (casillas iniciales):</b> es un primo que deja bastante margen
 *   para la red cargada por defecto (44 estaciones, promedio 44/101 = 0,44, muy por
 *   debajo del límite de 0,75; la tabla no crece hasta pasar de 75
 *   estaciones). Se quiso un primo por la razón explicada arriba y se tomó uno
 *   cercano a 100 por ser fácil de razonar, no por una propiedad especial.</li>
 * </ul>
 * <p><b>Comprobación posterior con los 44 identificadores de la red cargada por defecto.</b> Con
 * base 31 y 101 casillas quedan 34 casillas ocupadas y la lista más larga tiene
 * 3 estaciones. Probando otras bases (2, 10, 26, 33, 37, 41, 127, 131) con
 * 101 casillas, el resultado es parecido (35 a 38 casillas ocupadas, listas
 * más largas de 2 o 3), y con otros tamaños cercanos (97, 103, 127, 128) también.
 * Es decir, con solo 44 claves ninguna combinación es claramente mejor que
 * otra; 31 y 101 no son "los óptimos", son una elección razonable cuyo
 * comportamiento se verificó. Con muchas más estaciones (1000 identificadores
 * como {@code E0001} a {@code E1000}) la lista más larga sigue siendo de 2 o 3
 * estaciones, que es lo que importa para que la búsqueda siga siendo O(1) en
 * promedio.</p>
 *
 * <p>Las claves distinguen mayúsculas de minúsculas, y los espacios en los
 * extremos del identificador se ignoran. La clase no es segura para su uso
 * concurrente.</p>
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class TablaHash {

    /**
     * Base de las potencias que ponderan la posición de cada carácter en la
     * función de dispersión. Se eligió 31 por ser un valor convencional (es el
     * que usa {@code String.hashCode} en Java): es primo, mayor que el número de
     * letras del alfabeto, y por eso las potencias de 31 mezclan bien las
     * posiciones. No es un valor "exacto": cualquier primo pequeño funcionaría de
     * forma parecida.
     */
    private static final int BASE = 31;

    /**
     * Cantidad inicial de casillas; es un número primo (101). Con la red de
     * ejemplo (unas 44 estaciones) el promedio de estaciones por lista queda
     * muy por debajo de 0,75, así que la tabla no necesita crecer al inicio.
     */
    private static final int CASILLAS_INICIALES = 101;

    /**
     * Numerador de la fracción 3/4 = 0,75, que es el promedio máximo de
     * estaciones por lista. Se usa una fracción de enteros (y no el decimal
     * 0,75) para comparar sin divisiones ni redondeos; ver {@link #insertar}.
     * El valor 0,75 es un compromiso: más bajo gasta memoria en casillas vacías,
     * más alto alarga las listas.
     */
    private static final int PROMEDIO_MAXIMO_NUMERADOR = 3;

    /** Denominador de la fracción 3/4 descrita en {@link #PROMEDIO_MAXIMO_NUMERADOR}. */
    private static final int PROMEDIO_MAXIMO_DENOMINADOR = 4;

    /**
     * Casillas de la tabla. El índice de cada posición lo calcula
     * {@link #indiceDe(String, int)}. Una posición vale {@code null} mientras
     * ninguna estación haya sido asignada a ella: las listas se crean solo
     * cuando hacen falta, para no gastar memoria en casillas que nunca se usan.
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
        // Se rechaza el duplicado ANTES de insertar: la tabla no admite dos
        // estaciones con el mismo identificador.
        if (e == null || contiene(e.getId())) {
            return false;
        }
        agregarEnLista(casillas, e);
        tamano++;
        // Se redimensiona cuando el promedio (tamano / casillas.length) supera
        // 3/4. La condición "tamano / length > 3/4" es equivalente a
        // "tamano * 4 > length * 3" (se multiplican ambos lados por 4 * length,
        // que es positivo), y así se evitan la división y los decimales.
        // Ejemplo con 101 casillas: con 75 estaciones, 75*4 = 300 no supera
        // 101*3 = 303; con 76, 76*4 = 304 sí lo supera, y se redimensiona.
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
        // Paso 1: la función de dispersión indica en qué casilla DEBERÍA estar la
        // estación, si existe. Nunca hay que mirar otras casillas.
        ListaEnlazada<Estacion> lista = casillas[indiceDe(clave, casillas.length)];
        if (lista == null) {
            return null; // casilla nunca usada: la estación no puede estar
        }
        // Paso 2: en esa casilla pueden convivir varias estaciones (colisión), así
        // que se recorre la lista comparando los identificadores completos.
        Nodo<Estacion> actual = lista.getCabeza();
        while (actual != null) {
            if (actual.getDato().getId().equals(clave)) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null; // se recorrió toda la lista y no está
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
        // La lista de esa casilla existe seguro, porque buscar la encontró. Si queda
        // vacía se deja así (no se vuelve a poner en null): una lista vacía no
        // cambia ningún resultado y evita trabajo extra.
        casillas[indiceDe(e.getId(), casillas.length)].eliminar(e);
        tamano--;
        return e;
    }

    /**
     * Obtiene todas las estaciones registradas. No se garantiza ningún orden: se
     * recorren las casillas de la 0 a la última y, dentro de cada una, su lista
     * de principio a fin, por lo que el orden depende del hash y no del orden de
     * inserción.
     *
     * @return una lista nueva con las estaciones; nunca es {@code null} y su
     *         modificación no afecta a la tabla
     */
    public ListaEnlazada<Estacion> obtenerTodas() {
        ListaEnlazada<Estacion> copia = new ListaEnlazada<>();
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) {
                continue; // casilla sin usar, no hay nada que copiar
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
     * Elimina todas las estaciones y devuelve la tabla a su estado inicial, con
     * la cantidad inicial de casillas.
     */
    public void vaciar() {
        this.casillas = crearArregloDeCasillas(CASILLAS_INICIALES);
        this.tamano = 0;
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
     * tamaño indicado.
     *
     * <p><b>Qué hace, paso a paso.</b> Parte de {@code valor = 0} y, para cada
     * carácter de izquierda a derecha, hace
     * {@code valor = (valor * 31 + código del carácter) % tamañoArreglo}. Al
     * terminar, {@code valor} es el índice. Multiplicar por 31 en cada vuelta
     * "empuja" a los caracteres anteriores hacia una potencia mayor de la base:
     * el último carácter queda con peso 31^0, el penúltimo con 31^1, y así hasta
     * el primero. Es el mismo resultado que la fórmula de la documentación de la
     * clase, pero calculado en una sola pasada (método de Horner).</p>
     *
     * <p><b>Ejemplo</b> con la clave {@code "PRO"} y 101 casillas (los códigos
     * son P=80, R=82, O=79):</p>
     * <pre>
     * vuelta 1: (0  * 31 + 80) % 101 = 80
     * vuelta 2: (80 * 31 + 82) % 101 = 2562 % 101 = 37
     * vuelta 3: (37 * 31 + 79) % 101 = 1226 % 101 = 14   -> índice 14
     * </pre>
     * <p>El orden importa: {@code "AB"} da 61 y {@code "BA"} da 91 con 101
     * casillas.</p>
     *
     * <p><b>Por qué el módulo en cada vuelta.</b> Porque
     * {@code (a * 31 + c) % m} da lo mismo que {@code ((a % m) * 31 + c) % m};
     * reducir en cada paso no cambia el resultado final y mantiene
     * {@code valor} siempre menor que {@code tamañoArreglo}. Así el producto
     * nunca desborda, aunque la clave sea larga. {@code valor} es {@code long}
     * por precaución: el producto {@code valor * 31} cabe en un {@code int}
     * mientras el arreglo sea pequeño, pero con {@code long} no hay que
     * demostrarlo para cualquier tamaño. El {@code (int)} final es seguro porque
     * {@code valor < tamañoArreglo}, que es un {@code int}.</p>
     *
     * <p><b>Por qué la clave es un {@code String}.</b> Porque el identificador
     * de una estación ({@code "PRO"}, {@code "E1001"}) es texto; cada carácter
     * de Java ya es un número (su código Unicode), que es lo que se suma.</p>
     *
     * @param clave identificador, ya sin espacios en los extremos
     * @param tamanoArreglo cantidad de casillas del arreglo; debe ser mayor que 0
     * @return un índice entre 0 y {@code tamanoArreglo - 1}
     */
    private static int indiceDe(String clave, int tamanoArreglo) {
        long valor = 0;
        for (int i = 0; i < clave.length(); i++) {
            // charAt(i) es un char; al sumarlo con un long se convierte
            // automáticamente en su código numérico (p. ej. 'P' -> 80).
            valor = (valor * BASE + clave.charAt(i)) % tamanoArreglo;
        }
        return (int) valor; // seguro: valor < tamanoArreglo
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
            destino[indice] = new ListaEnlazada<>(); // primera estación de esta casilla
        }
        destino[indice].insertarAlFinal(e);
    }

    /**
     * Agranda el arreglo hasta el primer número primo mayor que el doble de su
     * tamaño actual y vuelve a ubicar todas las estaciones, ya que su índice
     * depende del tamaño del arreglo.
     *
     * <p>Pasos: (1) se crea un arreglo nuevo vacío de tamaño primo; (2) se
     * recorre cada lista del arreglo viejo y cada estación se inserta en el
     * nuevo, recalculando su índice con el tamaño nuevo (no se pueden copiar las
     * listas tal cual, porque {@code % 101} y {@code % 211} dan índices
     * distintos para la misma clave); (3) el arreglo nuevo reemplaza al viejo.
     * Con 101 casillas ocurre al insertar la estación 76 (101 pasa a 211); las
     * siguientes veces son 159 (431), 324 (863) y 648 (1733).</p>
     */
    private void redimensionar() {
        ListaEnlazada<Estacion>[] nuevas = crearArregloDeCasillas(siguientePrimo(casillas.length * 2));
        for (int i = 0; i < casillas.length; i++) {
            if (casillas[i] == null) {
                continue;
            }
            Nodo<Estacion> actual = casillas[i].getCabeza();
            while (actual != null) {
                // No se usa insertar(): ya se sabe que no hay duplicados y no se
                // quiere que la cuenta (tamano) ni el chequeo de redimensión se repitan.
                agregarEnLista(nuevas, actual.getDato());
                actual = actual.getSiguiente();
            }
        }
        casillas = nuevas; // tamano no cambia: son las mismas estaciones
    }

    /**
     * Obtiene el menor número primo mayor que el valor indicado. Prueba los
     * números uno por uno a partir de {@code valor + 1} hasta que
     * {@link #esPrimo(int)} acepta alguno. Los primos son frecuentes, así que
     * se encuentra rápido.
     *
     * <p>Ejemplo: {@code siguientePrimo(202)} (que es lo que pide
     * {@link #redimensionar()} cuando hay 101 casillas) prueba 203 (= 7 x 29),
     * 204, 205, 206, 207 (= 9 x 23), 208, 209 (= 11 x 19), 210 y llega a 211,
     * que es primo.</p>
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
     * Indica si un número es primo: un número es primo si no tiene ningún
     * divisor aparte de 1 y de sí mismo. Se prueban los divisores 2, 3, 4...
     * solo mientras {@code divisor * divisor <= n}, es decir, hasta la raíz
     * cuadrada de {@code n}.
     *
     * <p><b>Por qué basta con llegar a la raíz cuadrada.</b> Si {@code n} se
     * puede escribir como {@code a * b}, uno de los dos factores es menor o
     * igual que la raíz cuadrada de {@code n} (si ambos fueran mayores, su
     * producto sería mayor que {@code n}). Entonces, si {@code n} tiene algún
     * divisor, ya se habrá encontrado uno antes de llegar a la raíz; probar más
     * allá solo repetiría pares ya vistos. No se calcula la raíz con
     * {@code Math.sqrt}: la condición {@code divisor * divisor <= n} es la misma
     * comparación sin decimales.</p>
     *
     * <p>Ejemplo con {@code n = 97}: la raíz es aprox. 9,8, así que se prueba
     * 2, 3, 4, 5, 6, 7, 8 y 9; ninguno divide a 97, por lo que es primo. Con
     * {@code n = 203} se prueba 2, 3, 4, 5, 6 y 7; el 7 lo divide (203 = 7 x 29)
     * y se descarta sin probar los números hasta 202. Para el tamaño de esta
     * tabla el costo es despreciable.</p>
     *
     * @param n número que se comprueba
     * @return {@code true} si {@code n} es primo
     */
    private static boolean esPrimo(int n) {
        if (n < 2) {
            return false; // 0 y 1 no son primos por definición
        }
        // El (long) evita que divisor*divisor se desborde cuando n es cercano al
        // máximo de un int; para los tamaños de esta tabla no ocurre, pero así el
        // método es correcto para cualquier n.
        for (int divisor = 2; (long) divisor * divisor <= n; divisor++) {
            if (n % divisor == 0) {
                return false;
            }
        }
        return true;
    }
}
