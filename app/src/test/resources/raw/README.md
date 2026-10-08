# RAW de prueba

`kodak-dc50.kdc` es el archivo `RAW_KODAK_DC50_é.KDC` de [rawpy](https://github.com/letmaik/rawpy/tree/main/test), obtenido originalmente de [rawsamples.ch](https://www.rawsamples.ch/).

La fotografía tiene licencia [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-nc-sa/4.0/), según el README del proveedor. Se conserva sin cambios, con un nombre ASCII para facilitar distribución. La licencia MIT del código no se aplica a esta fotografía. No se incluye en el JAR de producción.

Se utiliza para probar la decodificación completa de datos de cámara mediante LibRaw; las pruebas también copian su contenido a un nombre con extensión `.raw`.

Contiene una miniatura RGB de 96 × 64 y produce un revelado de 768 × 512. Las pruebas generan copias temporales: truncan antes de los datos del sensor (offset TIFF `0x4d00`), anulan o invalidan el offset de la miniatura (tag StripOffsets `0x0111` en `0x76`, valor en `0x7e`) y cambian la orientación TIFF (tags `0x0112` en `0x82` y `0x2c6`). El archivo distribuido permanece intacto. La conversión JPEG se prueba con imágenes generadas en memoria.

SHA-256: `37e290dbd0053f00e508d02a6b3a2a990432dad1eb74c40a52ca899f0f225ecc`.
