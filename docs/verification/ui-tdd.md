# Swing MVP: evidencia TDD

Responsabilidades: `RawViewerFrame` coordina apertura y estado en EDT; `RawImagePanel` traduce RGB a bitmap y decide ajuste proporcional; `RawImageDecoder` posee la decodificación. No hay conocimiento Swing en el dominio. Criterio aplicado: Intention Revealing Selector (#4).

## Bitmap y ajuste proporcional

Expectativa: preservar colores RGB y centrar una fotografía 2:1 en superficies cuadradas y 2:1 al redimensionar.

Rojo: `xvfb-run -a mvn -B -pl app -am test -Dtest=RawImagePanelTest -Dsurefire.failIfNoSpecifiedTests=false` falló compilando porque `RawImagePanel` todavía no existía. Verde: mismo comando, 1 test, 0 errores/fallas. La prueba pinta un panel real sobre bitmap y comprueba píxeles y márgenes.

Límite: interpolación visual de fotografías reales pendiente de integración con decoder nativo.

## Apertura y ciclo de vida

Expectativa: una carga en segundo plano conserva respuesta del EDT, deshabilita Abrir y muestra fotografía; un error visible permite reintentar; cerrar durante carga no interrumpe el decoder ni publica resultados.

Rojo inicial: `xvfb-run -a mvn -B -pl app -am test -Dtest=RawViewerFrameTest -Dsurefire.failIfNoSpecifiedTests=false` falló por `RawViewerFrame` inexistente. Con coordinación y recuperación implementadas, el escenario de cierre quedó rojo: esperaba `Cargando pending.raw…` pero observó `pending.raw` después de disponer la ventana. La prueba observa el evento de finalización `loading` y propiedades reales de Swing; no depende de sleeps.

La corrección mantiene el decoder como dueño de sus recursos y descarta el resultado cuando la ventana fue dispuesta. El evento `loading=false` comunica finalización incluso en ese caso sin actualizar los widgets.

Verde final: `xvfb-run -a mvn -B -pl app -am test`, 2 tests dominio y 4 tests Swing, cero errores/fallas. Incluye pintura real, respuesta EDT con decoder retenido por latch, error seguido de éxito y cierre con decoder activo. Todos los frames se disponen en teardown.

Límites: `Main`, apertura con LibRaw real, selector manual y launcher empaquetado corresponden a integración del coordinador. El filtro orienta la selección y conserva la opción Todos los archivos; el decoder decide por contenido.
