# Java RAW FFM Implementation Plan

> Antecedente del MVP. La evolución integral se sigue en la
> [hoja de ruta hacia Lightroom Classic](../../roadmap-lightroom-classic.md).
> Las casillas originales no representan el estado actual. Para trabajo nuevo
> rigen los acuerdos de AGENTS.md: mismo checkout, feature branch y sin worktrees.

**Goal:** Ventana que abre fotografías RAW y las muestra, con CI verde.
**Architecture:** Dominio RGB independiente; LibRaw FFM en frontera; Swing coordina carga async y presentación.
**Tech Stack:** Java 25, Maven, JUnit, Swing, LibRaw 0.21, Linux amd64.
**Spec:** ../specs/2026-10-02-java-raw-ffm-design.md

## Global Constraints

TDD Red–Green–Refactor, commits convencionales manuales, feature branch en el mismo checkout sin worktrees, revisión independiente, PR y merge sólo con CI verde.

## Task 1: Baseline y contratos

- [ ] Generar Swing desde java-template, ejecutar `mvn -B verify`: BUILD SUCCESS.
- [ ] Dominio `photorawlab.domain.RgbImage(int width,int height,int[] pixels)`: getters width(),height(),pixels(); valida dimensiones/tamaño y copia entrada/salida. `RawImageDecoder.decode(Path)` devuelve RgbImage y lanza IOException.
- [ ] Escribir tests de invariantes/copia, observar rojo, implementar, observar verde y commit.

## Task 2: Decoder FFM

- [ ] Crear `app.LibRawDecoder implements RawImageDecoder`: constructor por defecto y constructor Path para ruta explícita de biblioteca. init→open_file→unpack→dcraw_process→make_mem_image; sRGB 8 bits, bitmap=2, 3 canales, tamaño validado. clear_mem y close en finally.
- [ ] Test RAW Kodak fixture real, copia a `.raw`, píxeles/dimensiones, decodes repetidos, corrupto/inexistente y recuperación, biblioteca ausente, archivo/ruta Unicode. Observar rojo antes de cada escenario y verde tras implementarlo.
- [ ] `xvfb-run -a mvn -B verify`: ningún test omitido ni fallido. Commit por escenario.

## Task 3: Ventana

- [ ] Crear `app.RawViewerFrame(RawImageDecoder)` y `openRaw(Path)`, `RawImagePanel` con ajuste proporcional; Main abre ventana y acepta ruta inicial.
- [ ] Test carga async sin bloquear EDT, imagen renderizada real, error recuperable, abrir deshabilitado durante carga, cierre durante carga sin usar recursos liberados. TDD y commit por escenario.
- [ ] `xvfb-run -a mvn -B verify`: verde. Verificar visualmente ventana cargando fixture.

## Task 4: Entrega

- [ ] CI instala libraw-dev/xvfb y ejecuta suite real; JAR habilita acceso nativo por manifest y jpackage por opción JVM; Debian declara dependencia nativa. Actualizar README/CHANGELOG y guardar evidencia TDD.
- [ ] Revisar branch con agente independiente, corregir hallazgos y volver a verificar.
- [ ] Publicar PR, CI verde, squash merge, verificar release JAR/deb/runtime/changelog y apertura mediante launcher empaquetado.

## Review Focus

Paths Unicode/espacios; fallos nativos con recursos vivos; bitmap inesperado/tamaño desbordado; cerrar mientras decodifica; launcher sin acceso nativo o dependencia LibRaw.
