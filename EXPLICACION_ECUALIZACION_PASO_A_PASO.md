# Ecualización del histograma: flujo completo paso a paso

## 1. ¿Qué intenta conseguir `ecualizar`?

Una imagen oscura suele concentrar una gran cantidad de píxeles en intensidades bajas, es decir, en la zona izquierda del histograma. Eso significa que diferentes detalles de la escena están representados mediante valores muy parecidos. Por ejemplo, una sombra puede tener brillo 12 y otra 15: matemáticamente son diferentes, pero visualmente cuesta distinguirlas.

La ecualización analiza cuántos píxeles hay en cada nivel de brillo y construye una regla para asignarles nuevos brillos. Los valores muy concentrados se separan y pasan a ocupar una parte mayor del intervalo disponible.

La idea puede resumirse así:

```text
Imagen original
      ↓
Contar los brillos: histograma
      ↓
Sumar progresivamente las frecuencias: histograma acumulado
      ↓
Construir una función de transformación
      ↓
Cambiar el brillo de cada píxel
      ↓
Imagen ecualizada con mayor contraste global
```

La ecualización no inventa detalles que no existían. Hace más visibles diferencias de brillo que ya estaban almacenadas en la imagen.

## 2. Diferencia importante en este proyecto

El método `ecualizar` calcula su transformación con el brillo del modelo HSB:

```java
color.getBrightness()
```

En JavaFX, este brillo equivale aproximadamente a:

```text
brillo HSB = máximo(R, G, B)
```

En cambio, `HistogramAnalyzer`, que dibuja los gráficos de la interfaz, calcula intensidad mediante:

```text
0.299R + 0.587G + 0.114B
```

Esta es una medida de luminancia perceptual. Por eso el histograma que se ve en pantalla permite observar el efecto luminoso de la ecualización, pero no es exactamente el mismo histograma interno de brillo HSB con el que se calculó la transformación.

## 3. Ejemplo que se utilizará

Para entender el código, se usará una imagen imaginaria de 4 × 2 píxeles. Tiene 8 píxeles en total y solo tres niveles de brillo:

```text
50   50   50   100
100  150  150  150
```

Estos valores se encuentran en la escala de 0 a 255. Su histograma reducido es:

| Brillo | Cantidad de píxeles |
|---:|---:|
| 50 | 3 |
| 100 | 2 |
| 150 | 3 |

Las demás posiciones del histograma contienen cero.

El intervalo utilizado originalmente es solamente 50–150. El objetivo será extenderlo hasta aproximadamente 0–255.

## 4. Paso 1: validar la imagen

```java
validarImagen(imagen);
```

El método verifica que:

- la referencia no sea `null`;
- JavaFX no haya detectado un error al cargarla;
- exista un `PixelReader` para leer los píxeles.

Si la imagen no es válida, se lanza una excepción. Esto evita continuar con datos inexistentes.

## 5. Paso 2: obtener dimensiones y total de píxeles

```java
int ancho = (int) imagen.getWidth();
int alto = (int) imagen.getHeight();
int totalPixeles = ancho * alto;
```

Para el ejemplo:

```text
ancho = 4
alto = 2
totalPixeles = 4 × 2 = 8
```

El total será necesario para convertir cantidades acumuladas en proporciones.

## 6. Paso 3: calcular el histograma de brillo

```java
int[] histograma = calcularHistogramaBrillo(imagen);
```

El método auxiliar crea un arreglo de 256 contadores. Luego recorre todos los píxeles:

```java
int brillo = (int) Math.round(
        lector.getColor(x, y).getBrightness() * 255);
histograma[brillo]++;
```

`getBrightness()` devuelve un número entre 0 y 1. Se multiplica por 255 para convertirlo en un índice entero entre 0 y 255.

Para el ejemplo, las únicas posiciones no vacías son:

```text
histograma[50]  = 3
histograma[100] = 2
histograma[150] = 3
```

El histograma responde la pregunta: **¿cuántos píxeles poseen exactamente cada brillo?**

## 7. Paso 4: construir el histograma acumulado

```java
int[] acumulado = new int[256];
acumulado[0] = histograma[0];
for (int i = 1; i < acumulado.length; i++) {
    acumulado[i] = acumulado[i - 1] + histograma[i];
}
```

Cada posición del acumulado contiene la cantidad de píxeles cuyo brillo es menor o igual que ese índice:

```text
acumulado[i] = histograma[0] + histograma[1] + ... + histograma[i]
```

Para el ejemplo:

| Nivel consultado | Operación conceptual | Acumulado |
|---:|---|---:|
| 49 | No se encontró ningún píxel todavía | 0 |
| 50 | Aparecen los 3 píxeles de brillo 50 | 3 |
| 99 | No aparecen nuevos píxeles | 3 |
| 100 | Se suman 2 píxeles | 5 |
| 149 | No aparecen nuevos píxeles | 5 |
| 150 | Se suman los últimos 3 píxeles | 8 |
| 255 | Ya se contaron todos | 8 |

Una forma de verlo es:

```text
Histograma normal:    ¿cuántos tienen exactamente este brillo?
Histograma acumulado: ¿cuántos tienen este brillo o uno menor?
```

El acumulado también indica la posición relativa de un nivel dentro de toda la población de píxeles.

## 8. Paso 5: encontrar el primer nivel utilizado

```java
int primerNivelUsado = 0;
while (primerNivelUsado < 256 && histograma[primerNivelUsado] == 0) {
    primerNivelUsado++;
}
```

El ciclo comienza en cero y avanza mientras no haya píxeles en ese nivel.

En el ejemplo:

```text
histograma[0]  = 0  → continuar
histograma[1]  = 0  → continuar
...
histograma[49] = 0  → continuar
histograma[50] = 3  → detenerse
```

Por lo tanto:

```text
primerNivelUsado = 50
```

Este paso no busca el píxel más oscuro por su posición `(x, y)`. Busca el índice de brillo más bajo que tenga por lo menos un píxel.

## 9. Paso 6: detectar una imagen uniforme

```java
if (primerNivelUsado == 256 ||
        totalPixeles == acumulado[primerNivelUsado]) {
    return copiarImagen(imagen);
}
```

Hay dos casos protegidos:

1. `primerNivelUsado == 256`: no se encontró ningún nivel utilizado;
2. `totalPixeles == acumulado[primerNivelUsado]`: todos los píxeles pertenecen al primer nivel encontrado.

El segundo caso representa una imagen de un solo brillo. Por ejemplo:

```text
80 80
80 80
```

No existen diferencias de brillo que puedan separarse. Además, la fórmula posterior terminaría dividiendo para cero. El programa devuelve una copia sin cambios.

En el ejemplo principal no ocurre esto:

```text
totalPixeles = 8
acumulado[50] = 3
8 != 3
```

## 10. Paso 7: preparar la imagen de salida

```java
PixelReader lector = imagen.getPixelReader();
WritableImage resultado = new WritableImage(ancho, alto);
PixelWriter escritor = resultado.getPixelWriter();
int acumuladoMinimo = acumulado[primerNivelUsado];
```

- `lector` obtiene cada color original;
- `resultado` es una imagen nueva y vacía;
- `escritor` coloca los colores procesados;
- `acumuladoMinimo` es el acumulado del nivel más bajo utilizado.

En el ejemplo:

```text
primerNivelUsado = 50
acumuladoMinimo = acumulado[50] = 3
```

La resta de este mínimo en la fórmula permite que el nivel más oscuro presente pase exactamente a cero.

## 11. Paso 8: recorrer cada píxel

```java
for (int y = 0; y < alto; y++) {
    for (int x = 0; x < ancho; x++) {
        Color colorOriginal = lector.getColor(x, y);
        int brilloAnterior = (int) Math.round(
                colorOriginal.getBrightness() * 255);
```

El algoritmo vuelve a visitar toda la imagen. La primera visita sirvió para construir el histograma; esta segunda visita sirve para transformar y escribir cada píxel.

Dos píxeles con el mismo brillo anterior consultan la misma posición del acumulado y reciben el mismo brillo nuevo. No importa dónde se encuentren en la imagen.

## 12. Paso 9: calcular el brillo nuevo

```java
double brilloNuevo =
        (double) (acumulado[brilloAnterior] - acumuladoMinimo)
        / (totalPixeles - acumuladoMinimo);
```

La fórmula general es:

```text
                acumulado[brillo] - acumuladoMínimo
brilloNuevo =  ────────────────────────────────────
                   totalPíxeles - acumuladoMínimo
```

El cast `(double)` es esencial para realizar una división decimal. Sin él, Java haría división entera en casos como `2 / 5` y produciría cero.

### ¿Qué hace el numerador?

```text
acumulado[brilloAnterior] - acumuladoMinimo
```

Mide cuánto ha avanzado ese nivel desde el primer grupo de píxeles.

### ¿Qué hace el denominador?

```text
totalPixeles - acumuladoMinimo
```

Representa todo el recorrido disponible desde el primer grupo hasta el total.

### Resultado para brillo 50

```text
acumulado[50] = 3

brilloNuevo = (3 - 3) / (8 - 3)
             = 0 / 5
             = 0.0
```

Convertido a la escala del histograma:

```text
0.0 × 255 = 0
```

Por eso `50 → 0`.

### Resultado para brillo 100

```text
acumulado[100] = 5

brilloNuevo = (5 - 3) / (8 - 3)
             = 2 / 5
             = 0.4
```

Convertido a 0–255:

```text
0.4 × 255 = 102
```

Por eso `100 → 102`.

### Resultado para brillo 150

```text
acumulado[150] = 8

brilloNuevo = (8 - 3) / (8 - 3)
             = 5 / 5
             = 1.0
```

Convertido a 0–255:

```text
1.0 × 255 = 255
```

Por eso `150 → 255`.

### Tabla completa de transformación

| Brillo original | Frecuencia | Acumulado | Fórmula normalizada | Brillo nuevo 0–1 | Equivalente 0–255 |
|---:|---:|---:|---|---:|---:|
| 50 | 3 | 3 | `(3-3)/(8-3)` | 0.0 | 0 |
| 100 | 2 | 5 | `(5-3)/(8-3)` | 0.4 | 102 |
| 150 | 3 | 8 | `(8-3)/(8-3)` | 1.0 | 255 |

La matriz de brillos cambia así:

```text
Antes:                 Después:

50   50   50   100     0    0    0    102
100  150  150  150     102  255  255  255
```

El programa no multiplica explícitamente `brilloNuevo` por 255 al crear el color, porque `Color.hsb` necesita el brillo entre `0.0` y `1.0`. La multiplicación por 255 solo sirve para entender dónde aparecerá luego en un histograma de 0 a 255.

## 13. Paso 10: limitar el resultado

```java
brilloNuevo = limitarValorColor(brilloNuevo);
```

El método aplica:

```java
Math.max(0, Math.min(1, valor))
```

La fórmula debería generar valores válidos, pero este límite protege contra posibles pequeñas desviaciones numéricas. Todo valor menor que cero queda en cero y todo valor mayor que uno queda en uno.

## 14. Paso 11: reconstruir el color

```java
Color colorEcualizado = Color.hsb(
        colorOriginal.getHue(),
        colorOriginal.getSaturation(),
        brilloNuevo,
        colorOriginal.getOpacity()
);
```

El color se representa temporalmente en el modelo HSB:

- `Hue`: tono, por ejemplo rojo, amarillo o azul;
- `Saturation`: intensidad o pureza del color;
- `Brightness`: brillo;
- `Opacity`: transparencia.

El método conserva tono, saturación y opacidad, y reemplaza solamente el brillo. Por ejemplo, un azul oscuro puede convertirse en un azul más claro, pero sigue intentando ser azul.

Esto explica por qué la aplicación puede ecualizar una imagen a color sin convertirla en una imagen gris.

## 15. Paso 12: escribir y devolver la imagen

```java
escritor.setColor(x, y, colorEcualizado);
```

Cada nuevo color se guarda en la misma coordenada que ocupaba el original. Al terminar los dos ciclos:

```java
return resultado;
```

Se devuelve una imagen nueva. La imagen recibida como parámetro no fue modificada.

## 16. ¿Por qué mejora visualmente una imagen oscura?

Supóngase que casi todos los brillos originales están entre 0 y 40. Dos objetos pueden estar representados mediante niveles 10 y 14, una diferencia pequeña. La función acumulada puede enviar esos grupos, por ejemplo, a 30 y 90. La diferencia se hace mayor y los bordes o texturas se vuelven más visibles.

La transformación depende de la frecuencia:

- donde se acumulan muchos píxeles, la CDF crece rápidamente;
- ese crecimiento produce separaciones mayores entre ciertos niveles de salida;
- las zonas tonales muy pobladas ganan contraste;
- otras zonas pueden comprimirse.

Por eso la ecualización es adaptativa: no aplica la misma regla fija a todas las fotografías. Calcula una regla a partir de cada imagen.

## 17. Análisis de los dos histogramas adjuntos

### Histograma original

En el primer gráfico se observa:

- una concentración extremadamente alta cerca de la intensidad 0;
- la mayor parte de los píxeles aproximadamente entre 0 y 35;
- un pico secundario alrededor de 15–20;
- muy pocos píxeles en intensidades medias y altas;
- una cola pequeña que se extiende hacia la derecha.

Esto indica que la imagen original es predominantemente oscura. La escena utiliza de forma muy desigual el rango 0–255. Muchos píxeles poseen intensidades similares en la zona baja, así que los detalles situados en sombras tienen poco contraste entre sí.

El gran pico cercano a cero también puede representar un fondo negro, bordes oscuros o una región muy grande con poca iluminación. El histograma permite afirmar que existen muchos píxeles oscuros, pero por sí solo no permite saber qué objetos o regiones de la fotografía los producen.

### Histograma procesado

En el segundo gráfico se observa:

- los píxeles están repartidos por una parte mucho mayor del intervalo;
- aparecen picos desde intensidades bajas hasta aproximadamente 100;
- existe una cola apreciable hacia intensidades altas, incluso cerca de 240;
- sigue habiendo picos, especialmente alrededor de 12 y entre 80–90;
- la distribución no queda plana ni perfectamente uniforme.

Esto es normal. La ecualización no puede dividir un grupo de píxeles que tenía exactamente el mismo brillo: todo el grupo recibe el mismo brillo nuevo y forma otro pico. Además, los 256 niveles son discretos y la imagen contiene colores y frecuencias reales que no se pueden repartir de manera perfectamente uniforme.

El pico original enorme parece reducirse y la información queda distribuida en más niveles. Eso concuerda con una mejora del contraste global: diferencias antes comprimidas en la zona oscura pasan a estar más separadas.

### ¿Se puede concluir que mejoró la calidad?

De los histogramas sí se puede concluir que:

1. aumentó el rango de intensidades utilizado;
2. disminuyó la concentración exclusiva en tonos muy oscuros;
3. aumentó el contraste global;
4. probablemente se hicieron más visibles detalles que ya existían en las sombras.

Sin embargo, un histograma no permite afirmar por sí solo que toda la calidad mejoró. Para esa conclusión también hay que mirar la imagen procesada, porque la ecualización puede:

- hacer visible ruido que estaba oculto en las sombras;
- producir zonas demasiado claras;
- generar una apariencia de contraste excesivo;
- cambiar la percepción del color, aunque se conserven tono y saturación HSB;
- separar muy bien unas regiones y comprimir otras.

Por la comparación presentada y por tu observación de que la imagen resultante se ve mejor, la conclusión razonable es: **la ecualización corrigió eficazmente una distribución fuertemente oscura y mejoró el contraste perceptible de esta imagen concreta**. Es más preciso decir que mejoró el contraste y la visibilidad de detalles que decir que siempre mejora automáticamente la calidad.

## 18. Una precisión sobre el primer pico del resultado

En el histograma procesado todavía aparece un pico muy alto en una intensidad baja, aproximadamente 12, en lugar de estar exactamente en cero. Esto no demuestra que el algoritmo haya fallado.

El algoritmo transforma el **brillo HSB** mínimo a `0.0`, mientras el gráfico vuelve a medir los colores resultantes mediante **luminancia ponderada**. Ambas medidas no son iguales. Además, diferentes colores pueden tener un brillo HSB parecido pero luminancias distintas. Por eso el histograma visible después de procesar no tiene que coincidir exactamente con la CDF del histograma interno.

También puede haber regiones negras o casi negras muy numerosas que continúan formando un pico incluso después de la transformación.

## 19. Explicación breve para una exposición

Una forma clara de explicarlo oralmente es:

> Primero cuento cuántos píxeles hay en cada brillo. Luego construyo el histograma acumulado, que me dice cuántos píxeles tienen un brillo menor o igual a cada nivel. Normalizo ese acumulado entre cero y uno para crear una tabla de transformación. El brillo mínimo utilizado pasa a cero, el máximo acumulado pasa a uno y los valores intermedios se redistribuyen según su posición acumulada. Finalmente recorro otra vez la imagen, conservo el tono y la saturación de cada píxel y reemplazo solamente su brillo. Así, una distribución que estaba comprimida en la zona oscura ocupa un intervalo mayor y aumenta el contraste.

## 20. Resumen del ejemplo

```text
Entrada:  50, 50, 50, 100, 100, 150, 150, 150

Histograma:
50 aparece 3 veces
100 aparece 2 veces
150 aparece 3 veces

Acumulado:
50 -> 3
100 -> 5
150 -> 8

Normalización:
50  -> (3-3)/(8-3) = 0.0 -> nivel 0
100 -> (5-3)/(8-3) = 0.4 -> nivel 102
150 -> (8-3)/(8-3) = 1.0 -> nivel 255

Resultado: 0, 0, 0, 102, 102, 255, 255, 255
```

La idea fundamental es que la ecualización no decide el nuevo brillo únicamente por el valor numérico original. Lo decide por la posición acumulada de ese brillo dentro de la distribución completa de la imagen.
