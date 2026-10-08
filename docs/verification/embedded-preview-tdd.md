# Verificación de previews embebidas

Expectativa: el mosaico puede mostrar la preview de cámara sin revelar el sensor; abrir la fotografía conserva el revelado completo. La ausencia de una preview utilizable conserva el fallback completo y los errores de apertura siguen siendo recuperables.

## Evidencia automatizada

- Línea base: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`, verde en 5,5 s.
- Renderer: Red porque se pidió `decode` completo; Green al pedir `decodePreview`. El default preserva decoders expresados como lambdas. Se mantienen proporción, límite 240 × 180 y previews pequeñas sin agrandar.
- LibRaw: Red devolvía 768 píxeles de ancho; Green extrae la miniatura RGB 96 × 64. Una copia truncada en `0x4d00` falla al revelar el sensor y conserva exactamente la preview original, demostrando que la ruta rápida no depende del revelado completo.
- Copias con miniatura ausente o con offset inválido devuelven los mismos píxeles que el revelado completo. Repetir previews y alternar fallas con éxitos conserva resultados. Los errores de apertura no activan el fallback; la sesión nativa se cierra antes del fallback y libera el buffer incluso cuando la conversión falla.
- JPEG: Red rechazaba el header nativo con dimensiones cero; Green obtiene dimensiones y RGB del payload mediante ImageIO. Se rechazan tamaños inválidos y payload ilegible, conservando una copia Java independiente del buffer nativo.
- Orientación: Red bitmap 96 × 64, Green 64 × 96 con TIFF orientation 6. Se verifican las ocho transformaciones de LibRaw. Red JPEG con EXIF orientation 6 conservaba 3 × 2; Green produce 2 × 3. EXIF del JPEG prevalece sobre orientación RAW; se cubren ambos órdenes de bytes, los ocho valores y metadata inválida o truncada.
- Verificación final de comportamiento: `verify` verde en 6,2 s. `ruby .github/release_version_test.rb`: 5 tests, 7 assertions, sin fallas.

## Regresión del diagnóstico de apertura tras revisión

El commit `685b8e7`, aunque se tituló `refactor`, incluyó una corrección de comportamiento: ante una falla de `open_file`, dejó de reintentar el revelado completo y pasó de `Cannot decode <ruta>` a `Cannot decode preview <ruta>`, preservando la causa nativa de la primera apertura. El fallback quedó limitado a miniaturas no utilizables. No se reescribió el historial para cambiar el tipo del commit.

La revisión detectó que las pruebas anteriores sólo exigían `IOException`. Se agregaron dos regresiones que verifican el contexto de preview, la ruta del archivo y una causa `IOException` con diagnóstico `LibRaw open_file failed`, para un archivo corrupto y uno inexistente. Línea base de esta revisión: `verify` verde en 5,184 s. Para observar Red se reemplazó temporalmente sólo el lanzamiento de la excepción de apertura por `return decode(path)`, reproduciendo el comportamiento anterior sin cambiar de rama ni descartar trabajo: ambas regresiones fallaron porque recibieron `Cannot decode <ruta>`. Después se restauró la línea original con `apply_patch`; ambas regresiones pasaron. Comando focalizado:

```text
mvn -B -pl app -am test '-Dtest=LibRawDecoderTest#reportsCorruptPreviewFileWithOpeningContextAndNativeCause+reportsMissingPreviewFileWithOpeningContextAndNativeCause' -Dsurefire.failIfNoSpecifiedTests=false
```

Estas regresiones protegen el diagnóstico observable y su causa; la restricción del reintento se verificó además inspeccionando que `open_file` ocurre fuera del bloque de fallback. La verificación completa posterior quedó verde: 51 tests Java, incluido el test de integración del launcher, en 6,026 s. La prueba nativa sigue usando bitmap Kodak; JPEG sigue cubierto con imágenes sintéticas.

## Comparación manual reproducible

El 8 de octubre de 2026, con Java 25.0.4 y la biblioteca local `libraw.so.23`, cinco pares de calentamiento y veinte pares medidos sobre el fixture Kodak dieron medias de **53,578 ms** para `decode` y **0,995 ms** para `decodePreview`. Se midió la decodificación, sin escalado Swing ni lectura de una carpeta. Esta comparación de un único archivo con caché caliente no predice el tiempo de otras cámaras, formatos o discos.

Después de `mvn verify`, iniciar:

```text
jshell --enable-native-access=ALL-UNNAMED --class-path app/target/classes:domain/target/classes
```

Ingresar en el REPL de Java:

```java
import photorawlab.app.LibRawDecoder;
import java.nio.file.Path;
var decoder = new LibRawDecoder();
var fixture = Path.of("app/src/test/resources/raw/kodak-dc50.kdc");
for (int i = 0; i < 5; i++) { decoder.decode(fixture); decoder.decodePreview(fixture); }
long fullNanos = 0, previewNanos = 0;
for (int i = 0; i < 20; i++) {
    long start = System.nanoTime();
    decoder.decode(fixture);
    fullNanos += System.nanoTime() - start;
    start = System.nanoTime();
    decoder.decodePreview(fixture);
    previewNanos += System.nanoTime() - start;
}
System.out.printf("Full average %.3f ms; preview average %.3f ms%n", fullNanos / 20_000_000.0, previewNanos / 20_000_000.0);
/exit
```

## Responsabilidades y límites

`RawImageDecoder` ofrece el pedido de preview; `LibRawDecoder` decide extracción y fallback; `ProcessedBitmap` copia la memoria nativa a RGB; `JpegOrientation` interpreta orientación EXIF IFD0 con bounds; `PreviewOrientation` transforma píxeles; `PreviewRenderer` escala para el mosaico. Se aplica Intention Revealing Selector (#4).

La [API C de LibRaw](https://www.libraw.org/docs/API-C.html) ofrece `unpack_thumb` y `dcraw_make_mem_thumb`. Su [implementación 0.21](https://github.com/LibRaw/LibRaw/blob/0.21-stable/src/postprocessing/mem_image.cpp) entrega JPEG con dimensiones en el payload, bitmaps RGB sin rotar y memoria que debe liberarse. El acceso a `sizes.flip` usa el [ABI 0.21 amd64](https://github.com/LibRaw/LibRaw/blob/0.21-stable/libraw/libraw_types.h), coherente con el paquete Linux amd64 del proyecto.

El fixture real cubre bitmap; JPEG y EXIF usan payload generado en memoria, sin probar un RAW real con JPEG embebido. No se verificó un conjunto amplio de cámaras ni memoria residente de larga duración. El parser de orientación busca EXIF IFD0 estándar; ante metadata ausente o no reconocida usa la orientación de cámara.
