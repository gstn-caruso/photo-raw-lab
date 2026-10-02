# Integración y distribución

Expectativa: `Main` recibe la ruta de un RAW real, abre una ventana visible y muestra una fotografía; el JAR y el launcher Debian deben reproducir la misma apertura.

Red: `MainIntegrationTest` contra Main del template falló con Timeout esperando el nombre de la fotografía, porque la ventana inicial ignoraba el argumento. Tras cablear decoder/frame se vio un crash SIGSEGV en un hilo nativo inmediatamente después del unload de LibRaw. El worker reprodujo y corrigió ese fallo con una regresión de decodes desde executors de corta vida; véase decoder-tdd.md.

La captura inicialmente era negra aunque el estado indicaba carga terminada. Se confirmó que el host conserva WAYLAND_DISPLAY=wayland-0 y XDG_SESSION_TYPE=wayland dentro de xvfb-run. El código fuente XRobotPeer/XdgDesktopPortal de Java 25 elige el portal Wayland con ese entorno, en vez de la pantalla X11 de prueba. El mismo test, sin esas dos variables, pasó y produjo raw-window.png con la fotografía de edificios Kodak. Se eliminan sólo del proceso de prueba.

Red de distribución: `PackagedLauncherIT` ejecutado contra el JAR anterior del template mostró sólo 3 colores en la región central y falló tras esperar una fotografía. Después de reconstruir apareció otro fallo reproducible: Maven Shade cambiaba basedir a app/target por dependencyReducedPomLocation, de modo que Failsafe buscaba target/target/*.jar. Se deshabilitó el POM reducido para este ejecutable, sin publicación Maven. La [documentación oficial de Shade](https://maven.apache.org/plugins/maven-shade-plugin/shade-mojo.html) describe ese cambio de basedir.

Comando reproducible de suite: `env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify`. Surefire verifica dominio/FFM/UI/Main; Failsafe abre el JAR reconstruido como proceso independiente y comprueba píxeles fotográficos de la pantalla virtual. Los PNG quedan en app/target/verification.

La release extrae el Debian, comprueba libjvm.so, dependencia libraw23t64 y opción FFM, y vuelve a ejecutar PackagedLauncherIT con -Dphoto.raw.launcher apuntando al binario Debian. Así verifica el runtime incluido sin necesitar un comando java dentro de la imagen jlink (ese binario se elimina normalmente por jpackage).

El comando aislado de Failsafe requiere `test-compile` en el reactor antes de invocar los goals directamente; sin lifecycle Maven no resuelve el módulo domain en una segunda ejecución limpia. La verificación del launcher usa `mvn -B -pl app -am -Dphoto.raw.launcher=/ruta/al/binario test-compile failsafe:integration-test failsafe:verify` bajo Xvfb.

Límites: un fixture Kodak real no prueba todos los modelos de cámara; no se usó profiler de leaks ni inyección de fallos de asignación nativa. No se implementan edición/exportación ni decodificación de volcados sin cabecera.

Green observado 2026-10-02: suite completa BUILD SUCCESS, 14 tests Surefire + 1 Failsafe, 0 failures/errors/skips. Debian local 0.1.1 amd64 contiene libjvm.so y declara libraw23t64. Prueba del launcher extraído BUILD SUCCESS, 1/1, sin stderr; packaged-window.png muestra la fotografía con botón Abrir archivo… y nombre kodak-dc50.kdc. La release remota todavía debe observarse después del PR.

Criterio: intention-revealing-selector (#4); fronteras nativas y Swing separadas del dominio.
