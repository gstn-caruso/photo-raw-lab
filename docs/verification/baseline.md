# Evidencia MVP

Expectativa: el template Swing Java 25 renderizado debe compilar y producir JAR; la app inicial todavía no abre ni muestra RAW.

Línea base: `/home/gaston/Code/photo-raw-lab` y el plan pedido no existían. GitHub tampoco tenía el repo bajo gstn-caruso. Se creó el directorio, un repo desde java-template, una branch y un worktree. Se conservó el commit local vacío inicial en la branch bootstrap-local; no se publicaron archivos directamente a main.

Comando baseline: `mvn -B verify` en feat/java-raw-ffm, 2026-10-02. Resultado: BUILD SUCCESS; ningún test todavía. La ventana del template sólo presenta una etiqueta. Evidencia parcial: el build inicial no prueba soporte RAW.

Diseño delegado confirmó ABI del header LibRaw 0.21.5 frente a documentación web desactualizada. Referencias: https://raw.githubusercontent.com/LibRaw/LibRaw/0.21.5/libraw/libraw_types.h ; https://www.libraw.org/docs/API-C.html ; https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/foreign/SymbolLookup.html .

Decisiones: reconstruir plan desde el objetivo, Swing desktop Linux amd64, LibRaw vía FFM sin subprocess. Coste si no coincidía con el plan ausente: ajustar el diseño a ese documento cuando aparezca. Las implementaciones trabajan en dos worktrees sin archivos compartidos; integración y revisión independiente antes de publicar.

Criterio: intention-revealing-selector (#4); dominio sin Swing/FFM, responsabilidades de frontera separadas.
