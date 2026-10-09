# Changelog

Los cambios de cada PR se registran acá antes del merge a `main`.

## Unreleased

- Comando de proyecto `/abrir-app` para compilar y abrir el visor con Maven.

- Previews rápidas desde el JPEG o bitmap RGB embebido por la cámara, con orientación y fallback al revelado completo cuando la miniatura no es utilizable. Abrir una fotografía conserva el revelado RAW completo.

- Escala de interfaz calculada al arrancar según los DPI del escritorio Linux/X11, con overrides explícitos respetados y consulta `xrdb` acotada.
- Dependencia Debian `x11-xserver-utils` para obtener la configuración DPI mediante `xrdb`.

- Mosaico de previews RAW del directorio elegido, con carga secuencial en segundo plano, apertura de fotografías completas y vuelta al mosaico.
- Último directorio recordado, selector inicial cuando falta y soporte de carpetas por línea de comandos.
- Resultados obsoletos ignorados al cambiar de carpeta o cerrar; liberación de imágenes al cerrar y errores individuales recuperables.
- Releases semánticas según Conventional Commits, con versión consistente entre JAR, Debian, tag y changelog adjunto.

- Comando de build local reproducible con Xvfb y entorno X11 para las pruebas visuales.

- Proyecto inicial.
- Visor Swing que abre fotografías RAW con LibRaw mediante FFM, carga en segundo plano y muestra errores recuperables.
- CI con un RAW real y pruebas de ventana bajo Xvfb; JAR y Debian amd64 con runtime Java y dependencia LibRaw declarada.
