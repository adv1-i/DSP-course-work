package com.example.courseworkgui;

import org.bytedeco.javacpp.*;
import static org.bytedeco.fftw.global.fftw3.*;


public class FFT2D {
    // Высота и ширина изображения
    private final int height;
    private final int width;
    // Входной и выходной массивы для FFTW
    private final DoublePointer in;
    private final DoublePointer out;
    // План преобразования Фурье
    private fftw_plan plan;

    // Конструктор класса
    public FFT2D(int height, int width) {
        // Инициализация высоты и ширины
        this.height = height;
        this.width = width;
        // Инициализация входного и выходного массивов
        this.in = new DoublePointer((long) height * width * 2);
        this.out = new DoublePointer((long) height * width * 2);
    }

    // Метод для установки данных изображения
    public void setImageData(double[][] imageData) {
        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Устанавливаем реальную и мнимую части для каждого пикселя
                in.put(((long) i * width + j) * 2, imageData[i][j] * degree(i + j));
                in.put(((long) i * width + j) * 2 + 1, 0);
            }
        }
    }

    // Метод для получения данных изображения
    public double[][] getImageData() {
        // Создаем массив для хранения данных изображения
        double[][] imageData = new double[height][width];

        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Получаем данные для каждого пикселя
                imageData[i][j] = in.get(((long) i * width + j) * 2) / (height * width)
                        * degree(i + j);
            }
        }

        return imageData;
    }

    // Метод для выполнения прямого преобразования Фурье
    public void forwardTransform() {
        // Создаем план прямого преобразования Фурье
        plan = fftw_plan_dft_2d(
                height, width, in, out, FFTW_FORWARD, (int) FFTW_ESTIMATE);
        // Выполняем прямое преобразование Фурье
        fftw_execute(plan);
    }

    // Метод для получения результата прямого преобразования Фурье
    public TransformData getForwardTransform() {
        // Создаем массив для хранения результата преобразования
        double[][] forwardTransform = new double[height][width];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Получаем реальную и мнимую части для каждого пикселя
                double real = out.get(((long) i * width + j) * 2);
                double imag = out.get(((long) i * width + j) * 2 + 1);
                // Вычисляем амплитуду для каждого пикселя
                forwardTransform[i][j] = Math.sqrt(real * real + imag * imag);
                // Обновляем минимальное и максимальное значения
                if (forwardTransform[i][j] < min) {
                    min = forwardTransform[i][j];
                }
                if (forwardTransform[i][j] > max) {
                    max = forwardTransform[i][j];
                }
            }
        }
        return new TransformData(forwardTransform, min, max);
    }

    // Метод для получения спектра
    public TransformData getSpectrum() {
        // Создаем массив для хранения спектра
        double[][] spectrum = new double[height][width];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Получаем реальную и мнимую части для каждого пикселя
                double real = out.get(((long) i * width + j) * 2);
                double imag = out.get(((long) i * width + j) * 2 + 1);
                // Вычисляем магнитуду для каждого пикселя
                double magnitude = Math.log(Math.sqrt(real * real + imag * imag) + 1);
                spectrum[i][j] = magnitude;
                // Обновляем минимальное и максимальное значения
                if (magnitude < min) {
                    min = magnitude;
                }
                if (magnitude > max) {
                    max = magnitude;
                }
            }
        }
        // Нормализуем спектр
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                spectrum[i][j] = (spectrum[i][j] - min) / (max - min);
            }
        }
        return new TransformData(spectrum, min, max);
    }

    // Метод для получения обратного преобразования Фурье
    public TransformData getInverseTransform() {
        // Создаем план обратного преобразования Фурье
        fftw_plan inversePlan = fftw_plan_dft_2d(
                height, width, out, in, FFTW_BACKWARD, (int) FFTW_ESTIMATE);
        // Выполняем обратное преобразование Фурье
        fftw_execute(inversePlan);
        // Уничтожаем план преобразования Фурье
        fftw_destroy_plan(inversePlan);

        // Создаем массив для хранения результата обратного преобразования
        double[][] inverseTransform = new double[height][width];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Получаем значение для каждого пикселя
                double value = in.get(((long) i * width + j) * 2) / (height * width)
                        * degree(i + j);
                inverseTransform[i][j] = value;
                // Обновляем минимальное и максимальное значения
                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
            }
        }
        // Нормализуем обратное преобразование
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                inverseTransform[i][j] = (inverseTransform[i][j] - min) / (max - min);
            }
        }
        return new TransformData(inverseTransform, min, max);
    }

    // Метод для применения фильтра высоких частот
    public void applyHighPassFilter(double cutoffFrequency) {
        double centerX = (double) width / 2;
        double centerY = (double) height / 2;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double distance = Math.sqrt(Math.pow(i - centerX, 2) + Math.pow(j - centerY, 2));

                if (distance < cutoffFrequency) {
                    out.put(((long) i * width + j) * 2, 0);
                    out.put(((long) i * width + j) * 2 + 1, 0);
                } else {
                    double ratio = (distance - cutoffFrequency) / (Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2)) - cutoffFrequency);
                    double real = out.get(((long) i * width + j) * 2);
                    double imag = out.get(((long) i * width + j) * 2 + 1);
                    out.put(((long) i * width + j) * 2, real * ratio);
                    out.put(((long) i * width + j) * 2 + 1, imag * ratio);
                }
            }
        }
    }




    // Метод для применения фильтра Гаусса низких частот
    public void applyGaussianLowPassFilter(double cutoffFrequency) {
        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Вычисляем расстояние от текущего пикселя до центра изображения
                double distance =
                        Math.sqrt(Math.pow(i - height / 2, 2) + Math.pow(j - width / 2, 2));
                // Вычисляем Гауссову функцию для текущего пикселя
                double gaussian =
                        Math.exp(-Math.pow(distance, 2) / (2 * Math.pow(cutoffFrequency, 2)));
                // Применяем Гауссову функцию к реальной и мнимой частям каждого пикселя
                out.put(((long) i * width + j) * 2,
                        out.get(((long) i * width + j) * 2) * gaussian);
                out.put(((long) i * width + j) * 2 + 1,
                        out.get(((long) i * width + j) * 2 + 1) * gaussian);
            }
        }
    }

    public void cleanup() {
        // Уничтожаем план преобразования Фурье
        fftw_destroy_plan(plan);
        // Очищаем все ресурсы, связанные с FFTW
        fftw_cleanup();
    }

    public void setSpectrum(double[][] newSpectrum) {
        // Проходим по всем пикселям изображения
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Устанавливаем новые значения спектра для каждого пикселя
                out.put(((long) i * width + j) * 2, newSpectrum[i][j]);
                out.put(((long) i * width + j) * 2 + 1, 0);
            }
        }
    }

    private static float degree(int n) {
        // Функция для определения степени мнимой части
        return (1 - 2 * (n % 2));
    }
}
