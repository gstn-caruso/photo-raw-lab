# {{name}}

{{description}}

## Ejecutar

`mvn -q -pl app -am package` y luego `java -jar app/target/{{slug}}-app-0.1.0-SNAPSHOT.jar`. Abrí `http://localhost:8080/`.

## Testear

```
mvn -B test
```

Cada merge a `main` publica un JAR y un `.deb` con runtime Java incluido, incluso para `docs:` y `chore:`. El PR debe actualizar `CHANGELOG.md`.

## Instalación

El `.deb` incluye Java 25. Bajá el `.deb` del [último release](https://github.com/{{owner}}/{{slug}}/releases/latest)
en GitHub e instalalo:

```
sudo apt install ./{{slug}}_<versión>_amd64.deb
```

El lanzador queda en `/opt/{{slug}}/bin/{{slug}}`.
