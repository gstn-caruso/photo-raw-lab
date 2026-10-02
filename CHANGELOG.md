# Changelog

Los cambios de cada PR se registran acá antes del merge a `main`.

## Unreleased

- Comando de build local reproducible con Xvfb y entorno X11 para las pruebas visuales.

- Proyecto inicial.
- Visor Swing que abre fotografías RAW con LibRaw mediante FFM, carga en segundo plano y muestra errores recuperables.
- CI con un RAW real y pruebas de ventana bajo Xvfb; JAR y Debian amd64 con runtime Java y dependencia LibRaw declarada.
