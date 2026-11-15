# UrbanRoute — Optimización y Análisis de Redes de Transporte Multimodal

UrbanRoute modela una red de transporte urbano (por ejemplo, el Metro de Caracas) como un grafo ponderado, con una tabla hash propia para indexar las estaciones, y permite cargar la red desde un archivo CSV, modificarla, visualizarla, detectar subredes aisladas, calcular la ruta de menor tiempo con Dijkstra e identificar los hubs de transferencia.

**Integrantes:** Emmanuel Goncalves y Miguel Lozada

**Repositorio:** <https://github.com/emmanuelgoncalves-arch/Proyecto---UrbanRoute---Goncalves-y-Lozada>

## Cómo ejecutarlo

1. Abrir la carpeta `Proyecto Estructura de Datos Miguel y Emmanuel` como proyecto en NetBeans (Java 20 o superior).
2. Verificar que las librerías de la carpeta `lib` (`gs-core-2.0.jar` y `gs-ui-swing-2.0.jar`, de GraphStream) estén en el classpath. El proyecto ya las referencia.
3. Ejecutar *Clean and Build* y luego *Run*. También puede ejecutarse el `.jar` de la carpeta `dist` (su carpeta `dist/lib` debe acompañarlo).
4. Al iniciar, la aplicación carga automáticamente el archivo maestro `datos/diccionario.csv`, que debe estar en la carpeta del proyecto.

## Funcionalidades

- **Cargar archivo:** selección de un `.csv` con `JFileChooser`, con aviso de guardar antes de sobrescribir los datos en memoria.
- **Modificar la red:** agregar estaciones (con sus conexiones y tiempos, opcionalmente), agregar conexiones, y cerrar, reabrir o eliminar estaciones.
- **Guardar cambios:** actualiza el archivo fuente para que los cambios persistan.
- **Visualización:** dibujo del grafo con GraphStream, distinguiendo subredes y líneas, y actualizado tras cada modificación.
- **Subredes:** componentes conexos mediante búsqueda en anchura (BFS).
- **Ruta óptima:** algoritmo de Dijkstra entre dos estaciones.
- **Hubs:** estaciones con mayor número de conexiones directas (centralidad de grado).

## Estructura del código (`src/proyecto/estructura/de/datos/miguel/y/emmanuel`)

| Clase | Responsabilidad |
|---|---|
| `Estacion`, `Arista` | Entidades de la red: vértice y conexión con su tiempo en minutos. |
| `Grafo` | Grafo ponderado no dirigido con listas de adyacencia; BFS, Dijkstra y hubs. Delega la búsqueda de estaciones a `TablaHash`. |
| `TablaHash` | Tabla de dispersión propia con encadenamiento separado (clave: ID de la estación). |
| `ListaEnlazada`, `Nodo` | Lista enlazada propia, base de las listas de adyacencia y de las casillas de la tabla. |
| `Subred`, `Ruta` | Resultados de la detección de subredes y del cálculo de rutas. |
| `GestorArchivo` | Lectura y escritura del CSV; no tiene interfaz. |
| `VisualizadorGrafo` | Dibujo del grafo con GraphStream. |
| `MenuPrincipal` y `Panel*` | Interfaz gráfica (Swing). |

No se usa el *Java Collections Framework*: todas las estructuras de datos son propias. GraphStream se usa únicamente para el renderizado.

## Formato del archivo CSV

El archivo tiene una sección de estaciones (`ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora`) y una de conexiones (`Estacion_Origen,Estacion_Destino,Tiempo_Minutos`). Ver `datos/diccionario.csv` como ejemplo.

## Documentación

- **Javadoc:** carpeta `doc` (abrir `doc/index.html` en el navegador). Se puede regenerar desde NetBeans con *Run → Generate Javadoc*.
- **Diagrama de clases:** archivo `Diagrama de clases UrbanRoute.pdf`.
