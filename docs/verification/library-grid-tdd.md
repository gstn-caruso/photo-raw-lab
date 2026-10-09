# Grilla de biblioteca

Referencia visual: [Biblioteca Lightroom](https://www.youtube.com/watch?v=F5yy-XpLXOs), captura local `/tmp/photo-raw-lab-grid-reference.png`. Alcance: grilla, nombres y previews; sin barras laterales ni herramientas de clasificación.

Responsabilidades: `DirectoryMosaicPanel` conecta rutas, previews y acciones; su grilla Swing decide columnas, geometría y scroll según el viewport. El dominio no cambia.

Línea base: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify` verde (2 tests dominio, 50 app, 1 integración).

## Escenario 1: geometría adaptable

Expectativa: celdas de 264 × 232, alineadas arriba/izquierda; reducir el ancho cambia de tres a dos columnas conservando altura; última fila y foto única no se estiran; scroll vertical cuando hay suficientes filas.

Red: `LibraryGridTest.resizeReflowsColumnsWithoutStretchingRowsOrTheLastPhoto` falla: esperaba 264 × 232 y recibió 282 × 215. Green: el mismo test pasa con grilla `Scrollable`; verifica dos anchos, siete fotos y una foto.

Comando focal: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B -pl app -am test -Dtest=LibraryGridTest -Dsurefire.failIfNoSpecifiedTests=false`.

Criterio: RDD, responsabilidades de UI en el interfacer; Intention Revealing Selector (#4). No fue necesario un tidying previo.
