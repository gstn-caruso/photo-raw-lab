---
description: Compilar y abrir Photo RAW Lab con Maven en el escritorio local
disable-model-invocation: true
---

Desde la raíz de este repositorio, ejecutá:

```bash
mvn -B -pl app -am compile exec:exec
```

Usá la ejecución en segundo plano de la herramienta Bash para mantener la app
abierta al terminar la respuesta. Conservá la sesión gráfica actual; la ventana
tiene que aparecer en el escritorio del usuario.

Maven compila `domain` y `app`, y el plugin configurado en `app/pom.xml` lanza
`photorawlab.app.Main` con acceso nativo habilitado. Requiere Java 25 (asdf), Maven
y `libraw.so.23` disponibles.

Revisá la salida: si aparece un error de compilación o de arranque, informalo.
Si arranca, confirmá brevemente que la app quedó abierta. El proceso sigue activo
hasta que el usuario cierra la ventana; no esperes a que termine para responder.
