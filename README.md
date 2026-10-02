# Photo RAW Lab

Visor de fotografías RAW con Java 25, Swing y LibRaw mediante FFM. Abre una fotografía desde **Abrir archivo…** y la muestra ajustada a la ventana. La carga ocurre en segundo plano y los errores permiten volver a abrir otro archivo.

## Ejecutar

El MVP se distribuye para Linux amd64. Instalá el `.deb` de [Releases](https://github.com/gstn-caruso/photo-raw-lab/releases) con `sudo apt install ./photo-raw-lab_*.deb` y ejecutá `/opt/photo-raw-lab/bin/photo-raw-lab`. El paquete incluye su propio Java y declara la dependencia nativa LibRaw.

Para desarrollar en Ubuntu 24.04 o posterior:

```text
sudo apt install libraw-dev xvfb
asdf install
mvn -B package
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar
```

También podés pasar una ruta inicial:

```text
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar "/ruta/fotografía.raw"
```

El JAR necesita Java 25 y `libraw.so.23`. Los launchers habilitan el [acceso nativo de FFM](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/foreign/SymbolLookup.html). El decoder usa la [API C de LibRaw](https://www.libraw.org/docs/API-C.html) para revelar los datos RAW completos a RGB sRGB de 8 bits. Identifica el formato por el contenido: acepta `.raw`, CR2/CR3, NEF, ARW, DNG, KDC y otros formatos soportados por la biblioteca instalada. Un volcado de sensor sin cabecera no identifica por sí solo cámara y dimensiones.

## Testear

`env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`

Al usar Xvfb desde una sesión Wayland se eliminan esas dos variables sólo para el proceso de prueba, para que Java capture la pantalla virtual X11.

La suite incluye una fotografía RAW de cámara real, archivos corruptos y rutas Unicode, y pruebas Swing bajo Xvfb. El fixture tiene [licencia y atribución propias](app/src/test/resources/raw/README.md), se usa sólo en tests y no se incluye en el JAR.

Cada merge a `main` publica un JAR y un `.deb` con runtime Java incluido, incluso para `docs:` y `chore:`. El PR debe actualizar `CHANGELOG.md`.
