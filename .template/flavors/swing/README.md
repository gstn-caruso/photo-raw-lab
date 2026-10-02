# {{name}}

{{description}}

## Ejecutar

`mvn -q -pl app -am package` y luego `java -jar app/target/{{slug}}-app-0.1.0-SNAPSHOT.jar`.

## Testear

`mvn -B verify`

Cada merge a `main` publica un JAR y un `.deb` con runtime Java incluido, incluso para `docs:` y `chore:`. El PR debe actualizar `CHANGELOG.md`.
