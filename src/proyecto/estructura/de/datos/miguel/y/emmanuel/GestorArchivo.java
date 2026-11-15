package proyecto.estructura.de.datos.miguel.y.emmanuel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Lee el archivo de texto (CSV) de la red de transporte para llenar un
 * {@link Grafo} y escribe en él el contenido actual de la red.
 *
 * <p><b>Responsabilidad.</b> Esta clase solo se ocupa del contenido del archivo:
 * leerlo, validarlo y escribirlo. No tiene ninguna interfaz gráfica: elegir el
 * archivo con {@link javax.swing.JFileChooser} y mostrar mensajes al usuario es
 * trabajo de {@link PanelCarga}, que le entrega aquí un {@link File} ya elegido.
 * Así esta clase puede usarse y probarse sin ventanas.</p>
 *
 * <p><b>Formato del archivo.</b> Un archivo de texto con dos bloques, cada uno
 * con su encabezado:</p>
 * <pre>
 * ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora[,CERRADA]
 * PRO,Propatria,Linea 1,Catia,12000
 *
 * Estacion_Origen,Estacion_Destino,Tiempo_Traslado_Min
 * PRO,PER,2
 * </pre>
 *
 * <p><b>Columnas.</b> El significado de cada columna está fijado por las
 * constantes {@code COL_*} de esta clase, que se usan tanto al leer como al
 * escribir, de modo que ambos lados no pueden desacordarse:</p>
 * <table border="1">
 *   <caption>Columnas de cada bloque</caption>
 *   <tr><th>Bloque</th><th>Columna</th><th>Constante</th><th>Dato en el programa</th></tr>
 *   <tr><td>Estaciones</td><td>0</td><td>{@code COL_ID}</td><td>{@link Estacion#getId()}</td></tr>
 *   <tr><td>Estaciones</td><td>1</td><td>{@code COL_NOMBRE}</td><td>{@link Estacion#getNombre()}</td></tr>
 *   <tr><td>Estaciones</td><td>2</td><td>{@code COL_LINEA}</td><td>{@link Estacion#getLinea()}</td></tr>
 *   <tr><td>Estaciones</td><td>3</td><td>{@code COL_ZONA}</td><td>{@link Estacion#getZona()}</td></tr>
 *   <tr><td>Estaciones</td><td>4</td><td>{@code COL_CAPACIDAD}</td><td>{@link Estacion#getCapacidadPasajerosHora()}</td></tr>
 *   <tr><td>Estaciones</td><td>5 (opcional)</td><td>{@code COL_MARCA_CERRADA}</td><td>{@code CERRADA} si {@link Estacion#estaActiva()} es falso</td></tr>
 *   <tr><td>Conexiones</td><td>0</td><td>{@code COL_ORIGEN}</td><td>{@link Arista#getOrigen()}</td></tr>
 *   <tr><td>Conexiones</td><td>1</td><td>{@code COL_DESTINO}</td><td>{@link Arista#getDestino()}</td></tr>
 *   <tr><td>Conexiones</td><td>2</td><td>{@code COL_TIEMPO}</td><td>{@link Arista#getTiempoMin()}</td></tr>
 * </table>
 *
 * <p><b>Cómo se lee.</b> El archivo se recorre dos veces: en la primera pasada se
 * cargan las estaciones y en la segunda las conexiones. Así una conexión siempre
 * encuentra sus estaciones ya cargadas, sin importar el orden de los bloques.
 * Cada línea se clasifica por su cantidad de columnas con contenido: 5 o 6 es
 * una estación y 3 es una conexión (ver {@link #tipoDeLinea(String[])}). Se
 * aceptan además líneas en blanco, comentarios que empiezan con {@code #},
 * archivos guardados desde Excel (que completan las filas cortas con comas
 * vacías, como {@code PRO,PER,2,,}) y el punto y coma como separador. Si el
 * archivo solo trae conexiones, las estaciones se crean automáticamente con el
 * id como nombre.</p>
 *
 * <p><b>Tolerancia a errores.</b> Las líneas inválidas se omiten y se reportan
 * como advertencias en el resumen que devuelve {@link #cargar(File, Grafo)}.
 * Solo se lanza {@link IOException} si el archivo no se puede leer o no contiene
 * ningún dato válido.</p>
 *
 * @author Emmanuel Goncalves y Miguel Lozada
 */
public class GestorArchivo {

    /** Ruta, relativa a la carpeta del proyecto, del archivo maestro que se carga al iniciar. */
    public static final String ARCHIVO_MAESTRO = "datos/diccionario.csv";

    /** Encabezado del bloque de estaciones al guardar. Sus columnas siguen el orden de las constantes {@code COL_*} de estaciones. */
    public static final String ENCABEZADO_ESTACIONES = "ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora";

    /** Encabezado del bloque de conexiones al guardar. Sus columnas siguen el orden de las constantes {@code COL_*} de conexiones. */
    public static final String ENCABEZADO_CONEXIONES = "Estacion_Origen,Estacion_Destino,Tiempo_Traslado_Min";

    /** Texto de la columna opcional {@link #COL_MARCA_CERRADA} que indica una estación cerrada. */
    public static final String MARCA_CERRADA = "CERRADA";

    // Posición (contada desde 0) de cada columna en una línea de estación.

    /** Columna del identificador de la estación. */
    private static final int COL_ID = 0;

    /** Columna del nombre de la estación. */
    private static final int COL_NOMBRE = 1;

    /** Columna de la línea de transporte a la que pertenece la estación. */
    private static final int COL_LINEA = 2;

    /** Columna de la zona de la ciudad en la que está la estación. */
    private static final int COL_ZONA = 3;

    /** Columna de la capacidad en pasajeros por hora; debe ser un entero no negativo. */
    private static final int COL_CAPACIDAD = 4;

    /** Columna opcional que vale {@link #MARCA_CERRADA} cuando la estación está cerrada. */
    private static final int COL_MARCA_CERRADA = 5;

    // Posición (contada desde 0) de cada columna en una línea de conexión.

    /** Columna del identificador de una de las estaciones de la conexión. */
    private static final int COL_ORIGEN = 0;

    /** Columna del identificador de la otra estación de la conexión. */
    private static final int COL_DESTINO = 1;

    /** Columna del tiempo de traslado en minutos; debe ser un entero positivo. */
    private static final int COL_TIEMPO = 2;

    /** Cantidad de columnas de una estación sin la marca de cerrada. */
    private static final int COLUMNAS_ESTACION = 5;

    /** Cantidad de columnas de una estación con la marca de cerrada. */
    private static final int COLUMNAS_ESTACION_CERRADA = 6;

    /** Cantidad de columnas de una conexión. */
    private static final int COLUMNAS_CONEXION = 3;

    // Resultados posibles de tipoDeLinea.

    /** La línea tiene el formato de una estación. */
    private static final int TIPO_ESTACION = 1;

    /** La línea tiene el formato de una conexión. */
    private static final int TIPO_CONEXION = 2;

    /** La línea no tiene el formato de ninguno de los dos bloques. */
    private static final int TIPO_INVALIDA = 3;

    /** Máximo de advertencias que se detallan en el resumen de la carga. */
    private static final int MAX_ADVERTENCIAS_EN_RESUMEN = 10;

    /** Último archivo cargado o guardado; es el que actualiza {@link #guardar(Grafo)}. */
    private File archivoFuente;

    /** Crea un gestor sin archivo fuente: todavía no se ha cargado ni guardado ninguno. */
    public GestorArchivo() {
        archivoFuente = null;
    }

    // ------------------------------------------------------------------
    // Auxiliares de lectura
    // ------------------------------------------------------------------

    /**
     * Agrega una advertencia con el número de línea del archivo.
     *
     * @param advertencias lista donde se acumulan las advertencias
     * @param numeroLinea número de línea del archivo, desde 1
     * @param mensaje descripción del problema
     */
    private void advertir(ListaEnlazada<String> advertencias, int numeroLinea, String mensaje) {
        advertencias.insertarAlFinal("Línea " + numeroLinea + ": " + mensaje);
    }

    /**
     * Indica si un campo es el primero de una fila de encabezado, sin distinguir
     * mayúsculas de minúsculas y con o sin tilde.
     *
     * @param primerCampo primer campo de la línea
     * @return {@code true} si la línea es un encabezado
     */
    private boolean esEncabezado(String primerCampo) {
        return primerCampo.equalsIgnoreCase("ID_Estacion")
                || primerCampo.equalsIgnoreCase("ID_Estación")
                || primerCampo.equalsIgnoreCase("Estacion_Origen")
                || primerCampo.equalsIgnoreCase("Estación_Origen");
    }

    /**
     * Separa una línea en sus campos. El separador es la coma; si la línea no
     * tiene ninguna coma pero sí punto y coma, se usa este último, que es el que
     * escribe Excel con la configuración regional en español.
     *
     * <p>Primero se cuentan los separadores para saber cuántos campos hay, y luego
     * se recorre la línea cortando un campo cada vez que se encuentra uno. Los
     * campos vacíos del final se conservan: {@code "PRO,PER,2,,"} da 5 campos, y
     * eso permite reconocer el relleno que agrega Excel.</p>
     *
     * @param texto línea del archivo, sin espacios en los extremos
     * @return los campos sin limpiar
     */
    private String[] dividirLinea(String texto) {
        char separador = ',';
        if (texto.indexOf(',') < 0 && texto.indexOf(';') >= 0) {
            separador = ';';
        }
        int cantidad = 1; // hay un campo más que separadores
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == separador) {
                cantidad++;
            }
        }
        String[] campos = new String[cantidad];
        int inicio = 0;     // dónde empieza el campo actual
        int posicion = 0;   // en qué posición del arreglo se guarda
        for (int i = 0; i <= texto.length(); i++) {
            // Un campo termina en cada separador y al final de la línea.
            if (i == texto.length() || texto.charAt(i) == separador) {
                campos[posicion] = texto.substring(inicio, i);
                posicion++;
                inicio = i + 1;
            }
        }
        return campos;
    }

    /**
     * Cuenta los campos hasta el último que tiene contenido, es decir, ignora los
     * campos vacíos que sobran al final de la línea.
     *
     * @param campos campos de la línea, ya limpios
     * @return la cantidad de campos significativos; 0 si todos están vacíos
     */
    private int contarCamposSignificativos(String[] campos) {
        int cantidad = campos.length;
        while (cantidad > 0 && campos[cantidad - 1].isEmpty()) {
            cantidad--;
        }
        return cantidad;
    }

    /**
     * Quita los espacios y las comillas dobles que envuelven un campo.
     *
     * @param campo texto del campo
     * @return el campo limpio
     */
    private String limpiarCampo(String campo) {
        String s = campo.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * Convierte un texto en un entero no negativo.
     *
     * @param texto texto a convertir
     * @return el entero, o -1 si el texto no es un entero no negativo
     */
    private int enteroNoNegativo(String texto) {
        try {
            int valor = Integer.parseInt(texto.trim());
            return (valor >= 0) ? valor : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Lee todas las líneas de un archivo. Intenta primero con la codificación
     * UTF-8; si aparece el carácter de reemplazo {@code '�'} (que Java
     * escribe cuando encuentra bytes que no son UTF-8 válido, como los acentos de
     * un CSV guardado desde Excel en Windows), lee el archivo de nuevo como
     * ISO-8859-1 para que los acentos y la letra ñ no se dañen.
     *
     * @param archivo archivo a leer
     * @return las líneas, en orden
     * @throws IOException si no se puede leer
     */
    private ListaEnlazada<String> leerLineas(File archivo) throws IOException {
        ListaEnlazada<String> lineas = leerLineas(archivo, StandardCharsets.UTF_8);
        Nodo<String> nodo = lineas.getCabeza();
        while (nodo != null) {
            if (nodo.getDato().indexOf('�') >= 0) {
                return leerLineas(archivo, StandardCharsets.ISO_8859_1);
            }
            nodo = nodo.getSiguiente();
        }
        return lineas;
    }

    /**
     * Lee todas las líneas de un archivo con la codificación indicada.
     *
     * @param archivo archivo a leer
     * @param charset codificación del archivo
     * @return las líneas, en orden
     * @throws IOException si no se puede leer
     */
    private ListaEnlazada<String> leerLineas(File archivo, Charset charset) throws IOException {
        ListaEnlazada<String> lineas = new ListaEnlazada<>();
        // El try con paréntesis cierra el archivo automáticamente al terminar,
        // haya o no un error.
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), charset))) {
            String linea = lector.readLine();
            while (linea != null) {
                lineas.insertarAlFinal(linea);
                linea = lector.readLine();
            }
        }
        return lineas;
    }

    /**
     * Prepara una línea para analizarla: la limpia y la separa en campos.
     *
     * @param texto línea tal como está en el archivo
     * @param numeroLinea número de línea del archivo, desde 1
     * @return los campos limpios, o {@code null} si la línea no contiene datos:
     *         está en blanco, es un comentario ({@code #}), es un encabezado o
     *         solo tiene comas vacías
     */
    private String[] leerCampos(String texto, int numeroLinea) {
        // Algunos editores escriben al inicio del archivo una marca invisible
        // (BOM, ﻿) que se debe quitar para reconocer el encabezado.
        if (numeroLinea == 1 && texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        texto = texto.trim();
        if (texto.isEmpty() || texto.startsWith("#")) {
            return null;
        }
        String[] campos = dividirLinea(texto);
        for (int i = 0; i < campos.length; i++) {
            campos[i] = limpiarCampo(campos[i]);
        }
        if (contarCamposSignificativos(campos) == 0 || esEncabezado(campos[COL_ID])) {
            return null; // fila vacía rellenada con comas (",,,,") o encabezado
        }
        return campos;
    }

    /**
     * Decide si una línea con datos es una estación, una conexión o ninguna.
     *
     * <p>Los editores de hojas de cálculo (Excel) completan cada fila con comas
     * finales hasta el ancho de la fila más larga: {@code PRO,PER,2} se guarda
     * como {@code PRO,PER,2,,}. Por eso se cuentan los campos con contenido y no
     * las columnas del texto. Es conexión si tiene exactamente 3 campos con
     * contenido y, cuando la línea trae relleno, el tercero es un número (el
     * tiempo); eso evita confundirla con una estación incompleta como
     * {@code PRO,Propatria,Linea 1,,}. Si no es conexión y tiene 5 o 6 columnas,
     * es una estación.</p>
     *
     * @param campos campos de la línea, ya limpios
     * @return {@link #TIPO_ESTACION}, {@link #TIPO_CONEXION} o {@link #TIPO_INVALIDA}
     */
    private int tipoDeLinea(String[] campos) {
        boolean pareceConexion = contarCamposSignificativos(campos) == COLUMNAS_CONEXION
                && (campos.length == COLUMNAS_CONEXION || enteroNoNegativo(campos[COL_TIEMPO]) >= 0);
        if (pareceConexion) {
            return TIPO_CONEXION;
        }
        if (campos.length == COLUMNAS_ESTACION || campos.length == COLUMNAS_ESTACION_CERRADA) {
            return TIPO_ESTACION;
        }
        return TIPO_INVALIDA;
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    /**
     * Carga el archivo maestro por defecto ({@link #ARCHIVO_MAESTRO}).
     *
     * @param grafo grafo vacío que se llena
     * @return resumen de la carga con las cantidades y las advertencias
     * @throws IOException si el archivo no existe, no se puede leer o no tiene datos válidos
     */
    public String cargarArchivoMaestro(Grafo grafo) throws IOException {
        return cargar(new File(ARCHIVO_MAESTRO), grafo);
    }

    /**
     * Lee un archivo CSV y llena el grafo (y, por medio de él, su tabla hash).
     *
     * <p>Se hacen dos pasadas por las líneas del archivo. En la primera se
     * agregan las estaciones con {@link Grafo#agregarEstacion(Estacion)} y se
     * reportan las líneas que no tienen un formato válido. En la segunda se
     * agregan las conexiones con {@link Grafo#agregarConexion(String, String, int)},
     * cuando ya están todas las estaciones. Si la carga termina bien, el archivo
     * pasa a ser el archivo fuente de {@link #guardar(Grafo)}.</p>
     *
     * <p>Ejemplos de cómo se clasifica una línea:</p>
     * <ul>
     *   <li>{@code PRO,Propatria,Linea 1,Catia,12000}: estación (5 columnas).</li>
     *   <li>{@code PRO,Propatria,Linea 1,Catia,12000,CERRADA}: estación cerrada (6 columnas).</li>
     *   <li>{@code PRO,PER,2}: conexión (3 columnas).</li>
     *   <li>{@code PRO,PER,2,,}: conexión; las comas finales vacías, que agrega Excel, se ignoran.</li>
     * </ul>
     *
     * @param archivo archivo que se lee
     * @param grafo grafo que debe estar vacío; para reemplazar una red que ya
     *        tiene datos se carga en un grafo nuevo y luego se usa
     *        {@link Grafo#reemplazarContenido(Grafo)}
     * @return resumen de la carga con las cantidades y las advertencias
     * @throws IOException si no se puede leer o no contiene datos válidos
     * @throws IllegalArgumentException si algún argumento es nulo o el grafo no está vacío
     */
    public String cargar(File archivo, Grafo grafo) throws IOException {
        if (archivo == null || grafo == null) {
            throw new IllegalArgumentException("El archivo y el grafo son obligatorios");
        }
        if (grafo.getNumEstaciones() != 0) {
            throw new IllegalArgumentException("El grafo debe estar vacío antes de cargar");
        }
        if (!archivo.isFile() || !archivo.canRead()) {
            throw new IOException("No se puede leer el archivo: " + archivo.getPath());
        }

        ListaEnlazada<String> lineas = leerLineas(archivo);
        ListaEnlazada<String> advertencias = new ListaEnlazada<>();
        int estaciones = 0;
        int lineasDeConexion = 0;

        // Pasada 1: estaciones. Las conexiones solo se cuentan; se procesan en la pasada 2.
        int numeroLinea = 0;
        Nodo<String> nodo = lineas.getCabeza();
        while (nodo != null) {
            numeroLinea++;
            String[] campos = leerCampos(nodo.getDato(), numeroLinea);
            nodo = nodo.getSiguiente();
            if (campos == null) {
                continue;
            }
            int tipo = tipoDeLinea(campos);
            if (tipo == TIPO_ESTACION) {
                if (procesarEstacion(numeroLinea, campos, grafo, advertencias)) {
                    estaciones++;
                }
            } else if (tipo == TIPO_CONEXION) {
                lineasDeConexion++;
            } else {
                advertir(advertencias, numeroLinea, "se esperaban " + COLUMNAS_CONEXION + " columnas (conexión) o "
                        + COLUMNAS_ESTACION + "-" + COLUMNAS_ESTACION_CERRADA + " (estación) y hay "
                        + campos.length);
            }
        }

        // Pasada 2: conexiones. Si el archivo no trae ninguna estación, se crean a
        // partir de los ids que aparecen en las conexiones.
        boolean soloConexiones = (estaciones == 0);
        int autocreadas = 0;
        int conexiones = 0;
        numeroLinea = 0;
        nodo = lineas.getCabeza();
        while (nodo != null) {
            numeroLinea++;
            String[] campos = leerCampos(nodo.getDato(), numeroLinea);
            nodo = nodo.getSiguiente();
            if (campos == null || tipoDeLinea(campos) != TIPO_CONEXION) {
                continue;
            }
            String origen = campos[COL_ORIGEN];
            String destino = campos[COL_DESTINO];
            int tiempo = enteroNoNegativo(campos[COL_TIEMPO]);
            if (origen.isEmpty() || destino.isEmpty()) {
                advertir(advertencias, numeroLinea, "conexión con una estación vacía");
                continue;
            }
            if (tiempo <= 0) {
                advertir(advertencias, numeroLinea, "tiempo de traslado inválido (\"" + campos[COL_TIEMPO]
                        + "\"), debe ser un entero positivo");
                continue;
            }
            if (origen.equals(destino)) {
                advertir(advertencias, numeroLinea, "conexión de una estación consigo misma (" + origen + ")");
                continue;
            }
            if (soloConexiones) {
                autocreadas += crearSiNoExiste(origen, grafo);
                autocreadas += crearSiNoExiste(destino, grafo);
            }
            if (grafo.buscarEstacion(origen) == null || grafo.buscarEstacion(destino) == null) {
                advertir(advertencias, numeroLinea, "la conexión " + origen + " - " + destino
                        + " usa una estación que no está definida");
            } else if (grafo.existeConexion(origen, destino)) {
                advertir(advertencias, numeroLinea, "conexión duplicada " + origen + " - " + destino);
            } else if (grafo.agregarConexion(origen, destino, tiempo)) {
                conexiones++;
            } else {
                advertir(advertencias, numeroLinea, "el grafo rechazó la conexión " + origen + " - " + destino);
            }
        }

        // Un archivo con estaciones pero sin ninguna línea de conexión es válido, pero
        // casi siempre indica que falta el segundo bloque; se avisa para que no pase inadvertido.
        if (estaciones > 0 && lineasDeConexion == 0) {
            advertencias.insertarAlFinal("El archivo no tiene conexiones entre estaciones; "
                    + "el grafo quedará sin aristas (falta el bloque Estacion_Origen,Estacion_Destino,Tiempo).");
        }

        int totalEstaciones = estaciones + autocreadas;
        if (totalEstaciones == 0) {
            String causa = advertencias.estaVacia() ? "" : "\nPrimer problema: " + advertencias.getCabeza().getDato();
            throw new IOException("El archivo \"" + archivo.getName()
                    + "\" no contiene estaciones ni conexiones válidas." + causa);
        }

        archivoFuente = archivo;
        return armarResumen(archivo, totalEstaciones, autocreadas, conexiones, advertencias);
    }

    /**
     * Valida una línea de estación y, si es correcta, la agrega al grafo. Cada
     * dato se toma de la columna que indica la constante {@code COL_*}
     * correspondiente.
     *
     * @param numeroLinea número de línea del archivo
     * @param campos campos de la línea, ya limpios; tiene 5 o 6 elementos
     * @param grafo grafo que se llena
     * @param advertencias lista donde se acumulan las advertencias
     * @return {@code true} si la estación quedó cargada
     */
    private boolean procesarEstacion(int numeroLinea, String[] campos, Grafo grafo,
                                     ListaEnlazada<String> advertencias) {
        String id = campos[COL_ID];
        if (id.isEmpty()) {
            advertir(advertencias, numeroLinea, "estación sin ID");
            return false;
        }
        int capacidad = enteroNoNegativo(campos[COL_CAPACIDAD]);
        if (capacidad < 0) {
            advertir(advertencias, numeroLinea, "capacidad inválida (\"" + campos[COL_CAPACIDAD]
                    + "\") para la estación " + id);
            return false;
        }
        if (grafo.buscarEstacion(id) != null) {
            advertir(advertencias, numeroLinea, "ID duplicado \"" + id + "\" (se conserva la primera aparición)");
            return false;
        }
        // Si el nombre está vacío se usa el id, para que la estación tenga siempre un texto que mostrar.
        String nombre = campos[COL_NOMBRE].isEmpty() ? id : campos[COL_NOMBRE];
        Estacion e = new Estacion(id, nombre, campos[COL_LINEA], campos[COL_ZONA], capacidad);
        if (!grafo.agregarEstacion(e)) {
            advertir(advertencias, numeroLinea, "el grafo rechazó la estación " + id);
            return false;
        }
        if (campos.length == COLUMNAS_ESTACION_CERRADA
                && MARCA_CERRADA.equalsIgnoreCase(campos[COL_MARCA_CERRADA])) {
            grafo.cerrarEstacion(id);
        }
        return true;
    }

    /**
     * Crea una estación con datos por defecto si todavía no existe. Se usa
     * cuando el archivo solo trae conexiones.
     *
     * @param id identificador de la estación
     * @param grafo grafo que se llena
     * @return 1 si la creó, 0 si ya existía o el grafo la rechazó
     */
    private int crearSiNoExiste(String id, Grafo grafo) {
        if (grafo.buscarEstacion(id) != null) {
            return 0;
        }
        Estacion e = new Estacion(id, id, "Sin linea", "Sin zona", 0);
        return grafo.agregarEstacion(e) ? 1 : 0;
    }

    /**
     * Arma el texto con el resultado de la carga.
     *
     * @param archivo archivo cargado
     * @param estaciones total de estaciones cargadas
     * @param autocreadas estaciones creadas automáticamente
     * @param conexiones total de conexiones cargadas
     * @param advertencias advertencias acumuladas
     * @return el resumen
     */
    private String armarResumen(File archivo, int estaciones, int autocreadas, int conexiones,
                                ListaEnlazada<String> advertencias) {
        StringBuilder sb = new StringBuilder();
        sb.append("Archivo cargado: ").append(archivo.getName()).append('\n');
        sb.append("Estaciones: ").append(estaciones);
        if (autocreadas > 0) {
            sb.append(" (").append(autocreadas).append(" creadas automáticamente a partir de las conexiones)");
        }
        sb.append('\n');
        sb.append("Conexiones: ").append(conexiones).append('\n');
        sb.append("Advertencias: ").append(advertencias.getTamano());
        int mostradas = 0;
        Nodo<String> nodo = advertencias.getCabeza();
        while (nodo != null && mostradas < MAX_ADVERTENCIAS_EN_RESUMEN) {
            sb.append("\n  - ").append(nodo.getDato());
            mostradas++;
            nodo = nodo.getSiguiente();
        }
        if (advertencias.getTamano() > mostradas) {
            sb.append("\n  ... y ").append(advertencias.getTamano() - mostradas).append(" más");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Guardado
    // ------------------------------------------------------------------

    /**
     * Obtiene el archivo fuente actual.
     *
     * @return el último archivo cargado o guardado, o {@code null} si no hay
     */
    public File getArchivoFuente() {
        return archivoFuente;
    }

    /**
     * Indica si ya hay un archivo fuente, es decir, si {@link #guardar(Grafo)} puede usarse.
     *
     * @return {@code true} si hay archivo fuente
     */
    public boolean hayArchivoFuente() {
        return archivoFuente != null;
    }

    /**
     * Actualiza el archivo fuente con el contenido actual de la red.
     *
     * @param grafo grafo en memoria
     * @throws IOException si no hay archivo fuente o falla la escritura
     */
    public void guardar(Grafo grafo) throws IOException {
        if (archivoFuente == null) {
            throw new IOException("No hay un archivo fuente definido; use \"Guardar como\".");
        }
        guardarComo(archivoFuente, grafo);
    }

    /**
     * Guarda la red en el archivo indicado, que pasa a ser el archivo fuente.
     *
     * <p>Primero se arma todo el texto en memoria y solo después se abre el
     * archivo para escribirlo de una vez; así, si algo falla mientras se arma el
     * texto, el archivo original no se toca. Las estaciones se escriben en el
     * orden en que están en el grafo y las cerradas llevan la sexta columna
     * {@link #MARCA_CERRADA}. Cada línea se arma con las mismas constantes
     * {@code COL_*} que usa la lectura, por lo que lo que se guarda siempre se
     * puede volver a cargar. El archivo se escribe en UTF-8.</p>
     *
     * @param destino archivo de destino
     * @param grafo grafo en memoria
     * @throws IOException si falla la escritura
     */
    public void guardarComo(File destino, Grafo grafo) throws IOException {
        if (destino == null || grafo == null) {
            throw new IllegalArgumentException("El destino y el grafo son obligatorios");
        }
        StringBuilder texto = new StringBuilder();
        texto.append(ENCABEZADO_ESTACIONES).append('\n');
        Nodo<Estacion> nodoEstacion = grafo.getEstaciones().getCabeza();
        while (nodoEstacion != null) {
            texto.append(armarLineaEstacion(nodoEstacion.getDato())).append('\n');
            nodoEstacion = nodoEstacion.getSiguiente();
        }
        // Línea en blanco que separa los dos bloques.
        texto.append('\n');
        texto.append(ENCABEZADO_CONEXIONES).append('\n');
        Nodo<Arista> nodoArista = grafo.getConexiones().getCabeza();
        while (nodoArista != null) {
            texto.append(armarLineaConexion(nodoArista.getDato())).append('\n');
            nodoArista = nodoArista.getSiguiente();
        }

        // Si la carpeta de destino no existe, se crea.
        File carpeta = destino.getAbsoluteFile().getParentFile();
        if (carpeta != null) {
            carpeta.mkdirs();
        }
        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {
            escritor.write(texto.toString());
        }
        archivoFuente = destino;
    }

    /**
     * Arma la línea de una estación colocando cada dato en su columna.
     *
     * @param e estación que se escribe
     * @return la línea, sin salto de línea; tiene 6 columnas si la estación está
     *         cerrada y 5 en caso contrario
     */
    private String armarLineaEstacion(Estacion e) {
        String[] columnas = new String[e.estaActiva() ? COLUMNAS_ESTACION : COLUMNAS_ESTACION_CERRADA];
        columnas[COL_ID] = campo(e.getId());
        columnas[COL_NOMBRE] = campo(e.getNombre());
        columnas[COL_LINEA] = campo(e.getLinea());
        columnas[COL_ZONA] = campo(e.getZona());
        columnas[COL_CAPACIDAD] = String.valueOf(e.getCapacidadPasajerosHora());
        if (!e.estaActiva()) {
            columnas[COL_MARCA_CERRADA] = MARCA_CERRADA;
        }
        return unirConComas(columnas);
    }

    /**
     * Arma la línea de una conexión colocando cada dato en su columna.
     *
     * @param a arista que representa la conexión
     * @return la línea, sin salto de línea
     */
    private String armarLineaConexion(Arista a) {
        String[] columnas = new String[COLUMNAS_CONEXION];
        columnas[COL_ORIGEN] = campo(a.getOrigen().getId());
        columnas[COL_DESTINO] = campo(a.getDestino().getId());
        columnas[COL_TIEMPO] = String.valueOf(a.getTiempoMin());
        return unirConComas(columnas);
    }

    /**
     * Une los textos de un arreglo poniendo una coma entre uno y otro.
     *
     * @param columnas textos a unir
     * @return los textos separados por comas
     */
    private String unirConComas(String[] columnas) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < columnas.length; i++) {
            if (i > 0) {
                linea.append(',');
            }
            linea.append(columnas[i]);
        }
        return linea.toString();
    }

    /**
     * Evita que comas o saltos de línea dentro de un dato rompan el formato CSV,
     * cambiándolos por espacios.
     *
     * @param valor dato a escribir
     * @return el dato sin comas ni saltos de línea
     */
    private String campo(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(',', ' ').replace('\r', ' ').replace('\n', ' ').trim();
    }
}
