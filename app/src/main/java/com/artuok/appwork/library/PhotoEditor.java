package com.artuok.appwork.library;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;

public class PhotoEditor {

    public PhotoEditor(){

    }

    public static Bitmap applyBlur(Bitmap bitmap, int radius) {
        // Crea una copia mutable del Bitmap original
        Bitmap blurredBitmap = bitmap.copy(bitmap.getConfig(), true);

        int width = blurredBitmap.getWidth();
        int height = blurredBitmap.getHeight();
        int[] pixels = new int[width * height];

        // Obtiene los píxeles del bitmap en un arreglo
        blurredBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int iterations = 3; // Cantidad de iteraciones del desenfoque (puedes ajustar este valor según tu preferencia)

        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Obtiene el color de cada píxel
                    int pixel = pixels[y * width + x];

                    // Calcula la suma de los colores RGB de los píxeles vecinos
                    int sumRed = 0;
                    int sumGreen = 0;
                    int sumBlue = 0;

                    for (int i = -radius; i <= radius; i++) {
                        for (int j = -radius; j <= radius; j++) {
                            int xCoord = x + i;
                            int yCoord = y + j;

                            // Asegura que las coordenadas estén dentro de los límites del bitmap
                            if (xCoord >= 0 && xCoord < width && yCoord >= 0 && yCoord < height) {
                                int neighborPixel = pixels[yCoord * width + xCoord];

                                sumRed += Color.red(neighborPixel);
                                sumGreen += Color.green(neighborPixel);
                                sumBlue += Color.blue(neighborPixel);
                            }
                        }
                    }

                    // Calcula el color promedio de los píxeles vecinos y lo establece como nuevo color del píxel actual
                    int count = (2 * radius + 1) * (2 * radius + 1);
                    int avgRed = sumRed / count;
                    int avgGreen = sumGreen / count;
                    int avgBlue = sumBlue / count;

                    int newPixel = Color.rgb(avgRed, avgGreen, avgBlue);
                    pixels[y * width + x] = newPixel;
                }
            }
        }

        // Establece los píxeles modificados en la imagen resultante
        blurredBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return blurredBitmap;
    }

    public static Bitmap resizeImage(Bitmap bitmap, int targetSize) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int targetWidth, targetHeight;

        // Calcula el tamaño de destino basado en el lado más largo de la imagen
        if (originalWidth > originalHeight) {
            float aspectRatio = (float) originalWidth / originalHeight;
            targetWidth = targetSize;
            targetHeight = Math.round(targetSize / aspectRatio);
        } else {
            float aspectRatio = (float) originalHeight / originalWidth;
            targetHeight = targetSize;
            targetWidth = Math.round(targetSize / aspectRatio);
        }

        // Crea una matriz para redimensionar la imagen
        Matrix matrix = new Matrix();
        matrix.postScale((float) targetWidth / originalWidth, (float) targetHeight / originalHeight);

        // Redimensiona la imagen con la matriz
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true);

        return resizedBitmap;
    }
}
