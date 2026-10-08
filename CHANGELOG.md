# Changelog

Los cambios de cada PR se registran acá antes del merge a `main`.

## Unreleased

- Mosaico de previews RAW del directorio elegido, con carga secuencial en segundo plano, apertura de fotografías completas y vuelta al mosaico.
- Último directorio recordado, selector inicial cuando falta y soporte de carpetas por línea de comandos.
- Resultados obsoletos ignorados al cambiar de carpeta o cerrar; liberación de imágenes al cerrar y errores individuales recuperables.
- Releases semánticas según Conventional Commits, con versión consistente entre JAR, Debian, tag y changelog adjunto.

- Comando de build local reproducible con Xvfb y entorno X11 para las pruebas visuales.

- Proyecto inicial.
- Visor Swing que abre fotografías RAW con LibRaw mediante FFM, carga en segundo plano y muestra errores recuperables.
- CI con un RAW real y pruebas de ventana bajo Xvfb; JAR y Debian amd64 con runtime Java y dependencia LibRaw declarada.
