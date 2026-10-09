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

Verificación final: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify` verde: 2 tests dominio, 53 app (1 omitido: captura opcional), 1 integración. La ejecución focal con `libraryGridScreenshot` pasó los 3 tests y produjo el PNG; inspección manual confirmó cuatro columnas, nombre superior, previews centradas, celda de error y última fila sin estirar. Conservados los tests existentes de carga asíncrona, errores, apertura y regreso.
