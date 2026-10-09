# Photo RAW Lab

Visor de fotografías RAW con Java 25, Swing y LibRaw mediante FFM. Al iniciar, muestra un mosaico del último directorio elegido. La primera vez, o si esa carpeta ya no existe, pide elegir una. **Abrir carpeta…** permite cambiarla; cancelar conserva la ventana disponible.

El mosaico incluye archivos RAW del directorio, sin recorrer subcarpetas, ordenados por nombre. La grilla de biblioteca adapta la cantidad de columnas al ancho de la ventana, con celdas de 264 × 232 píxeles alineadas arriba e izquierda y desplazamiento vertical. Cada celda muestra el nombre arriba y la fotografía centrada en un área de 240 × 180; una foto única y la última fila conservan el mismo tamaño. Las previews aparecen en segundo plano; hacé clic para abrir una foto completa y usá **Volver al mosaico** para regresar. Los archivos que fallan quedan deshabilitados con el error en su tooltip; el resto continúa cargándose. **Abrir archivo…** también permite elegir una fotografía directamente.

Las miniaturas conservan la proporción dentro de 240 × 180 píxeles, sin agrandar previews pequeñas. El mosaico recupera primero la preview JPEG o RGB guardada por la cámara, sin revelar los datos del sensor: sus colores, recorte y resolución pueden diferir de la fotografía completa. Respeta la orientación EXIF del JPEG cuando está disponible y la orientación de cámara en los demás casos. Si la preview falta, su formato no está soportado o no se puede leer, revela el RAW completo como alternativa; esos archivos pueden tardar más. Abrir una fotografía sigue revelando el RAW completo.

Las previews se cargan de a una en segundo plano. Al volver a un mosaico que todavía estaba cargando, se vuelve a cargar esa carpeta. Cambiar de carpeta o cerrar ignora resultados anteriores y deja terminar la llamada nativa en curso. La [verificación de previews](docs/verification/embedded-preview-tdd.md) registra los casos cubiertos y una comparación local de tiempos.

## Ejecutar

El MVP se distribuye para Linux amd64. Instalá el `.deb` de [Releases](https://github.com/gstn-caruso/photo-raw-lab/releases) con `sudo apt install ./photo-raw-lab_*.deb` y ejecutá `/opt/photo-raw-lab/bin/photo-raw-lab`. El paquete incluye su propio Java y declara la dependencia nativa LibRaw.

Para desarrollar en Ubuntu 24.04 o posterior:

```text
sudo apt install libraw-dev xvfb
asdf install
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar
```

Para compilar y abrir la app desde la raíz sin generar el JAR:

```text
mvn -B -pl app -am compile exec:exec
```

En Claude Code, `/abrir-app` ejecuta ese comando mediante
[`.claude/commands/abrir-app.md`](.claude/commands/abrir-app.md) y deja la ventana
abierta en el escritorio actual.

También podés pasar una ruta inicial:

```text
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar "/ruta/fotografía.raw"
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar "/ruta/fotografías"
```

El JAR necesita Java 25 y `libraw.so.23`. Los launchers habilitan el [acceso nativo de FFM](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/foreign/SymbolLookup.html). El decoder usa la [API C de LibRaw](https://www.libraw.org/docs/API-C.html) para revelar los datos RAW completos a RGB sRGB de 8 bits. Identifica el formato por el contenido: acepta `.raw`, CR2/CR3, NEF, ARW, DNG, KDC y otros formatos soportados por la biblioteca instalada. Un volcado de sensor sin cabecera no identifica por sí solo cámara y dimensiones.

En Linux con `DISPLAY`, la aplicación consulta `Xft.dpi` mediante `xrdb` antes de iniciar Swing. Convierte DPI/96 al entero más cercano: 192 DPI usa escala 2 y 288 DPI usa 3. Sólo aplica escalas de 2 o más, con DPI válidos entre 96 y 768; ante un dato ausente, inválido o una consulta fallida, Java conserva su escala automática. El paquete Debian incluye la dependencia `x11-xserver-utils`, que provee `xrdb`.

Se respetan los ajustes explícitos `-Dsun.java2d.uiScale=…`, `-Dsun.java2d.uiScale.enabled=false`, `J2D_UISCALE` y `GDK_SCALE`. La consulta dura como máximo 750 ms y la escala se decide al arrancar: si cambiás los DPI del escritorio o de monitor, reiniciá la aplicación. Java limita el escalado de Linux/X11 a factores enteros; los ajustes Java están documentados en [Java 2D Properties](https://docs.oracle.com/en/java/javase/25/troubleshoot/java-2d-properties.html).

## Testear

`env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`

Al usar Xvfb desde una sesión Wayland se eliminan esas dos variables sólo para el proceso de prueba, para que Java capture la pantalla virtual X11.

La suite incluye una fotografía RAW de cámara real, archivos corruptos y rutas Unicode, y pruebas Swing bajo Xvfb. El fixture tiene [licencia y atribución propias](app/src/test/resources/raw/README.md), se usa sólo en tests y no se incluye en el JAR.

La release se calcula desde el último tag: `feat:` aumenta minor, `fix:` y `perf:` aumentan patch, y `!` o `BREAKING CHANGE:` aumentan major. Cambios como `docs:` o `chore:` por sí solos no publican una release. El JAR, `.deb`, tag y changelog adjunto usan la misma versión. El PR debe actualizar `CHANGELOG.md`.

La clasificación de versiones se verifica con `ruby .github/release_version_test.rb` (Ruby con minitest); `ruby .github/release_version.rb` muestra la decisión para el historial actual.
