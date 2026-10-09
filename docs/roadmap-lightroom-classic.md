# Photo RAW Lab: hoja de ruta hacia una experiencia tipo Lightroom Classic

Fecha: 2026-10-09. Estado: plan de producto; implementación pendiente salvo la
base indicada abajo. Línea base: `fef4afe` en `main`.

## Objetivo y alcance

Convertir el visor actual en una aplicación de escritorio para el flujo completo
de fotografía: importar → organizar → seleccionar → revelar sin modificar el
original → exportar → presentar y archivar. La similitud buscada abarca el flujo
de trabajo, la organización de la interfaz y las herramientas, con identidad
propia. No se promete equivalencia de resultados con el motor de Adobe.

El primer producto útil termina en la etapa 5: una sesión de trabajo puede
guardarse, reabrirse y producir fotografías exportadas con sus ajustes. Las
etapas posteriores amplían calidad y cobertura. Este documento reemplaza al
plan del MVP como guía de evolución; el plan y la especificación originales se
conservan como antecedentes. No autoriza implementar todas las etapas juntas.

Se mantiene Java 25, Maven, JUnit, Swing y LibRaw mediante FFM, con Linux amd64
como plataforma inicial. Catálogo y originales locales, uso sin conexión y
dominio independiente de Swing, almacenamiento y bibliotecas nativas.

La referencia funcional se contrastó con documentación oficial de Adobe:
[espacio de trabajo](https://helpx.adobe.com/lightroom-classic/desktop/workspace/workspace-basics.html),
[catálogos](https://helpx.adobe.com/lightroom-classic/desktop/manage-catalogs-and-files/lightroom-catalog-basics.html)
y [Revelar](https://helpx.adobe.com/lightroom-classic/desktop/process-and-develop-photos/develop-module-tools.html).
La organización por módulos y el catálogo que conserva instrucciones de edición
son las referencias principales; las prioridades y contratos siguientes son
decisiones propuestas para Photo RAW Lab.

## Base existente y brechas

Verificado por lectura del README, clases y tests; no equivale a una nueva
ejecución de la aplicación ni de la suite.

| Área | Disponible | Brecha hacia el objetivo |
| --- | --- | --- |
| Biblioteca | Grilla adaptable, paleta gris, previews embebidas y fallback RAW | Paneles, tira de fotos, selección múltiple, lupa y comparación |
| Exploración | Una carpeta, último directorio recordado, abrir archivo | Importación, subcarpetas, catálogo persistente, relocalización |
| Organización | EXIF, filtros por captura/cámara/lente/tipo, orden por nombre/modificación/tamaño | Calificaciones, banderas, etiquetas, palabras clave, colecciones y búsqueda |
| Imagen | Revelado LibRaw a RGB sRGB de 8 bits y ajuste proporcional | Pipeline de alta precisión, zoom 1:1, gestión de color y ajustes editables |
| Trabajo en segundo plano | Carga secuencial y descarte de resultados obsoletos | Caché persistente, prioridades de carga, virtualización y tareas visibles |
| Entrega | Tests de dominio/Swing/RAW real, CI y JAR/.deb con runtime | Nuevos fixtures y mediciones para catálogo, edición, exportación y recuperación |

Evidencia en [README](../README.md), [dominio](../domain/src/main/java/photorawlab/domain/),
[aplicación](../app/src/main/java/photorawlab/app/) y
[verificaciones existentes](verification/). El soporte de metadatos depende del
formato; una preview de cámara puede diferir del revelado completo. No presentar
ninguno de los dos como soporte universal de cámaras o fidelidad de edición.

## Experiencia de interfaz objetivo

- Barra superior con Biblioteca y Revelar; Exportar como acción siempre accesible.
  Incorporar los demás módulos cuando tengan un recorrido funcional completo.
- Panel izquierdo para navegador, carpetas, colecciones y presets según el módulo.
- Centro para grilla, lupa, comparación o imagen en edición; selección activa
  compartida entre vistas, sin perder filtros, orden ni posición de desplazamiento.
- Panel derecho para metadatos e histograma en Biblioteca, y ajustes en Revelar.
- Tira inferior de fotos para recorrer la selección; paneles plegables, pantalla
  completa, navegación por teclado y controles legibles con distintos DPI.
- Estado visible de importación, previews, revelado y exportación, con progreso,
  cancelación y errores por archivo; una falla no bloquea el resto de la sesión.

No agregar controles sin comportamiento. Validar cada vista con fotos de distintas
orientaciones, ventanas pequeñas, foco de teclado y escala de escritorio.

## Etapas y condiciones de salida

Las etapas se ejecutan en orden, por pequeños escenarios completos. Cada una
depende de la anterior salvo donde se indica una dependencia más precisa. Los
criterios son expectativas futuras, no capacidades ya implementadas.

### 1. Biblioteca y selección fluida

**Estado:** pendiente. **Dependencia:** base actual.

- [ ] Separar selección activa de apertura; selección múltiple con teclado y ratón.
- [ ] Lupa con ajuste a ventana, zoom 1:1, desplazamiento y anterior/siguiente.
- [ ] Tira de fotos, paneles plegables y regreso a la misma posición de la grilla.
- [ ] Comparación de dos fotos y vista de selección de un grupo.
- [ ] Conservar previews y estado al alternar vistas, sin releer toda la carpeta.

**Salida verificable:** seleccionar tres fotos, recorrerlas en lupa, comparar dos
y volver a la grilla conserva selección, filtros, orden y posición. Cambiar de
carpeta durante una carga nunca muestra fotos de la anterior. Probar carpeta
vacía, foto única, archivo corrupto y cierre durante una llamada nativa.

### 2. Catálogo persistente e importación segura

**Estado:** pendiente. **Dependencia:** etapa 1.

- [ ] Crear, abrir y recordar un catálogo; identidad estable de foto separada de su ruta.
- [ ] Importar carpetas con recursión opcional; comenzar por agregar en ubicación
  original y luego copiar desde tarjeta/carpeta con vista previa de destino.
- [ ] Política explícita de duplicados y colisiones; renombrado y metadatos iniciales.
- [ ] Registrar archivos desconectados y relocalizarlos conservando identidad.
- [ ] Persistir metadatos, selección de trabajo y configuración relevante.
- [ ] Transacciones, versión de esquema, migraciones, backup y restauración comprobada.
  Diferenciar backup del catálogo de backup de originales.
- [ ] Quitar del catálogo separado de borrar del disco, con confirmación y política
  de recuperación para la operación destructiva.

**Salida verificable:** importar una carpeta dos veces según la política de
duplicados, reiniciar y conservar las mismas identidades. Desconectar y
relocalizar el origen conserva organización. Interrumpir una copia no registra
un original incompleto ni sobrescribe otro; restaurar el backup recupera el
catálogo. Al cerrar esta etapa debe existir una política probada de persistencia
para los datos que incorporen las etapas siguientes.

**Decisión previa al primer cambio:** elegir almacenamiento local transaccional
(candidato: SQLite vía JDBC) y formato/versionado del catálogo mediante una
prueba pequeña de cierre abrupto, restauración y migración. No sumar un servidor.

### 3. Organización fotográfica completa

**Estado:** pendiente. **Dependencia:** catálogo de etapa 2.

- [ ] Estrellas 0–5, seleccionada/rechazada/sin bandera y etiquetas de color.
- [ ] Aplicación en lote, deshacer y atajos de selección sin activar acciones al
  escribir en campos de texto.
- [ ] Palabras clave, título, descripción, autor y copyright; distinguir EXIF
  leído del archivo de anotaciones editadas por el usuario.
- [ ] Colecciones, conjuntos, colecciones inteligentes y apilado manual.
- [ ] Búsqueda textual y filtros combinados por atributos, metadatos y estado;
  orden por captura separado de modificación, con regla explícita para ausentes.
- [ ] Lectura/escritura de un subconjunto documentado de XMP en sidecars, con
  detección de conflictos y preservación de campos desconocidos.

**Salida verificable:** calificar y etiquetar un lote, crear una colección
inteligente y reiniciar conserva resultados y conteos. Relocalizar una foto no
pierde sus anotaciones. Un conflicto de sidecar se informa antes de sobrescribir;
el contrato XMP enumera los campos compatibles, sin prometer importar ajustes
propietarios de revelado de Lightroom.

### 4. Revelado no destructivo básico

**Estado:** pendiente. **Dependencias:** etapas 2–3 y prueba técnica de imagen.

- [ ] Resolver primero una ruta de RAW a imagen de trabajo de alta precisión
  (16 bits o flotante), espacio de color explícito y conversión de pantalla.
  El RGB de 8 bits actual queda como representación de visualización; no es
  suficiente como única entrada del motor de edición.
- [ ] Receta versionada por foto: balance de blancos, exposición, contraste,
  altas luces, sombras, blancos, negros, saturación y vibrancia.
- [ ] Recorte, enderezado y rotación; histograma y avisos de clipping calculados
  sobre la imagen correspondiente al estado de edición.
- [ ] Reset, deshacer/rehacer, historial persistente, instantáneas y antes/después.
- [ ] Previsualización interactiva de menor resolución y render final desde el
  original con la misma receta; una edición nueva invalida resultados viejos.
- [ ] Guardar ajustes sin tocar el RAW y recuperarlos al reiniciar.

**Prueba técnica obligatoria antes de ofrecer sliders:** medir precisión,
color, recursos nativos y tiempos con varios RAW reales y patrones conocidos.
Definir orden de operaciones, rango útil y algoritmo de cada ajuste. LibRaw
decodifica; no asumir que aporta por sí solo todas las herramientas de revelado.

**Salida verificable:** editar exposición, balance de blancos y recorte, cerrar,
reabrir y reproducir el resultado dentro de tolerancias declaradas. El hash del
original permanece idéntico. Reset vuelve al revelado base; deshacer/rehacer
recupera estados. La imagen de pantalla y el render final conservan composición
y color dentro del contrato documentado; no se usa el JPEG embebido como edición
final ni se compara contra Adobe como un oráculo de píxeles exactos.

### 5. Exportación y trabajo por lotes — primer flujo completo

**Estado:** pendiente. **Dependencia:** motor y recetas de etapa 4.

- [ ] Exportar JPEG sRGB con calidad, dimensiones, nombre y destino elegidos.
- [ ] Exportar TIFF de 16 bits con perfil explícito una vez validado el encoder.
- [ ] Política de metadatos: conservar, reducir o retirar GPS/datos personales.
- [ ] Presets de revelado y exportación; copiar/sincronizar ajustes elegidos en lote.
- [ ] Copias virtuales con recetas independientes y original compartido.
- [ ] Cola con progreso, cancelación, errores individuales y reintento; colisiones
  resueltas antes de escribir, salida temporal y publicación del archivo completo.

**Salida verificable:** importar una sesión, seleccionar favoritas, revelar,
reiniciar y exportar un lote. Abrir los archivos en un lector independiente y
comprobar dimensiones, perfil, profundidad y política de metadatos. Un archivo
fallido no impide exportar los demás; cancelar no deja una salida parcial como
terminada ni modifica originales. Esta es la condición de primer producto útil.

### 6. Calidad de imagen y revelado avanzado

**Estado:** pendiente. **Dependencias:** etapas 4–5.

- [ ] Curvas, mezcla HSL, blanco y negro y gradación de color.
- [ ] Enfoque, reducción de ruido, corrección de aberración, distorsión y viñeteo,
  con perfiles de cámara/lente cuya procedencia y licencia estén documentadas.
- [ ] Máscaras por pincel, gradiente lineal/radial y rango de color/luminancia.
- [ ] Clonar/corregir manchas, perspectiva y transformaciones.
- [ ] Gestión ICC de pantalla y exportación, soft proofing y advertencias de gamut.
- [ ] Previews editables sin originales disponibles, con limitaciones explícitas
  y render de máxima calidad al reconectar el RAW.

**Salida verificable:** máscaras y ajustes se reproducen al reabrir, cambian sólo
la región esperada y conservan geometría tras el recorte. Comparar fixtures de
ruido/color/lentes con resultados de referencia y tolerancias definidas para el
algoritmo propio; documentar cámaras, perfiles y operaciones realmente soportados.

### 7. Presentación, salida y archivo

**Estado:** pendiente. **Dependencia:** exportación de etapa 5; color de etapa 6
para impresión fiable. Implementar un módulo por vez.

- [ ] Mapa: GPS, búsqueda por ubicación y geotagging; elegir proveedor, licencia,
  caché y comportamiento sin conexión antes de integrar mapas.
- [ ] Presentación: reproducción de una colección, orden, duración y pantalla completa.
- [ ] Impresión: hojas de contacto, plantillas, márgenes y perfiles de salida.
- [ ] Libro: composición de páginas y exportación de un documento imprimible.
- [ ] Web: galería estática exportada localmente, con selección de metadatos públicos.
- [ ] Archivo: verificar originales por checksum y restaurar catálogo y fotos desde
  backups independientes; integración con editor externo mediante copias derivadas.

**Salida verificable:** cada módulo tiene un recorrido desde una colección hasta
su salida utilizable; validar impresión/documentos/galería con herramientas
independientes. GPS ausente no rompe el mapa y retirar metadatos privados afecta
también las salidas de presentación. Restaurar en una ubicación nueva conserva
colecciones, recetas y vínculos a originales.

### 8. Extensiones que requieren evaluación propia

**Estado:** diferidas; no bloquean el flujo completo.

HDR y panoramas, captura conectada a cámara, reconocimiento de personas,
selección asistida, máscaras automáticas, reducción de ruido por IA, eliminación
generativa, aceleración GPU, video, segundo monitor, plugins, otras plataformas
y sincronización remota. Cada capacidad necesita un caso de uso, prueba de
viabilidad, costo de mantenimiento y tratamiento de datos antes de entrar al
backlog comprometido. Compatibilidad con `.lrcat`, presets Adobe y su motor de
revelado no se presume a partir de la similitud de interfaz.

## Rendimiento y robustez en todas las etapas

Medir al inicio con el equipo, formatos, resolución, disco y caché identificados.
Objetivos iniciales propuestos para un catálogo de 10.000 fotos: ninguna lectura
o llamada nativa en el EDT; feedback de selección en menos de 100 ms y primera
página de previews cacheadas en menos de 1 s. Para Revelar, fijar el presupuesto
de preview y render final tras la prueba técnica de etapa 4. Estos valores son
metas de diseño, no mediciones actuales ni garantías para cualquier hardware.

Introducir caché en memoria/disco con cuotas, invalidación por original/receta y
evicción; priorizar fotos visibles y activa; virtualizar la grilla para no tener
una imagen y un componente por cada archivo. La concurrencia nativa se decide
con mediciones de memoria y seguridad de LibRaw. Cancelar deja terminar una
llamada nativa que no sea interrumpible y descarta su resultado sin liberar
recursos que siga usando. Probar memoria acotada, disco lleno, archivos dañados,
rutas Unicode, cierre abrupto y originales en un volumen desconectado.

## Responsabilidades y decisiones técnicas

Son responsabilidades propuestas, no una lista de clases que deba crearse de
antemano. Concretar colaboradores y nombres al diseñar el siguiente escenario.

| Responsable | Información o decisión propia | Frontera / colaboración |
| --- | --- | --- |
| Foto catalogada | Identidad, ubicación conocida, disponibilidad y anotaciones | Catálogo; no componentes Swing ni handles nativos |
| Catálogo y colecciones | Pertenencia, consultas y reglas de organización | Puerto de almacenamiento; no SQL en el dominio |
| Plan de importación | Destinos, duplicados y colisiones | Coordinador delega copias a filesystem y commits a almacenamiento |
| Selección de biblioteca | Foto activa, conjunto seleccionado y recorrido visible | Vistas Swing presentan y envían acciones |
| Receta de revelado | Ajustes, orden semántico, versiones e invariantes | Motor interpreta receta; historial conserva estados |
| Motor de imagen | Transformación reproducible y precisión/color | Adaptador LibRaw decodifica; adaptadores ICC y encoder convierten |
| Historial de edición | Deshacer, rehacer, instantáneas y versiones de receta | Persistencia guarda estados; UI comunica acciones |
| Plan de exportación | Tamaños, formato, privacidad y colisiones | Cola coordina motor, encoder y escritura de archivos |
| Caché y tareas | Cuotas, prioridades, vigencia de resultados y progreso | Frontera de ejecución; dominio no conoce EDT ni SwingWorker |

Aplicar RDD y control delegado de `code-criteria/DIGEST.md`: evitar concentrar
catálogo, edición y exportación en `RawViewerFrame` (God Service, #78); nombrar
mensajes por intención (Intention Revealing Selector, #4). No reestructurar todo
el visor por anticipación: separar responsabilidades cuando lo exija el escenario,
con cambios estructurales y funcionales en commits distintos.

## Siguiente escenario recomendado y forma de entrega

Empezar por etapa 1: **selección activa y navegación en lupa**. Dada una carpeta
de tres fotos, seleccionar la segunda y abrirla; avanzar a la tercera y volver
a la grilla debe conservar la tercera como activa y mantener filtros y orden.
Contemplar extremos del recorrido, error de decode y cambio de carpeta durante
la carga. Este escenario entrega una mejora visible sin imponer todavía un
formato de catálogo.

Antes de implementarlo, releer estado de Git y evidencia, decidir quién conserva
la selección y quién coordina la carga, y escribir el test observable más pequeño
que falle. No crear todas las responsabilidades de la tabla de una vez.

Cada escenario usa feature branch en el mismo checkout, sin worktrees, TDD y
commit convencional manual al cerrar en verde; sin TCR. Verificar dominio con
JUnit, fronteras con fixtures reales y Swing bajo Xvfb, según lo afectado:

```text
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE xvfb-run -a mvn -B verify
```

Registrar expectativa, rojo/verde o chequeo manual reproducible y límites en
`docs/verification/`; revisión independiente antes de publicar, PR con changelog
y merge sólo con CI verde. Usar la automatización semántica existente: `feat`
minor, `fix`/`perf` patch, incompatible major y documentación sola sin bump.
Actualizar este plan al cerrar una etapa con PR, evidencia y limitaciones reales.
No asignar fechas ni versiones hasta estimar el siguiente alcance.

## Verificación de este plan

**Expectativa:** una persona puede identificar lo existente, el primer escenario
y el orden de trabajo hasta importar, organizar, revelar y exportar, además de
las capacidades avanzadas y los riesgos técnicos sin confundirlos con entregas.

**Antes:** el único plan cubría la ventana RAW del MVP; su especificación dejaba
edición y exportación fuera de alcance y su lista seguía sin marcar. No había
una hoja de ruta integral pese a las mejoras posteriores de Biblioteca.

**Chequeo reproducible:** leer este documento siguiendo una sesión desde una
carpeta hasta JPEG exportado; comprobar que cada etapa indica dependencias y
salida observable; contrastar la tabla de base con README y las clases enlazadas;
seguir desde README el enlace al plan. Revisar `git diff --check` y confirmar
que el diff modifica sólo documentación.

**Después:** el recorrido queda cubierto por etapas 1–5, con catálogo duradero,
recetas persistentes, protección del original y validación de las salidas. Las
etapas 6–8 explicitan ampliaciones y límites. La inspección documental no verifica
la futura calidad de imagen, tiempos, almacenamiento ni compatibilidad: requieren
las pruebas señaladas en cada etapa.
