# Swing MVP: evidencia TDD

Responsabilidades: `RawViewerFrame` coordina apertura y estado en EDT; `RawImagePanel` traduce RGB a bitmap y decide ajuste proporcional; `RawImageDecoder` posee la decodificación. No hay conocimiento Swing en el dominio. Criterio aplicado: Intention Revealing Selector (#4).

## Bitmap y ajuste proporcional

Expectativa: preservar colores RGB y centrar una fotografía 2:1 en superficies cuadradas y 2:1 al redimensionar.

Rojo: `xvfb-run -a mvn -B -pl app -am test -Dtest=RawImagePanelTest -Dsurefire.failIfNoSpecifiedTests=false` falló compilando porque `RawImagePanel` todavía no existía. Verde: mismo comando, 1 test, 0 errores/fallas. La prueba pinta un panel real sobre bitmap y comprueba píxeles y márgenes.

Límite: interpolación visual de fotografías reales pendiente de integración con decoder nativo.
