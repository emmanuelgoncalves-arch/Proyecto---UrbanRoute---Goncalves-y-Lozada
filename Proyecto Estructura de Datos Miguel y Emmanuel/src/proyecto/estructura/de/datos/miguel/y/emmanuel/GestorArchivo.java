package proyecto.estructura.de.datos.miguel.y.emmanuel;
 
import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
 
/**
 * Selección del archivo con
 * {@link JFileChooser}, lectura del CSV para llenar un {@link Grafo} y escritura
 * del archivo fuente con el contenido actual de la red.
 *
 * <h2>Formato del archivo</h2>
 * Un archivo de texto con dos bloques, cada uno con su encabezado:
 * <pre>
 * ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora[,CERRADA]
 * PRO,Propatria,Linea 1,Catia,12000
 *
 * Estacion_Origen,Estacion_Destino,Tiempo_Traslado_Min
 * PRO,PER,2
 * </pre>
 * Cada línea se clasifica por su cantidad de columnas (5 o 6: estación; 3:
 * conexión), por lo que se aceptan los dos bloques en cualquier orden, cualquiera
 * de los nombres de encabezado del enunciado ({@code Tiempo_Minutos} o
 * {@code Tiempo_Traslado_Min}), líneas en blanco, comentarios que empiezan con
 * {@code #}. Si el archivo solo trae
 * conexiones, las estaciones se crean automáticamente con el id como nombre.
 *
 * <h2>Tolerancia a errores</h2>
 * Las líneas inválidas se omiten y se reportan como advertencias en el resumen
 * que devuelve {@link #cargar(File, Grafo)}. Solo se lanza {@link IOException} si
 * el archivo no se puede leer o no contiene ningún dato válido.
 */
public class GestorArchivo {
 
    /** Ruta de la carpeta del proyecto, del archivo maestro que se carga al iniciar. */
    public static final String ARCHIVO_MAESTRO = "datos/diccionario.csv";
 
    /** Encabezado del bloque de estaciones al guardar. */
    public static final String ENCABEZADO_ESTACIONES = "ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora";
 
    /** Encabezado del bloque de conexiones al guardar. */
    public static final String ENCABEZADO_CONEXIONES = "Estacion_Origen,Estacion_Destino,Tiempo_Traslado_Min";
 
    /** Marca opcional (sexta columna) de una estación cerrada. */
    public static final String MARCA_CERRADA = "CERRADA";
 
    /** Máximo de advertencias que se detallan en el resumen de la carga. */
    private static final int MAX_ADVERTENCIAS_EN_RESUMEN = 10;
 
    /** Último archivo cargado o guardado; es el que actualiza {@link #guardar(Grafo)}. */
    private File archivoFuente;
 
    /** Conexión leída del archivo, pendiente de insertarse cuando todas las estaciones estén cargadas. */
    private static class ConexionPendiente {
        final int numeroLinea;
        final String origen;
        final String destino;
        final int tiempoMin;
 
        ConexionPendiente(int numeroLinea, String origen, String destino, int tiempoMin) {
            this.numeroLinea = numeroLinea;
            this.origen = origen;
            this.destino = destino;
            this.tiempoMin = tiempoMin;
        }
    }
 
    // Selección de archivos
 
    /**
     * Muestra un {@link JFileChooser} para elegir el archivo que se va a cargar.
     *
     * @param padre componente sobre el que se centra el diálogo; puede ser {@code null}
     * @return el archivo elegido, o {@code null} si el usuario canceló
     */
    public File seleccionarArchivo(Component padre) {
        JFileChooser selector = crearSelector("Cargar red de transporte");
        int resultado = selector.showOpenDialog(padre);
        return (resultado == JFileChooser.APPROVE_OPTION) ? selector.getSelectedFile() : null;
    }
 
    /**
     * Muestra un {@link JFileChooser} para elegir dónde guardar la red. Si el
     * nombre no tiene extensión se le agrega {@code .csv}.
     *
     * @param padre componente sobre el que se centra el diálogo; puede ser {@code null}
     * @return el archivo de destino, o {@code null} si el usuario canceló
     */
    public File seleccionarDestino(Component padre) {
        JFileChooser selector = crearSelector("Guardar red de transporte");
        int resultado = selector.showSaveDialog(padre);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File destino = selector.getSelectedFile();
        if (destino.getName().indexOf('.') < 0) {
            destino = new File(destino.getParentFile(), destino.getName() + ".csv");
        }
        return destino;
    }
 
    /**
     * Crea un selector de archivos que abre en la carpeta del archivo fuente, o
     * en la carpeta {@code datos} si no hay archivo fuente.
     *
     * @param titulo título de la ventana
     * @return el selector configurado
     */
    private JFileChooser crearSelector(String titulo) {
        File carpeta = new File("datos");
        if (archivoFuente != null && archivoFuente.getAbsoluteFile().getParentFile() != null) {
            carpeta = archivoFuente.getAbsoluteFile().getParentFile();
        }
        JFileChooser selector = new JFileChooser(carpeta.isDirectory() ? carpeta : new File("."));
        selector.setDialogTitle(titulo);
        selector.setFileFilter(new FileNameExtensionFilter("Archivos CSV o de texto (*.csv, *.txt)", "csv", "txt"));
        return selector;
    }

    // Auxiliares de lectura
 
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
     * Detecta las filas de encabezado de cualquiera de los dos bloques, sin
     * distinguir mayúsculas ni tildes.
     *
     * @param primerCampo primer campo de la línea
     * @return {@code true} si la línea es un encabezado
     */
    private boolean esEncabezado(String primerCampo) {
        String p = Normalizer.normalize(primerCampo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return p.equals("id_estacion") || p.equals("estacion_origen");
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
     * Lee todas las líneas de un archivo.
     *
     * @param archivo archivo a leer
     * @return las líneas, en orden
     * @throws IOException si no se puede leer
     */
    private ListaEnlazada<String> leerLineas(File archivo) throws IOException {
        try {
            return leerLineas(archivo, StandardCharsets.UTF_8);
        } catch (CharacterCodingException ex) {
            return leerLineas(archivo, StandardCharsets.ISO_8859_1);
        }
    }
 
    /**
     * Lee todas las líneas de un archivo con una codificación estricta: si
     * encuentra un byte inválido, lanza {@link CharacterCodingException}.
     *
     * @param archivo archivo a leer
     * @param charset codificación a usar
     * @return las líneas, en orden
     * @throws IOException si no se puede leer o la codificación no corresponde
     */
    private ListaEnlazada<String> leerLineas(File archivo, Charset charset) throws IOException {
        CharsetDecoder decodificador = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ListaEnlazada<String> lineas = new ListaEnlazada<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), decodificador))) {
            String linea = lector.readLine();
            while (linea != null) {
                lineas.insertarAlFinal(linea);
                linea = lector.readLine();
            }
        }
        return lineas;
    }
    
    // Carga
 
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
     * <p>Primero se clasifica y valida cada línea: las estaciones se agregan de
     * inmediato con {@link Grafo#agregarEstacion(Estacion)} y las conexiones se
     * guardan como pendientes. Cuando ya están todas las estaciones se agregan
     * las conexiones con {@link Grafo#agregarConexion(String, String, int)}, de
     * modo que el orden de las líneas en el archivo no importa. Si la carga
     * termina bien, el archivo pasa a ser el archivo fuente de
     * {@link #guardar(Grafo)}.</p>
     *
     * @param archivo archivo que se lee
     * @param grafo grafo que debe estar vacío
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
        ListaEnlazada<ConexionPendiente> pendientes = new ListaEnlazada<>();
        int estaciones = 0;
 
        // Paso 1: clasificar y validar cada línea.
        int numeroLinea = 0;
        Nodo<String> nodo = lineas.getCabeza();
        while (nodo != null) {
            numeroLinea++;
            String texto = nodo.getDato();
            nodo = nodo.getSiguiente();
            if (numeroLinea == 1 && texto.startsWith("\uFEFF")) {
                texto = texto.substring(1);
            }
            texto = texto.trim();
            if (texto.isEmpty() || texto.startsWith("#")) {
                continue;
            }
            String[] campos = texto.split(",", -1);
            for (int i = 0; i < campos.length; i++) {
                campos[i] = limpiarCampo(campos[i]);
            }
            if (esEncabezado(campos[0])) {
                continue;
            }
            if (campos.length == 5 || campos.length == 6) {
                if (procesarEstacion(numeroLinea, campos, grafo, advertencias)) {
                    estaciones++;
                }
            } else if (campos.length == 3) {
                int tiempo = enteroNoNegativo(campos[2]);
                if (campos[0].isEmpty() || campos[1].isEmpty()) {
                    advertir(advertencias, numeroLinea, "conexión con una estación vacía");
                } else if (tiempo <= 0) {
                    advertir(advertencias, numeroLinea, "tiempo de traslado inválido (\"" + campos[2]
                            + "\"), debe ser un entero positivo");
                } else if (campos[0].equals(campos[1])) {
                    advertir(advertencias, numeroLinea, "conexión de una estación consigo misma (" + campos[0] + ")");
                } else {
                    pendientes.insertarAlFinal(new ConexionPendiente(numeroLinea, campos[0], campos[1], tiempo));
                }
            } else {
                advertir(advertencias, numeroLinea, "se esperaban 3 columnas (conexión) o 5-6 (estación) y hay "
                        + campos.length);
            }
        }
 
        // Agregar las conexiones, ya con todas las estaciones cargadas.
        boolean soloConexiones = (estaciones == 0);
        int autocreadas = 0;
        int conexiones = 0;
        Nodo<ConexionPendiente> actual = pendientes.getCabeza();
        while (actual != null) {
            ConexionPendiente p = actual.getDato();
            actual = actual.getSiguiente();
            if (soloConexiones) {
                autocreadas += crearSiNoExiste(p.origen, grafo);
                autocreadas += crearSiNoExiste(p.destino, grafo);
            }
            if (grafo.buscarEstacion(p.origen) == null || grafo.buscarEstacion(p.destino) == null) {
                advertir(advertencias, p.numeroLinea, "la conexión " + p.origen + " - " + p.destino
                        + " usa una estación que no está definida");
            } else if (grafo.existeConexion(p.origen, p.destino)) {
                advertir(advertencias, p.numeroLinea, "conexión duplicada " + p.origen + " - " + p.destino);
            } else if (grafo.agregarConexion(p.origen, p.destino, p.tiempoMin)) {
                conexiones++;
            } else {
                advertir(advertencias, p.numeroLinea, "el grafo rechazó la conexión " + p.origen + " - " + p.destino);
            }
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
     * Valida una línea de estación y, si es correcta, la agrega al grafo.
     *
     * @param numeroLinea número de línea del archivo
     * @param campos campos de la línea, ya limpios
     * @param grafo grafo que se llena
     * @param advertencias lista donde se acumulan las advertencias
     * @return {@code true} si la estación quedó cargada
     */
    private boolean procesarEstacion(int numeroLinea, String[] campos, Grafo grafo,
                                     ListaEnlazada<String> advertencias) {
        String id = campos[0];
        if (id.isEmpty()) {
            advertir(advertencias, numeroLinea, "estación sin ID");
            return false;
        }
        int capacidad = enteroNoNegativo(campos[4]);
        if (capacidad < 0) {
            advertir(advertencias, numeroLinea, "capacidad inválida (\"" + campos[4] + "\") para la estación " + id);
            return false;
        }
        if (grafo.buscarEstacion(id) != null) {
            advertir(advertencias, numeroLinea, "ID duplicado \"" + id + "\" (se conserva la primera aparición)");
            return false;
        }
        String nombre = campos[1].isEmpty() ? id : campos[1];
        Estacion e = new Estacion(id, nombre, campos[2], campos[3], capacidad);
        if (!grafo.agregarEstacion(e)) {
            advertir(advertencias, numeroLinea, "el grafo rechazó la estación " + id);
            return false;
        }
        if (campos.length == 6 && MARCA_CERRADA.equalsIgnoreCase(campos[5])) {
            grafo.cerrarEstacion(id);
        }
        return true;
    }
 
    /**
     * Crea una estación con datos por defecto si todavía no existe.
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
    
    // Guardado
 
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
     * <p>La escritura es segura: primero se escribe un archivo temporal
     * ({@code .tmp}) y solo cuando está completo reemplaza al destino, así un
     * fallo a mitad no deja el archivo original dañado. Las estaciones se
     * escriben en el orden en que están en el grafo y las cerradas llevan la
     * sexta columna {@link #MARCA_CERRADA}.</p>
     *
     * @param destino archivo de destino
     * @param grafo grafo en memoria
     * @throws IOException si falla la escritura
     */
    public void guardarComo(File destino, Grafo grafo) throws IOException {
        if (destino == null || grafo == null) {
            throw new IllegalArgumentException("El destino y el grafo son obligatorios");
        }
        Path ruta = destino.toPath().toAbsolutePath();
        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        Path temporal = ruta.resolveSibling(ruta.getFileName().toString() + ".tmp");
        try (BufferedWriter escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
            escritor.write(ENCABEZADO_ESTACIONES);
            escritor.newLine();
            Nodo<Estacion> nodoEstacion = grafo.getEstaciones().getCabeza();
            while (nodoEstacion != null) {
                Estacion e = nodoEstacion.getDato();
                escritor.write(campo(e.getId()) + "," + campo(e.getNombre()) + "," + campo(e.getLinea()) + ","
                        + campo(e.getZona()) + "," + e.getCapacidadPasajerosHora()
                        + (e.estaActiva() ? "" : "," + MARCA_CERRADA));
                escritor.newLine();
                nodoEstacion = nodoEstacion.getSiguiente();
            }
            escritor.newLine();
            escritor.write(ENCABEZADO_CONEXIONES);
            escritor.newLine();
            Nodo<Arista> nodoArista = grafo.getConexiones().getCabeza();
            while (nodoArista != null) {
                Arista a = nodoArista.getDato();
                escritor.write(campo(a.getOrigen().getId()) + "," + campo(a.getDestino().getId()) + ","
                        + a.getTiempoMin());
                escritor.newLine();
                nodoArista = nodoArista.getSiguiente();
            }
        } catch (IOException ex) {
            Files.deleteIfExists(temporal);
            throw ex;
        }
        try {
            Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
        }
        archivoFuente = destino;
    }
 
    /**
     * Evita que comas o saltos de línea dentro de un dato rompan el formato CSV.
     *
     * @param valor dato a escribir
     * @return el dato sin comas ni saltos de línea
     */
    private String campo(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("\\s*[,\\r\\n]+\\s*", " ").trim();
    }
}