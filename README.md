# Photo RAW Lab

Visor de fotografías RAW con Java 25, Swing y LibRaw mediante FFM. Al iniciar, muestra un mosaico del último directorio elegido. La primera vez, o si esa carpeta ya no existe, pide elegir una. **Abrir carpeta…** permite cambiarla; cancelar conserva la ventana disponible.

El mosaico incluye archivos RAW del directorio, sin recorrer subcarpetas, ordenados por nombre. Las previews aparecen en segundo plano; hacé clic para abrir una foto completa y usá **Volver al mosaico** para regresar. Los archivos que fallan quedan deshabilitados con el error en su tooltip; el resto continúa cargándose. **Abrir archivo…** también permite elegir una fotografía directamente.

Las miniaturas conservan la proporción dentro de 240 × 180 píxeles. Cada preview requiere revelar el RAW completo con LibRaw, de a uno, por lo que carpetas grandes o archivos pesados pueden tardar. Al volver a un mosaico que todavía estaba cargando, se vuelve a cargar esa carpeta. Cambiar de carpeta o cerrar ignora resultados anteriores y deja terminar la llamada nativa en curso.

## Ejecutar

El MVP se distribuye para Linux amd64. Instalá el `.deb` de [Releases](https://github.com/gstn-caruso/photo-raw-lab/releases) con `sudo apt install ./photo-raw-lab_*.deb` y ejecutá `/opt/photo-raw-lab/bin/photo-raw-lab`. El paquete incluye su propio Java y declara la dependencia nativa LibRaw.

Para desarrollar en Ubuntu 24.04 o posterior:

```text
sudo apt install libraw-dev xvfb
asdf install
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar
```

También podés pasar una ruta inicial:

```text
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar "/ruta/fotografía.raw"
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar "/ruta/fotografías"
```

El JAR necesita Java 25 y `libraw.so.23`. Los launchers habilitan el [acceso nativo de FFM](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/foreign/SymbolLookup.html). El decoder usa la [API C de LibRaw](https://www.libraw.org/docs/API-C.html) para revelar los datos RAW completos a RGB sRGB de 8 bits. Identifica el formato por el contenido: acepta `.raw`, CR2/CR3, NEF, ARW, DNG, KDC y otros formatos soportados por la biblioteca instalada. Un volcado de sensor sin cabecera no identifica por sí solo cámara y dimensiones.

## Testear

`env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`

Al usar Xvfb desde una sesión Wayland se eliminan esas dos variables sólo para el proceso de prueba, para que Java capture la pantalla virtual X11.

La suite incluye una fotografía RAW de cámara real, archivos corruptos y rutas Unicode, y pruebas Swing bajo Xvfb. El fixture tiene [licencia y atribución propias](app/src/test/resources/raw/README.md), se usa sólo en tests y no se incluye en el JAR.

La release se calcula desde el último tag: `feat:` aumenta minor, `fix:` y `perf:` aumentan patch, y `!` o `BREAKING CHANGE:` aumentan major. Cambios como `docs:` o `chore:` por sí solos no publican una release. El JAR, `.deb`, tag y changelog adjunto usan la misma versión. El PR debe actualizar `CHANGELOG.md`.

La clasificación de versiones se verifica con `ruby .github/release_version_test.rb` (Ruby con minitest); `ruby .github/release_version.rb` muestra la decisión para el historial actual.
