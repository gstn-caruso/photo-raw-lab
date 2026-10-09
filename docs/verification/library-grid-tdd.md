# Grilla de biblioteca

Referencia visual: [Biblioteca Lightroom](https://www.youtube.com/watch?v=F5yy-XpLXOs), captura local `/tmp/photo-raw-lab-grid-reference.png`. Alcance: grilla, nombres y previews; sin barras laterales ni herramientas de clasificación.

Responsabilidades: `DirectoryMosaicPanel` conecta rutas, previews y acciones; su grilla Swing decide columnas, geometría y scroll según el viewport. El dominio no cambia.

Línea base: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify` verde (2 tests dominio, 50 app, 1 integración).

## Escenario 1: geometría adaptable

Expectativa: celdas de 264 × 232, alineadas arriba/izquierda; reducir el ancho cambia de tres a dos columnas conservando altura; última fila y foto única no se estiran; scroll vertical cuando hay suficientes filas.

Red: `LibraryGridTest.resizeReflowsColumnsWithoutStretchingRowsOrTheLastPhoto` falla: esperaba 264 × 232 y recibió 282 × 215. Green: el mismo test pasa con grilla `Scrollable`; verifica dos anchos, siete fotos y una foto.

Comando focal: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B -pl app -am test -Dtest=LibraryGridTest -Dsurefire.failIfNoSpecifiedTests=false`.

Criterio: RDD, responsabilidades de UI en el interfacer; Intention Revealing Selector (#4). No fue necesario un tidying previo.

## Escenario 2: composición de la celda

Expectativa: nombre encima de una región fija de 240 × 180; previews apaisada y vertical centradas sin cambiar dimensiones ni proporciones.

Red: `filenameSitsAboveAFixedPreviewAreaWithBothOrientationsCentered` esperaba un área de 180 píxeles de altura y recibió 120. Green: test de geometría del texto más inspección de todos los píxeles de la foto pintada; ambos formatos centrados. `PreviewIcon` es responsable de centrar su propia imagen; el botón conserva texto, icono y acción.

## Inspección visual reproducible

La prueba opcional usa el fixture existente `kodak-dc50.kdc` revelado y reducido con `PreviewRenderer` para representar previews grandes en dos orientaciones. No duplica archivos de imagen en el repositorio ni cambia la política de previews de producción. Una celda representa un archivo con error.

```text
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B -pl app -am test -Dtest=LibraryGridTest -Dsurefire.failIfNoSpecifiedTests=false -DlibraryGridScreenshot=/tmp/photo-raw-lab-grid-after.png
```

Límites: tamaños expresados en coordenadas Swing; nombres largos se recortan con el comportamiento estándar de JButton. Por debajo del ancho de una celda, el viewport recorta la celda y conserva desplazamiento exclusivamente vertical. No se agregan ratings, barras laterales ni otras herramientas de Lightroom.

Verificación final tras review: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify` verde: 2 tests dominio, 54 app (1 omitido: captura opcional), 1 integración. La ejecución focal con `libraryGridScreenshot` pasó los 4 tests y produjo el PNG corregido; inspección manual confirmó cuatro columnas, nombre superior, previews centradas, celda de error, última fila sin estirar y ausencia de scrollbar para las once fotos en 1080 × 730. Conservados los tests existentes de carga asíncrona, errores, apertura y regreso.

## Review: cálculo estable de columnas y scroll

Expectativa: abrir directamente en 806 × 500 con seis fotos debe dar la misma geometría que redimensionar de 850 a 806: tres columnas, dos filas, sin scrollbar. En 570 × 500, dos columnas y tres filas; desplazar al máximo debe mostrar completa la última fila.

Red: `openingAndResizingToTheSameWidthProduceTheSameGridAndReachTheLastRow` falla: tercera celda en y=232 en vez de y=0 al abrir directamente. El tamaño preferido usaba el ancho anterior de la grilla, por lo que la scrollbar afectaba su propia necesidad. Green: cálculo desde el espacio disponible del JScrollPane, restando bordes y reservando scrollbar sólo si la altura de filas la necesita. Test focal verifica apertura y resize iguales, altura 464 sin scroll y 696 con scroll; extremo inferior del viewport y última celda coinciden en y=696.

## Review: captura después de eventos pendientes

Expectativa manual antes del cambio: once celdas en cuatro columnas necesitan 696 píxeles de alto, por lo que en 1080 × 730 no corresponde scrollbar. La captura anterior mostraba una scrollbar con altura preferida transitoria; inspección manual del PNG anterior refuta la evidencia original. Este hallazgo corrige la herramienta de captura, junto con la corrección funcional anterior; no se declara un nuevo rojo automatizado para el cambio de herramienta.

La creación y captura ahora ocurren en turnos EDT separados, dejando procesar los eventos de validación pendientes. Antes de pintar se comprueba altura de grilla=696, cuarta celda en y=0, última celda en y=464 y scrollbar ausente. La prueba opcional pasó con estas expectativas y regeneró `/tmp/photo-raw-lab-grid-after.png`; inspección manual confirmó la geometría estable y ausencia de scrollbar.
