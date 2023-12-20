package com.example.courseworkgui;

import java.util.Arrays;

public class GaussianFilter {

    public double[] applyGaussianFilter(double[] array, double sigma) {
        int N = array.length;
        int middle = N / 2;

        // Создаем фильтр Гаусса
        double[] filter = new double[N];
        for (int i = 0; i < N; i++) {
            double x = i - middle;
            filter[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
        }

        // Применяем фильтр к исходному массиву
        double[] filteredArray = new double[N];
        for (int i = 0; i < N; i++) {
            double sum = 0;
            for (int j = 0; j < N; j++) {
                int index = (i + j - middle + N) % N;
                sum += array[index] * filter[j];
            }
            filteredArray[i] = sum;
        }

        // Нормализация отфильтрованных данных
        normalizeData(array, filteredArray);

        return filteredArray;
    }

    private void normalizeData(double[] originalData, double[] filteredData) {
        double min = Arrays.stream(filteredData).min().getAsDouble();
        double max = Arrays.stream(filteredData).max().getAsDouble();
        double originalMin = Arrays.stream(originalData).min().getAsDouble();
        double originalMax = Arrays.stream(originalData).max().getAsDouble();

        double scale = (originalMax - originalMin) / (max - min);
        double shift = originalMin - min * scale;

        for (int i = 0; i < filteredData.length; i++) {
            filteredData[i] = filteredData[i] * scale + shift;
        }
    }

    public static void main(String[] args) {
        double[] array = {1,2,3,4,5,6,7,8,9,10};
        double sigma = 0.5; // Задайте значение сигмы

        GaussianFilter gaussianFilter = new GaussianFilter();
        double[] filteredArray = gaussianFilter.applyGaussianFilter(array, sigma);

        // Выводим отфильтрованный массив
        for (double value : filteredArray) {
            System.out.println(value);
        }
    }
}

