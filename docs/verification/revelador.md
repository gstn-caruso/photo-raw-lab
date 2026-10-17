# Verificación del revelador

Fecha: 2026-10-16. Rama: `feat/raw-development`.

## Entorno y resultado automatizado

Linux amd64, AMD Ryzen 5 8600G (12 hilos), 32 GB RAM; Temurin 25.0.4,
Maven 3.9.12, LibRaw 0.21.5, Lensfun 0.3.4 y Little CMS 2.17.

```sh
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify
ruby .github/release_version_test.rb
```

Resultado: **BUILD SUCCESS**. 10 pruebas de dominio, 84 de aplicación y una
integración del JAR; 0 fallos, 0 errores y una omisión preexistente de LibraryGrid.
Ruby: 5 pruebas, 7 aserciones, sin fallos.

Cobertura nueva:

- Invariantes de recetas, números no finitos, deshacer/rehacer por gesto y descarte
  de la rama de rehacer; pincel, segmentos, borrador, inversión y gradientes.
- Persistencia exacta de ajustes, geometría, máscaras y snapshot de calibración;
  hash del original inalterado; modificación externa, original cambiado, JSON
  truncado, temporal interrumpido, falta de permisos y publicación fallida.
- Borradores conservados tras un error de escritura y recuperación al reintentar.
- RAW Kodak real: repetibilidad, más de 256 niveles y preview limitado; patrón
  lineal con exposición conserva 1,6 en float; Little CMS convierte el gris de
  0,5 lineal a sRGB dentro de 60 códigos sobre 65535.
- Fabricantes reales, Sony FE 28–70 compatible con ILCE-7M3, búsqueda amplia
  ambigua, cámara desconocida, otras monturas, focal ausente, calibración parcial
  y reapertura del snapshot usando un nuevo contexto nativo.
- Máscaras que mantienen la región bajo recorte/rotación y mapas hacia original.
- TIFF con tres canales de 16 bits y tag ICC; JPEG legible; nombres sin colisiones,
  cancelación sin publicar, error por archivo y continuación del lote.
- Swing: un arrastre sólo guarda al terminar; creación de máscara, deshacer,
  rehacer y reapertura conservan el estado. La suite previa cubre navegación
  obsoleta, cierre durante decode, filtros, selector de archivos y grilla.

Durante la primera ejecución fallaron la selección de cámara Lensfun usando una
búsqueda que requería fabricante, el estado textual esperado por tests anteriores
y una expectativa de rotación en el test nuevo. Se corrigieron la búsqueda mediante
`lf_db_find_cameras_ext`, la compatibilidad del estado de carga y la expectativa de
coordenadas. También se detectó y corrigió un callback tardío al cerrar Swing. Una prueba Swing
específica confirma que una búsqueda automática atrasada no reemplaza el perfil
guardado de la fotografía siguiente.

Artefactos locales ignorados por Git: `app/target/verification/develop-window.png`,
`raw-window.png`, `packaged-window.png` y `sony-zoom.png`.

## RAW suministrados por el usuario

La carpeta contiene 184 ARW de **ILCE-7C (A7C)**: 166 identifican **FE 28–60mm F4–5.6**
y 18 carecen de identificación útil de lente. No son muestras de A7 III / FE 28–70.

Se encontró un error previo en la lectura EXIF: Sony tiene un SubIFD del sensor
antes del SubIFD de captura. Se selecciona ahora el bloque con tags de captura,
con un test específico; focal, apertura, ISO, lente y dimensiones reaparecen.

Se trabajó con copias en `/tmp/photoraw-real-final`; la carpeta original no recibió
sidecars ni exportaciones. Las tres muestras son ARW comprimidos. Receta de prueba:
+0,5 EV global, máscara de pincel local +0,4 EV, recorte 80 %, rotación 90°,
enderezado 1,5°, tres correcciones ópticas habilitadas y perfil automático conservado.

| Muestra | Focal / apertura | Primer preview final | Calibraciones disponibles |
| --- | --- | --- | --- |
| DSC02149.ARW | 28 mm, f/4 | 1,54 s; 1094 × 1638 | Distorsión, TCA, viñeteo |
| DSC02141.ARW | 42 mm, f/5,6 | 1,38 s; 1638 × 1094 | Distorsión, TCA, viñeteo |
| DSC02190.ARW | 60 mm, f/5,6 | 1,27 s; 1638 × 1094 | Distorsión, TCA, viñeteo |

Cada una tuvo una coincidencia Lensfun. Se guardó y reabrió con un repositorio nuevo;
receta y arrays float de preview resultaron exactamente iguales. SHA-256 del original
y su copia después del trabajo:

```text
DSC02149.ARW 40576c900009a86e919932d560fb1cc5f4c2301344e36ad46df1180293223bc4
DSC02141.ARW 8492104a96ad3c81360c340e6ea2bae7bb49ec6b6b3a6f592541debb81b6a290
DSC02190.ARW 2d313fa542da727a62d6b26dd4e5fdb7c7ae3c365fcc2de2fa05881e6b60d4ff
```

DSC02141: render completo y ambas exportaciones en 10,05 s, 4819 × 3219 píxeles
tras encuadre. Pillow 12.1.1 abrió ambos archivos: JPEG RGB con ICC de 6876 bytes;
TIFF RGB, BitsPerSample=(16,16,16), Photometric=2 e ICC de 6876 bytes. Al reducir
el JPEG completo al tamaño de preview con bilineal, la diferencia absoluta media
por canal fue (3,78; 2,44; 3,82) sobre 255. Es una observación de esta foto, no una
garantía de equivalencia píxel a píxel entre resoluciones ni una prueba fotométrica.

El zoom 100 % se comprobó bajo Xvfb sobre la copia de DSC02141, con render completo,
sin error ni bloqueo del EDT. El screenshot de 1280 × 900 quedó en `sony-zoom.png`.

Repetir con un directorio destino nuevo; la herramienta no reemplaza archivos:

```sh
java --enable-native-access=ALL-UNNAMED -Xmx4g \
  -cp app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar tools/ValidateRaw.java \
  '/ruta/a/originales' '/tmp/photoraw-validacion-nueva' \
  DSC02141.ARW DSC02149.ARW DSC02190.ARW
```

La herramienta copia antes de escribir recetas/exportar, comprueba hashes y
repetibilidad, y exporta completo el primer archivo de la lista.

## Referencia de 1000 entradas

Prueba sintética con 1000 enlaces simbólicos hacia los 184 originales, sin copiar ni
modificar las fotografías. Caché del sistema caliente; **no equivale a 1000 capturas
diferentes ni a una medición de disco frío**. Se ejecutó con heap máximo de 2 GB.

- Snapshot y metadatos: 0,481 s, fuera del EDT.
- Construcción de grilla Swing: 0,284 s en EDT.
- 1000 previews secuenciales: 54,419 s, cero errores.
- Heap retenido después de GC: aproximadamente 127 MiB; no incluye toda la memoria
  nativa ni el pico de decode. Las miniaturas aparecen progresivamente.

```sh
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a \
  java --enable-native-access=ALL-UNNAMED -Xmx2g \
  -cp app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar tools/MeasureLibrary.java \
  '/ruta/a/originales'
```

La grilla conserva un componente y miniatura por foto. La caché de revelado contiene
sólo un RAW reducido; los buffers completos son transitorios. Virtualizar la grilla,
procesar exportación por teselas y medir el pico nativo quedan para optimización.

## Debian

`jpackage` generó un `.deb` de aproximadamente 42 MiB con runtime Java y puente C.
Las dependencias declaradas incluyen `libraw23t64`, `liblensfun1`,
`liblensfun-data-v1`, `liblcms2-2` y `x11-xserver-utils`.

Se extrajo sin instalar y se ejecutó `PackagedLauncherIT` bajo Xvfb contra su
lanzador: **BUILD SUCCESS**, RAW Kodak mostrado. Archivo de entrega local:
`target/debian/final/photo-raw-lab_0.1.0_amd64.deb` (versión de desarrollo).
La CI instala ahora los encabezados/compilador, compila el puente dentro del JAR y
comprueba dependencias y lanzador en las releases.

## Límites aún abiertos

- A7 III / FE 28–70 a varias focales/aperturas y ARW sin comprimir no estaban entre
  las muestras aportadas. Su perfil se probó con Lensfun, no con una captura de ese equipo.
- Falta evaluación fotométrica de color/altas luces con carta y RAW controlados;
  los patrones sintéticos no certifican fidelidad de cámara. El balance es relativo,
  no Kelvin calibrado; la salida visible usa sRGB, sin perfil específico de monitor.
- El snapshot se reabrió aislado de la base y no depende de ella para corregir;
  no se reemplazó la base del sistema por otra versión durante estas pruebas.
- Faltan pruebas de corte real de energía y corrupción del filesystem. Se cubrieron
  temporales incompletos, falta de permisos, conflictos y borradores pendientes.
- No se comparó con el motor de Adobe ni se declara equivalencia de resultados.
