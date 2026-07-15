# Explicación del proyecto: Ecualizador de Histogramas

## 1. ¿Qué hace el proyecto?

Este proyecto es una aplicación de escritorio desarrollada con Java 21 y JavaFX. Permite cargar una imagen PNG o JPG, observar su histograma y producir una imagen nueva mediante cuatro operaciones:

1. conversión a escala de grises;
2. ecualización del histograma;
3. ajuste manual del brillo;
4. slice o segmentación de intensidades.

El programa conserva la imagen original. Cada procesamiento crea otra imagen y la coloca en `imagenProcesada`, de modo que es posible alternar entre la imagen original y el último resultado.

## 2. Organización general

El proyecto sigue una separación parecida al patrón MVC:

- `MainApp`: inicia JavaFX y carga la interfaz FXML.
- `main-view.fxml`: declara los botones, deslizadores, visor y gráficos.
- `MainController`: responde a las acciones del usuario y conecta la interfaz con los algoritmos.
- `ImageProcessor`: contiene los algoritmos que modifican imágenes.
- `HistogramAnalyzer`: analiza una imagen y cuenta sus intensidades, pero no la modifica.
- `styles.css`: define la presentación visual.
- las clases de `src/test`: verifican automáticamente los resultados principales.

El flujo básico es:

```text
Usuario -> interfaz FXML -> MainController -> ImageProcessor
                                  |
                                  +-> HistogramAnalyzer -> AreaChart
```

## 3. Conceptos fundamentales

### 3.1 Píxel y canales RGB

Una imagen digital es una matriz de píxeles. Cada píxel posee coordenadas `(x, y)` y un color. En JavaFX, un `Color` tiene, entre otros, estos componentes:

- rojo (`red`);
- verde (`green`);
- azul (`blue`);
- opacidad (`opacity`).

JavaFX entrega los canales RGB como números `double` entre `0.0` y `1.0`, no entre 0 y 255. Por ejemplo:

```text
Color negro:   R=0.0, G=0.0, B=0.0
Color blanco:  R=1.0, G=1.0, B=1.0
Rojo puro:     R=1.0, G=0.0, B=0.0
```

Por eso el código multiplica por 255 cuando necesita representar un nivel mediante un entero de 8 bits:

```java
int nivel = (int) Math.round(valorEntreCeroYUno * 255);
```

`Math.round` selecciona el entero más cercano y el cast a `int` deja el resultado con el tipo usado como índice del arreglo.

### 3.2 Intensidad

En procesamiento de imágenes, intensidad suele significar qué tan oscuro o claro es un píxel expresado mediante un solo número. En este proyecto se usa la escala de 256 niveles:

- `0`: negro;
- `255`: blanco;
- valores intermedios: distintos niveles de gris.

Un píxel a color tiene tres canales y no una única intensidad evidente. Por eso es necesario aplicar una regla que reduzca `(R, G, B)` a un número. `HistogramAnalyzer` usa luminancia perceptual para hacerlo.

### 3.3 Luminancia

La luminancia es una medida del brillo percibido por una persona. El ojo humano no responde con la misma sensibilidad a rojo, verde y azul. Es especialmente sensible al verde y menos sensible al azul. Por eso no se usa simplemente `(R + G + B) / 3`, sino una suma ponderada:

```text
Y = 0.299R + 0.587G + 0.114B
```

Los pesos suman 1:

```text
0.299 + 0.587 + 0.114 = 1
```

Así, si los tres canales son `0`, la luminancia es `0`; si son `1`, es `1`. El verde aporta 58.7 %, el rojo 29.9 % y el azul 11.4 %.

Ejemplo con colores puros:

```text
Rojo:  Y = 0.299 -> intensidad aproximada 76
Verde: Y = 0.587 -> intensidad aproximada 150
Azul:  Y = 0.114 -> intensidad aproximada 29
```

Aunque los tres colores tienen un canal al máximo, el verde puro se percibe más luminoso que el azul puro. La fórmula intenta representar esa percepción.

> Esta fórmula es una aproximación tradicional de luma para señales RGB. En sentido físico estricto, luminancia y luma no son exactamente lo mismo, pero “luminancia perceptual” describe adecuadamente su propósito dentro de este proyecto.

### 3.4 Brillo HSB y luminancia no son lo mismo

Esta diferencia es importante en el código:

- `HistogramAnalyzer.calcularIntensidad` usa `0.299R + 0.587G + 0.114B`.
- `ImageProcessor.calcularHistogramaBrillo` usa `Color.getBrightness()`.

En JavaFX, `getBrightness()` devuelve el componente **B** del modelo HSB/HSV. En términos sencillos, corresponde al mayor de los canales RGB:

```text
brillo HSB = max(R, G, B)
```

Para rojo, verde y azul puros, el brillo HSB es `1.0` en los tres casos. Sus luminancias, en cambio, son diferentes. Por lo tanto, el histograma que se presenta en el gráfico y el histograma interno usado al ecualizar describen propiedades relacionadas, pero no idénticas.

La razón práctica de usar HSB en la ecualización es que luego se puede reemplazar únicamente el brillo y conservar el tono y la saturación del color.

## 4. ¿Qué hacen `PixelReader`, `getColor` y `PixelWriter`?

### `imagen.getPixelReader()`

Devuelve un objeto `PixelReader` que permite leer los píxeles de una imagen. No devuelve un píxel ni un color: devuelve el **lector** con el que después se consulta cada coordenada.

```java
PixelReader lector = imagen.getPixelReader();
```

Si la imagen no tiene datos legibles, puede devolver `null`; de ahí las validaciones del proyecto.

### `lector.getColor(x, y)`

Lee el píxel ubicado en la columna `x` y la fila `y`, y devuelve un objeto JavaFX `Color`.

```java
Color color = lector.getColor(x, y);
```

De ese objeto se consultan `getRed()`, `getGreen()`, `getBlue()`, `getOpacity()`, `getHue()`, `getSaturation()` o `getBrightness()`.

Las coordenadas comienzan en cero. Para una imagen de ancho 800 y alto 600, `x` va de 0 a 799 y `y` de 0 a 599.

### `WritableImage` y `PixelWriter`

Una `Image` se utiliza principalmente para lectura. Para construir el resultado se crea una `WritableImage`, y su `PixelWriter` permite escribir colores:

```java
WritableImage resultado = new WritableImage(ancho, alto);
PixelWriter escritor = resultado.getPixelWriter();
escritor.setColor(x, y, nuevoColor);
```

Así el programa lee de la imagen de entrada y escribe en otra. No destruye ni sobrescribe la original.

## 5. La clase `HistogramAnalyzer`

Su única responsabilidad es convertir la imagen en una distribución estadística de intensidades. No cambia colores ni dimensiones.

### 5.1 ¿Qué representa `int[] histograma = new int[256]`?

El arreglo tiene una casilla para cada nivel posible:

```text
histograma[0]   = cantidad de píxeles con intensidad 0
histograma[1]   = cantidad de píxeles con intensidad 1
...
histograma[255] = cantidad de píxeles con intensidad 255
```

Todos comienzan en cero. Al visitar un píxel se calcula su intensidad y se incrementa exactamente la casilla correspondiente:

```java
histograma[intensidad]++;
```

Si se procesara una imagen de cuatro píxeles: negro, negro, gris 128 y blanco, las casillas relevantes quedarían así:

```text
histograma[0]   = 2
histograma[128] = 1
histograma[255] = 1
```

Las otras 253 casillas serían cero. La suma de todo el arreglo siempre debe ser `ancho * alto`.

### 5.2 Recorrido de la imagen

Los dos bucles recorren primero las filas y, dentro de cada fila, sus columnas:

```java
for (int y = 0; y < alto; y++) {
    for (int x = 0; x < ancho; x++) {
        Color color = lector.getColor(x, y);
        int intensidad = calcularIntensidad(color);
        histograma[intensidad]++;
    }
}
```

El orden no cambia el histograma porque solo se cuentan apariciones, pero asegura que todos los píxeles sean visitados una vez.

### 5.3 ¿Por qué existe `calcularIntensidad`?

Existe para encapsular la conversión de un `Color` RGB a una única intensidad de 0 a 255. Esto hace que `calcularHistograma` se concentre en recorrer y contar, mientras el método auxiliar se concentra en la fórmula matemática.

Funciona porque:

1. recibe canales entre 0 y 1;
2. calcula una suma ponderada cuyos pesos suman 1, así que el resultado queda entre 0 y 1;
3. multiplica por 255 para llevarlo a la escala del histograma;
4. redondea para obtener un índice entero entre 0 y 255.

Es `private` porque es un detalle interno: el resto de la aplicación solo necesita pedir el histograma completo.

### 5.4 ¿Qué se hace con el arreglo retornado?

`MainController.actualizarHistograma` recibe el arreglo y genera 256 puntos para un `AreaChart`:

```text
eje X = intensidad, de 0 a 255
eje Y = cantidad de píxeles de esa intensidad
```

Una concentración a la izquierda indica predominio de píxeles oscuros; a la derecha, píxeles claros. Un histograma estrecho suele indicar poco contraste. Uno distribuido a lo largo de un intervalo amplio suele indicar mayor variedad tonal.

El histograma no contiene la posición de cada píxel. Dos imágenes visualmente distintas pueden compartir el mismo histograma si tienen las mismas cantidades de intensidades distribuidas en posiciones diferentes.

## 6. Conversión a escala de grises

`ImageProcessor.convertirAEscalaGrises` calcula la misma luminancia perceptual y asigna ese resultado a los tres canales:

```java
new Color(gris, gris, gris, color.getOpacity())
```

Cuando `R = G = B`, no existe predominio de ningún canal y el resultado visual es gris. Se mantiene la opacidad original.

Ejemplo para rojo puro:

```text
gris = 0.299(1) + 0.587(0) + 0.114(0) = 0.299
nuevo color = (0.299, 0.299, 0.299)
```

## 7. El método `calcularHistogramaBrillo`

El código es:

```java
private int[] calcularHistogramaBrillo(Image imagen) {
    int[] histograma = new int[256];
    PixelReader lector = imagen.getPixelReader();
    int ancho = (int) imagen.getWidth();
    int alto = (int) imagen.getHeight();

    for (int y = 0; y < alto; y++) {
        for (int x = 0; x < ancho; x++) {
            int brillo = (int) Math.round(
                    lector.getColor(x, y).getBrightness() * 255);
            histograma[brillo]++;
        }
    }
    return histograma;
}
```

Paso a paso:

1. crea 256 contadores;
2. obtiene el lector de la imagen;
3. obtiene dimensiones enteras;
4. visita cada coordenada;
5. `getColor(x, y)` devuelve el color del píxel;
6. `getBrightness()` obtiene su brillo HSB entre 0 y 1;
7. se multiplica por 255 y se redondea;
8. se incrementa el contador de ese nivel;
9. se devuelve la distribución completa.

Lo retornado no es una imagen: es una tabla de frecuencias. Este histograma privado se usa para calcular cómo debe cambiar el brillo de cada píxel durante la ecualización.

## 8. ¿Cómo funciona la ecualización?

### 8.1 Objetivo

Una imagen puede usar solo una pequeña parte de los niveles disponibles. Por ejemplo, quizá todos sus brillos se encuentren entre 80 y 130. Aunque existen 256 niveles, la imagen aprovecha solo 51 y puede verse apagada o con poco contraste.

La ecualización construye una función de transformación basada en la distribución de la propia imagen. Su propósito es extender y redistribuir los niveles para aprovechar mejor el rango disponible. No garantiza que cada barra final tenga exactamente la misma altura: los niveles son discretos y grupos completos de píxeles se trasladan juntos.

### 8.2 Histograma acumulado o CDF

El arreglo `acumulado` guarda cuántos píxeles tienen brillo menor o igual a cada nivel:

```java
acumulado[0] = histograma[0];
for (int i = 1; i < acumulado.length; i++) {
    acumulado[i] = acumulado[i - 1] + histograma[i];
}
```

Si una parte del histograma fuera:

```text
nivel:       0   1   2   3
histograma:  2   3   0   1
acumulado:   2   5   5   6
```

Entonces `acumulado[1] = 5` significa que cinco píxeles tienen nivel menor o igual a 1. Al dividir el acumulado por el total se obtiene la función de distribución acumulada (CDF), es decir, la proporción de píxeles que queda hasta cada nivel.

El acumulado es creciente o se mantiene igual. Por eso respeta el orden: un brillo bajo nunca se transforma por encima de otro brillo que originalmente era mayor.

### 8.3 Primer nivel usado

El programa busca la primera casilla no vacía:

```java
while (primerNivelUsado < 256 && histograma[primerNivelUsado] == 0) {
    primerNivelUsado++;
}
```

Su acumulado se llama `acumuladoMinimo`. Se resta en la fórmula para hacer que el nivel mínimo realmente utilizado se transforme en cero. Sin esa resta, una imagen que no tenga píxeles negros podría seguir sin llegar a negro después de normalizar.

### 8.4 Fórmula de transformación

Para cada brillo anterior `r`, el proyecto calcula:

```text
nuevoBrillo = (acumulado[r] - acumuladoMinimo)
              / (totalPixeles - acumuladoMinimo)
```

Es una versión normalizada de la CDF:

- el primer nivel presente se lleva a `0.0`;
- el nivel cuyo acumulado alcanza el total se lleva a `1.0`;
- los demás se ubican proporcionalmente según cuántos píxeles se han acumulado.

`limitarValorColor` garantiza por seguridad que el resultado esté entre 0 y 1.

### 8.5 Ejemplo completo pequeño

Supóngase una imagen de 8 píxeles con estos brillos:

```text
50, 50, 50, 100, 100, 150, 150, 150
```

Frecuencias usadas:

```text
H[50]  = 3
H[100] = 2
H[150] = 3
```

Acumulados:

```text
CDFconteo[50]  = 3
CDFconteo[100] = 5
CDFconteo[150] = 8
```

El primer acumulado es 3 y el total es 8. La transformación queda:

```text
50  -> (3 - 3) / (8 - 3) = 0.0
100 -> (5 - 3) / (8 - 3) = 0.4
150 -> (8 - 3) / (8 - 3) = 1.0
```

En la escala 0–255, aproximadamente:

```text
50 -> 0
100 -> 102
150 -> 255
```

Los brillos originales ocupaban 50–150; los nuevos ocupan 0–255. Así aumenta el contraste global.

### 8.6 Conservación del color

En vez de sustituir R, G y B por gris, el programa crea el color ecualizado con:

```java
Color.hsb(
    colorOriginal.getHue(),
    colorOriginal.getSaturation(),
    brilloNuevo,
    colorOriginal.getOpacity()
)
```

El modelo HSB separa aproximadamente:

- `Hue`: tono, como rojo, verde o azul;
- `Saturation`: pureza del color;
- `Brightness`: claridad máxima del color.

Solo cambia `Brightness`; tono, saturación y opacidad se copian del original. Por eso la ecualización intenta mejorar contraste sin convertir la imagen a gris ni cambiar deliberadamente sus colores.

### 8.7 Caso especial de una imagen uniforme

Si todos los píxeles tienen el mismo brillo, no existe una distribución que extender. Además, el denominador de la fórmula sería cero. El código detecta el caso y devuelve una copia:

```java
if (primerNivelUsado == 256 ||
        totalPixeles == acumulado[primerNivelUsado]) {
    return copiarImagen(imagen);
}
```

## 9. Slice de intensidades

El slice funciona como una segmentación binaria. Primero convierte la imagen a gris. Después obtiene una intensidad entre 0 y 255 y pregunta si está dentro del intervalo inclusivo `[minimo, maximo]`:

```java
boolean estaEnRango = intensidad >= minimo && intensidad <= maximo;
```

Sin invertir:

```text
dentro del rango -> blanco
fuera del rango  -> negro
```

Con la casilla de inversión ocurre lo contrario. Esto sirve para resaltar regiones que poseen una franja de intensidades determinada. El resultado pierde los tonos originales porque deliberadamente solo usa negro y blanco.

Después de convertir a gris, los canales R, G y B tienen el mismo valor. Por eso es suficiente leer `getRed()` para recuperar la intensidad: cualquiera de los tres canales daría el mismo resultado.

## 10. Ajuste manual de brillo

`ajustarBrillo` recibe una cantidad de -100 a 100 y la transforma al intervalo -1 a 1:

```java
double cambio = cantidad / 100.0;
```

Luego suma ese valor por igual a R, G y B. Por ejemplo, `cantidad = 30` produce `cambio = 0.3`:

```text
(0.1, 0.2, 0.3) -> (0.4, 0.5, 0.6)
```

`limitarValorColor` recorta los resultados:

```java
Math.max(0, Math.min(1, valor))
```

Si se obtiene `1.2`, queda `1`; si se obtiene `-0.2`, queda `0`. Esto evita construir colores inválidos, aunque también implica pérdida de detalle cuando muchos valores llegan simultáneamente a negro o blanco.

Este ajuste no es ecualización: aplica la misma suma a todos los píxeles sin estudiar su distribución.

## 11. Funcionamiento del controlador

### Al iniciar

`initialize()` configura listeners de los deslizadores, actualiza etiquetas, prepara los gráficos y deshabilita los controles hasta que haya una imagen válida. Los listeners también impiden que el mínimo del slice supere al máximo.

### Al cargar una imagen

`cargarImagen()`:

1. abre un selector de PNG/JPG;
2. crea una `Image`;
3. valida que pueda leerse;
4. la guarda en `imagenOriginal`;
5. crea `imagenGris`;
6. establece inicialmente `imagenProcesada = imagenGris`;
7. actualiza ambos histogramas;
8. habilita los controles.

Por esa decisión, al cargar se muestra la original, pero el resultado procesado inicial es su copia gris.

### Origen de cada operación

Las operaciones no se encadenan sobre el último resultado:

- escala de grises parte de `imagenOriginal`;
- ecualización parte de `imagenOriginal`;
- ajuste de brillo parte de `imagenOriginal`;
- slice parte de `imagenGris`.

Por ejemplo, ecualizar y luego ajustar brillo no aplica brillo sobre la ecualización: el ajuste vuelve a partir de la original. `imagenProcesada` representa el último resultado, no una cadena acumulativa de filtros.

### Histogramas mostrados

Al cargar, el gráfico llamado “Histograma original” se calcula sobre `imagenGris`, no directamente sobre `imagenOriginal`. Como la escala de gris se produjo con la misma fórmula de luminancia usada por `HistogramAnalyzer`, representa la distribución luminosa de la original con pequeñas diferencias posibles por cuantización.

El segundo gráfico siempre se actualiza usando `imagenProcesada`.

### Restaurar y alternar

`restaurarImagen()` establece nuevamente `imagenProcesada = imagenGris`; no restaura el resultado procesado a una copia a color de la original. Luego muestra la vista original. `alternarVista()` solo decide qué imagen se presenta; no procesa nada.

### Guardar

Solo se guarda `imagenProcesada`, siempre en PNG. La imagen original no se sobrescribe automáticamente.

## 12. Relación entre FXML y controlador

El atributo `fx:controller` enlaza el FXML con `MainController`. Un elemento con `fx:id`, por ejemplo:

```xml
<Button fx:id="botonEcualizar"
        onAction="#ecualizarImagen"
        text="Ecualizar histograma" />
```

se conecta con el campo `@FXML botonEcualizar`. Al pulsarlo, JavaFX llama al método `@FXML ecualizarImagen()`.

Los dos `AreaChart` tienen un eje X fijo entre 0 y 255 y un eje Y que representa cantidades. El controlador agrega una serie de 256 puntos y desactiva los símbolos para que se vea como una distribución continua.

## 13. Validaciones y pruebas

`validarImagen` y la validación de `HistogramAnalyzer` rechazan referencias nulas, imágenes con error o imágenes sin lector. También se verifican los rangos del slice y del ajuste de brillo.

Las pruebas comprueban, entre otros puntos, que:

- el histograma tenga 256 posiciones y cuente todos los píxeles;
- la escala de grises cree otra imagen y produzca canales iguales;
- el slice clasifique e invierta correctamente;
- la ecualización extienda dos niveles hasta negro y blanco;
- la ecualización conserve tono y saturación;
- el ajuste de brillo sume correctamente y respete los límites.

## 14. Coste computacional

Todas las operaciones recorren la imagen completa. Para una imagen de `ancho * alto = N` píxeles, el coste principal es `O(N)`. El histograma y el acumulado ocupan arreglos de tamaño fijo 256, por lo que su memoria adicional es constante. La imagen de resultado sí necesita memoria proporcional a `N`.

## 15. Resumen de términos

| Término | Significado en el proyecto |
|---|---|
| Píxel | Elemento de la imagen ubicado en `(x, y)` |
| RGB | Canales rojo, verde y azul entre 0 y 1 |
| Intensidad | Valor único entre 0 y 255 que representa claridad |
| Luminancia perceptual | Combinación ponderada `0.299R + 0.587G + 0.114B` |
| Brillo HSB | `max(R,G,B)`, obtenido con `getBrightness()` |
| Histograma | Cantidad de píxeles que pertenece a cada nivel |
| Histograma acumulado | Cantidad de píxeles con nivel menor o igual al índice |
| Ecualización | Transformación de niveles basada en la distribución acumulada |
| Slice | Clasificación binaria de píxeles dentro/fuera de un rango |
| `PixelReader` | Objeto que permite leer los píxeles de una imagen |
| `Color` | Objeto que representa los componentes del color de un píxel |
| `PixelWriter` | Objeto que permite escribir píxeles en una `WritableImage` |

## 16. Idea central para explicar el proyecto oralmente

El programa lee cada píxel, lo traduce a una medida numérica de claridad y cuenta cuántos píxeles hay en cada nivel. Esa cuenta forma el histograma. Para ecualizar, acumula las frecuencias y usa la posición porcentual de cada brillo dentro de la distribución para asignarle un brillo nuevo. Los niveles ocupan así una parte mayor del intervalo disponible. Finalmente reconstruye cada píxel conservando su tono y saturación, pero usando el brillo redistribuido.
