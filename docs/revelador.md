# Revelar una carpeta de RAW

Abrí una carpeta o un archivo. La biblioteca conserva filtros, orden, selección activa
y desplazamiento al entrar y salir del revelador. Pausa la carga pendiente mientras
editás. La tira inferior sigue el orden filtrado; doble clic o Enter abre una foto,
y las flechas recorren el conjunto visible. Ctrl/clic y Shift/clic permiten seleccionar
varias para **Exportar…**. Sin selección se exporta la foto activa.

## Controles

Los paneles derechos se pliegan desde su título. La organización de formularios,
separadores y paneles toma como referencia [AEM Components](https://www.aemcomponents.dev/),
con controles Swing y un entorno neutro para observar la fotografía.

- **Ajustar** muestra el preview de hasta 2048 píxeles en su lado mayor. **100 %**
  solicita el render completo y muestra un píxel por píxel de pantalla lógica Java.
  En pantallas HiDPI el escalado Java sigue siendo aplicable. La rueda amplía/reduce;
  arrastrá con **Desplazar** o con el botón central para recorrer la foto.
- **Antes / después** alterna entre el RAW con balance de cámara y la receta actual.
  El histograma RGB corresponde a la salida sRGB editada, antes de la superposición de máscara.
- **Luz y color** incluye exposición (±5 EV), temperatura y matiz relativos al balance
  de cámara, contraste, altas luces, sombras, blancos, negros, saturación y vibrancia.
  Temperatura usa una escala relativa, no una medición Kelvin. El cuentagotas propone
  una corrección relativa a partir de la zona elegida; revisar el resultado visualmente.
- Elegí **Recortar** y arrastrá un rectángulo sobre la foto. **Quitar recorte** conserva
  la rotación y el enderezado. Rotar 90° reinicia el recorte para mostrar el nuevo encuadre.
  Enderezado conserva el tamaño del lienzo: las esquinas pueden quedar negras; recortalas.
- **Nueva máscara** crea una capa independiente. Elegí pincel, borrador o un gradiente
  en Herramienta; arrastrá sobre la fotografía. Un clic también deja un punto de pincel.
  En un radial el comienzo es el centro y el final define los radios horizontal/vertical.
  Tamaño es el radio del pincel como fracción de la altura original; suavidad y opacidad
  afectan su cobertura. **Restar trazo**, borrador e **Invertir** modifican la cobertura.
  Los controles locales son exposición, contraste, sombras, altas luces y saturación.
  **Ver máscara** muestra la cobertura roja sin incluirla en las exportaciones.
- Cada liberación de slider o trazo es una acción de historial. Deshacer/rehacer conserva
  hasta 100 acciones por fotografía durante la sesión; el historial no se persiste.
  Ctrl+Z/Ctrl+Y y flechas funcionan con foco en la fotografía, sin interferir al escribir.

## Correcciones de lente

**Automático** sólo aplica una coincidencia de cámara/lente inequívoca. Si hay varias,
el panel muestra las candidatas y requiere pasar a Manual. Si faltan metadatos o no
existe coincidencia, no elige una lente por aproximación silenciosa.

**Manual** permite escribir/buscar un fabricante y buscar modelos por texto o focal.
La búsqueda normal usa compatibilidad de cámara de Lensfun. **Incluir otras monturas**
amplía a todos los perfiles para lentes adaptados o cámaras desconocidas. Seleccioná
modelo y variante. Focal y apertura se toman de EXIF; pueden completarse manualmente
(0 significa sin datos). Cada corrección tiene su casilla: una calibración no disponible
queda deshabilitada. La distancia de enfoque no forma parte de esta versión: Lensfun
recibe 1000 metros como aproximación de infinito para viñeteo.

El sidecar contiene una copia XML de la calibración seleccionada, inclusive en modo
Automático. Actualizar la base no reemplaza esa copia. Volver a Automático elimina la
selección manual y realiza una nueva detección. El modo Desactivado conserva la elección
pero omite sus correcciones. Los perfiles proceden de la base Lensfun local (licencia
CC BY-SA de la base; bibliotecas LibRaw/Lensfun/Little CMS bajo sus propias licencias).
No se incluyen RAW particulares ni la base completa en el repositorio.

## Guardado y conflictos

La receta se guarda junto al original: `foto.ARW.photoraw.json`, esquema 1. Incluye
SHA-256 del RAW, ajustes, geometría, trazos, datos de captura utilizados y calibración.
El RAW nunca es destino de escritura. Cada gesto terminado se guarda en un trabajador
serial, con archivo temporal, sincronización y reemplazo atómico. El archivo `.lock`
permite coordinar instancias de la aplicación y puede permanecer vacío en la carpeta.

Si cambió el RAW, el JSON está dañado, su versión no es compatible o cambió externamente,
se informa el error sin sobrescribirlo. Un fallo de guardado conserva el borrador en memoria.
**Reintentar guardado** sirve después de corregir permisos/espacio disponible. La ventana
espera los guardados al cerrar y permanece abierta si aún fallan; no resuelve conflictos
externos descartando una de las versiones automáticamente. Conservá el archivo externo
antes de resolverlo. No borrar el original ni editar el sidecar durante un guardado.

## Exportación y color

Elegí carpeta y formato. La cola captura las recetas al comenzar; cambios posteriores
no alteran el lote. Un error no detiene los demás archivos. Cancelar deja terminar la
llamada nativa en curso y evita publicar ese resultado. Las salidas se escriben en un
temporal del directorio destino y se publican con un nombre libre (`-1`, `-2`, etc.).
La publicación usa enlaces duros atómicos: requiere un filesystem Linux que los soporte.
Un destino sin ese soporte produce un error, nunca una salida parcial presentada como final.

JPEG usa sRGB, 8 bits y calidad 95 %. TIFF usa sRGB, 16 bits por canal. Ambos incluyen
perfil ICC sRGB. No se copian EXIF, GPS ni notas de cámara. Se exporta la resolución
completa resultante del recorte; aún no hay presets de tamaño o calidad.

Pipeline: LibRaw con balance de cámara/relativo, gamma 1, salida Rec. 2020 de 16 bits y brillo automático
desactivado → normalización a Rec. 2020 lineal float32 → viñeteo/remuestreo óptico → ajustes
globales/locales → recorte/enderezado/rotación → Little CMS a sRGB. Viñeteo se evalúa en
coordenadas fuente antes de remuestrear, como requiere Lensfun. Las máscaras se evalúan
usando el mapa óptico verde hacia el original orientado; zoom y encuadre usan ese mismo mapa.

La exposición multiplica por `2^EV`; sombras/altas luces ponderan exposición mediante
luminancia, contraste pivota en 0,18 lineal, blancos/negros agregan offsets ponderados,
saturación/vibrancia modifican distancia a luminancia. Se conservan valores flotantes
fuera de [0,1] hasta la transformación de salida. La entrada LibRaw de 16 bits ya puede
contener recorte de altas luces: esto no es reconstrucción de zonas saturadas del sensor.
No hay soft proofing ni selección del perfil ICC físico de pantalla; el display es sRGB.

## Instalación y límites

Maven compila el puente C durante `generate-resources`. En Linux:

```sh
sudo apt install libraw-dev liblensfun-dev liblcms2-dev pkg-config build-essential xvfb
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify
java -jar app/target/photo-raw-lab-app-0.1.0-SNAPSHOT.jar
```

El paquete Debian incluye Java y el puente. Sus dependencias de ejecución son LibRaw,
Lensfun, `liblensfun-data-v1`, Little CMS y `x11-xserver-utils`.

Uso local sin conexión, carpetas sin recursión y una caché de RAW reducido activa.
El zoom 100 % y la exportación necesitan varios buffers a resolución completa y pueden
consumir más de 1 GB para 24 MP. No hay GPU ni render por teselas. La validación A7 III /
FE 28–70, RAW sin comprimir, patrones fotométricos y una sesión de 1000 capturas distintas sigue pendiente;
consultar las mediciones reales en [verificación](verification/revelador.md).
