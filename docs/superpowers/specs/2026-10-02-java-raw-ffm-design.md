# Photo RAW Lab MVP

Una aplicación Swing Java 25 abre fotografías RAW desde un selector y las muestra ajustadas a la ventana, conservando proporciones. También acepta una ruta como argumento. El contenido determina el formato, incluyendo archivos renombrados a `.raw`.

`domain.RgbImage` protege dimensiones y píxeles RGB con copias defensivas. `domain.RawImageDecoder.decode(Path)` devuelve `RgbImage` o lanza `IOException`. El dominio no depende de Swing ni de FFM.

`app.LibRawDecoder` usa la API C de LibRaw 0.21 mediante FFM. Cada decode posee arena, handler y bitmap nativo, libera los recursos en finally y copia los píxeles al heap antes de liberarlos. Produce RGB sRGB de 8 bits mediante dcraw_process, no una miniatura. Layout de libraw_processed_image_t: type@0 int; height@4, width@6, colors@8, bits@10 ushort; data_size@12 uint; data@16. El header 0.21.5 es la referencia ABI.

`app.RawViewerFrame` coordina una apertura por vez fuera del EDT y muestra estado/error recuperable. `app.RawImagePanel` traduce RGB a BufferedImage y dibuja con proporciones conservadas. Abrir se deshabilita mientras carga; cerrar no libera recursos nativos de otro hilo.

CI ejecuta Maven/JUnit bajo Xvfb con LibRaw instalado, incluyendo un RAW real de cámara. Cada merge publica JAR, Debian amd64 con runtime Java propio y CHANGELOG. El Debian declara dependencia de LibRaw. Verificar también la ventana real y el launcher empaquetado.

Decisiones autónomas: el plan original no existe en disco ni en GitHub; se reconstruye desde el objetivo autorizado. Swing es el stack desktop por defecto. Linux amd64 es el MVP distribuido. Fuera de alcance: edición, exportación y formatos sin soporte de LibRaw.
