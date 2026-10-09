# Orden del mosaico

Expectativa: ordenar por nombre, fecha de modificación o bytes, en ambas direcciones; cambiar el orden durante la carga conserva las celdas, sus imágenes, errores y acciones. La selección dura la sesión, incluyendo otra carpeta y abrir/volver de una foto.

Responsabilidades: `GalleryPhoto` conoce los valores inmutables; `GallerySort` decide comparadores puros sin Swing ni filesystem. `RawDirectory.snapshot` traduce `BasicFileAttributes` a esos valores, una lectura por candidato RAW, desde el `SwingWorker` existente. `RawViewerFrame` conserva sus guardas por generación. `DirectoryMosaicPanel` mantiene controles y relación ruta/botón y mueve los mismos botones. `showFiles` conserva soporte de rutas virtuales sin I/O.

Línea base: rama `feat/gallery-sort-controls` limpia; no existían controles ni claves de metadata. No hizo falta un tidying previo. Se reutilizan las skills `code-criteria`, `test-driven-development` y `feature-delivery`.

1. `GallerySortTest.sortsEachCriterionInBothDirections`: rojo por API inexistente; verde con los seis órdenes esperados (`mvn -B -pl domain test`).
2. `tiesUseAscendingNamesInEitherDirectionRegardlessOfInputOrder`: rojo por orden inverso de empates; verde con dos permutaciones, fecha/tamaño y ambas direcciones. Nombre usa `Locale.ROOT`, luego nombre exacto para distinguir mayúsculas.
3. `GalleryControlsTest.directionReordersExistingTilesPreservingPreviewErrorAndAction`: rojo porque la lista inicial no se ordenaba; verde con controles al pie, misma identidad de botones/iconos, error, habilitación y acción. Verifica paleta y `labelFor`. Se aumenta 40 px la altura de las ventanas de `LibraryGridTest` para mantener su espacio de viewport tras agregar el footer.
4. `RawDirectoryTest.capturesFileMetadataForSortingWithoutReadingImageContent`: rojo por snapshot inexistente; verde con fecha y bytes reales. `sortingDuringDecodeUsesMetadataAndKeepsPendingPreviewAttachedToItsPath`: rojo porque size no movía el archivo menor mientras el decoder estaba bloqueado por latch; verde con snapshot publicado, mismos botones, previews rojas/verdes en la ruta correcta y exactamente dos decodes. El timeout del latch acota el test, no decide el orden de eventos.
5. `sortingPreferencesSurvivePhotoNavigationAndChangingDirectories`: el contrato ya estaba verde por estado de controles del mismo panel; prueba de regresión sin cambio de producción. Verifica criterio/dirección retenidos y ausencia de redecode al volver al mosaico completo.

Al reforzar el último paso para verificar el orden de la carpeta nueva se detectó un evento `loading` pendiente de la vuelta al mosaico. Se vacía esa notificación antes de abrir la siguiente carpeta; la prueba espera ahora la finalización correcta. Los tests usan `LastDirectory` en memoria para evitar modificar preferencias del usuario.

Inspección de captura: los valores seleccionados de los combos se pintaban claros pese a sus propiedades de paleta. Red: comprobación de pixel en el valor actual esperaba `PANEL` y recibió el color del look and feel. Green: `AppPalette` pinta el valor y su fondo explícitamente y mantiene renderer gris para el desplegable y flecha gris. Siete tests focales (controles + grilla con captura) verdes; el PNG regenerado confirma footer gris, textos legibles, cuatro columnas, previews conservadas y ausencia de scrollbar con once fotos.

Incidente de proceso: después del verde de la suite del escenario 4 se añadió una comprobación de color más fuerte. Se commiteó antes de revisar esa ejecución, que falló por muestrear (120,90) en una imagen de un píxel centrada en (119,89). Se preservó el trabajo, se corrigió sólo la coordenada y se ejecutaron los cinco tests focales en verde en el commit siguiente. La revisión debe considerar este desvío del acuerdo de commit en verde.

Comando focal: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B -pl app -am -Dtest=GalleryControlsTest,RawDirectoryTest -Dsurefire.failIfNoSpecifiedTests=false test`.

Captura opcional: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B -pl app -am test -Dtest=LibraryGridTest -Dsurefire.failIfNoSpecifiedTests=false -DlibraryGridScreenshot=/tmp/photo-raw-lab-sort-after.png`. Reutiliza el fixture y deja estabilizar el EDT antes de pintar.

Límites: la metadata es un snapshot al abrir la carpeta; cambios externos posteriores requieren volver a abrirla. No persiste preferencias entre ejecuciones ni usa fecha EXIF. No se cambió la política existente de recargar un mosaico incompleto al volver desde una foto.

Verificación final: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`: 4 tests dominio, 58 app (1 omitido: captura opcional) y 1 integración del launcher. Captura opcional en `/tmp/photo-raw-lab-sort-after.png`, 4 tests de grilla verdes.

Criterio — intention-revealing-selector (#4) · `references/004-intention-revealing-selector.md`. RDD mantiene el dominio independiente de UI y filesystem.
