# java-template

Template Maven multi-módulo con Java 25 LTS, asdf, JUnit y un módulo `domain` sin framework. Se elige el tipo de aplicación al crear el proyecto: **Swing** (default), **libGDX** o **Spring Boot con SSR y Thymeleaf**. El template sin renderizar contiene placeholders y no compila directamente.

## Crear un proyecto

Primero preguntá qué tipo de aplicación quiere la persona. Usá `swing` si no elige otro.

1. Creá un repositorio desde `gstn-caruso/java-template` y clonalo.
2. En la raíz del checkout, ejecutá:

   ```text
   java .template/Renderer.java --slug mi-app --name "Mi App" --description "Descripción" --owner gstn-caruso --flavor swing
   ```

   Reemplazá `swing` por `libgdx` o `spring-ssr` según la elección. `--flavor` puede omitirse para usar Swing. Opcionales: `--package`, `--class`, `--group-id`, `--author`, `--email` y `--year`.
3. Ejecutá `mvn -B verify`, registrá el baseline en `CHANGELOG.md` y abrí un PR desde una feature branch. La CI debe pasar antes del merge.

El renderer copia el sabor elegido, reemplaza los placeholders y borra `.template` junto con el workflow propio del template. El proyecto generado conserva `.tool-versions` con Temurin 25.

## Release

Cada merge de PR a `main` dispara el workflow del proyecto generado, incluso si solo cambian docs o configuración. Tras `mvn verify`, construye el JAR con versión `0.1.<run_number>`, crea un `.deb` Linux `amd64` mediante `jpackage` y publica ambos junto con `CHANGELOG.md` en GitHub Releases. El `.deb` incluye una JVM propia; no depende de Java instalado en el equipo de destino. Cada PR actualiza `CHANGELOG.md` con una línea que describa el cambio para que el historial quede versionado.

El JAR sigue necesitando Java 25 instalado para ejecutarse por separado. El `.deb` incluye Java 25 y el programa. Para publicar paquetes de otras arquitecturas hace falta una CI de esa arquitectura.

## Desarrollo del template

`javac -d target/template-test .template/Renderer.java .template/RendererTest.java` y `java -cp target/template-test RendererTest` verifican el renderer. La CI genera los tres tipos de aplicación, ejecuta `mvn verify`, construye el JAR versionado y comprueba que cada `.deb` contenga `libjvm.so`.
