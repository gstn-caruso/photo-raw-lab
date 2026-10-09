# Biblioteca: metadatos, facetas y celdas

## Expectativa y responsabilidades

La biblioteca muestra información EXIF real y cuatro filtros combinables sin perder previews, errores ni acciones al ocultar fotos. `PhotoMetadata` conoce los datos opcionales; `LibraryFilter` decide coincidencias y conteos independientes de Swing. `RawMetadataReader` traduce tags en la frontera de archivos. `MetadataFilterPanel` presenta selecciones y `PhotoTile` presenta cada fotografía. El mapa por ruta de `DirectoryMosaicPanel` conserva todas las celdas; el orden y los índices se calculan sobre las visibles.

La rama de trabajo fue `feat/lightroom-library-metadata`, en el checkout original. La línea base ya pasaba `verify`; el mosaico previo sólo mostraba nombre y preview, sin facetas ni metadatos de captura.

## Ciclos y evidencia

| Escenario | Rojo observado | Verde y límites |
| --- | --- | --- |
| Biblioteca vacía y constructor compatible | API `PhotoMetadata`/`LibraryFilter` ausente | `LibraryFilterTest`: vacío sin conteos y constructor anterior con desconocidos |
| EXIF de archivo real | `RawMetadataReader` ausente | `RawMetadataReaderTest`: TIFF temporal con IFD0 y Exif SubIFD; captura, cámara, lente, ISO, apertura, tiempo, focal y dimensiones; `RawDirectory.snapshot` integra los mismos datos |
| Archivo inexistente, corrupto o sin EXIF | Fallback ya implementado con el lector; prueba de regresión verde desde el inicio | Conserva archivos y devuelve desconocidos; modificación no sustituye captura |
| Información en celda | Descripción accesible inexistente | `PhotoTileTest`: datos conocidos/desconocidos, tooltip, nombre, preview 240×180 y acción; `LibraryGridTest` verifica geometría y centrado pintando ambos formatos |
| AND, alternativas, desconocidos y cero coincidencias | API de selección/conteo ausente | `LibraryFilterTest`: conteos consideran las demás selecciones y excluyen sólo la faceta propia |
| Columnas y pie | No existían las cuatro listas | `MetadataFilterPanelTest`: selección/limpieza y conteos visibles; cuatro/dos/una columnas a 1080/650/350, controles enfocables y dentro del área visible |
| Preview tardía oculta | La conservación por ruta ya resolvía este caso | `GalleryControlsTest`: decoder bloqueado mediante latch; filtrar/ordenar durante carga, recibir preview oculta, quitar filtro y conservar el icono; error y acción también conservados |
| Navegación con carga interrumpida | Al volver aparecían 2 celdas en vez de 1 | `GalleryControlsTest`: conserva filtro al reiniciar previews; carpeta nueva limpia filtros y conserva orden. `MosaicLifecycleTest` conserva manejo de resultados obsoletos |
| Fecha EXIF imposible | 31/02 se normalizaba y quedaba como captura conocida | `ResolverStyle.STRICT`; fecha desconocida, cámara y demás tags preservados |
| Corrección de review: tiempo de exposición fiel | La descripción mostraba 0.6 s como 1/2 s | `PhotoTileTest`: 0.8, 0.6 y 0.3 segundos permanecen decimales; 0.008 y 0.125 conservan 1/125 y 1/8; 1.25 y 0.0001234 conservan precisión. Sólo se usa 1/N si reconstruye exactamente el valor leído |

Los archivos TIFF de test se construyen con `ByteBuffer` dentro de la suite Java y se escriben a archivos temporales; no hay mocks del parser ni scripts auxiliares. El fixture KDC distribuido se prueba intacto: extrae `Kodak Digital Science DC50 Zoom Camera`; los otros campos utilizados permanecen desconocidos. Sus dimensiones de miniatura no se presentan como dimensiones originales.

## Verificación reproducible

```text
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify
ruby .github/release_version_test.rb
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify -DlibraryGridScreenshot=/tmp/photo-grid-after.png -DlibraryGridNarrowScreenshot=/tmp/photo-grid-narrow.png
```

`verify` completo y las 5 pruebas Ruby con 7 aserciones pasaron. La ejecución con capturas activó también el test visual opcional. Se inspeccionaron ambas imágenes: ancho 1080 con cuatro facetas y cuatro celdas por fila; ancho 350 con facetas apiladas, una celda por fila y controles de orden/pie sin desbordar. La selección de facetas sólo consulta objetos de dominio ya cargados: no vuelve a leer archivos ni a revelar RAW, y la prueba con decoder bloqueado confirma que sigue disponible durante el trabajo de fondo.

La captura combina una fotografía KDC real con metadatos Nikon sintéticos del test para mostrar campos conocidos y desconocidos; estos datos no se agregan a la aplicación ni al fixture real. Los logs locales de ciclos quedaron en `/tmp/library-*-red.log` y `/tmp/library-*-green.log`; verificaciones completas en `/tmp/photo-grid-verify.log` y `/tmp/photo-grid-final-verify.log`.

La corrección del tiempo de exposición se verificó primero con `PhotoTileTest` y `LibraryGridTest`, y luego con `verify` completo. Evidencia del ciclo en `/tmp/library-exposure-red.log`, `/tmp/library-exposure-green.log` y `/tmp/photo-grid-review-fix-verify.log`.

## Límites

Se verificaron TIFF/EXIF y KDC; no se dispone de fixtures CR3 ni de cada fabricante y sus notas propietarias. La documentación oficial de [metadata-extractor](https://github.com/drewnoakes/metadata-extractor/tree/2.19.0) respalda la dependencia 2.19.0 y su API. Los tags opcionales ausentes no excluyen fotos. No se derivan captura de modificación, dimensiones del tamaño de preview ni valoraciones ficticias. El tipo de archivo usa la extensión. La lectura de EXIF ocurre en el `SwingWorker` del snapshot, antes de publicar las celdas; no se evaluó el rendimiento en carpetas con miles de RAW.

Criterios aplicados: Intention Revealing Selector (#4), `references/004-intention-revealing-selector.md`; Boolean Flag Parameter (#85), `references/085-boolean-flag-parameter.md`, eliminado en commit estructural al separar carpeta nueva de recarga de previews. El dominio conserva independencia de UI/framework conforme al DIGEST de `code-criteria`.
