package com.example.courseworkgui;

import javafx.application.Application;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingNode;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ProjectionViewer extends Application {

    private double[] discretP_Experimental;
    private int N;
    private int M;
    double dQ;
    private BufferedImage synthesizedImage;
    private Button loadButton;
    private boolean useGaussianFilter = false;
    private double cutoffFrequency;
    private XYSeries seriesOriginal;
    private XYSeries seriesFiltered;
    private XYSeries seriesPrepared;
    private JFreeChart chart;

    private XYSeries seriesFFT;
    private JFreeChart chartFFT;

    @Override
    public void start(Stage primaryStage) {
        // Устанавливаем заголовок окна
        primaryStage.setTitle("Projection Viewer");

        // Создаем объект для выбора файлов
        FileChooser fileChooser = new FileChooser();

        // Создаем текстовое поле для ввода номера проекции
        TextField projectionNumberField = new TextField();

        // Создаем чекбокс для выбора использования Гауссова фильтра
        CheckBox gaussianFilterCheckBox = new CheckBox("Use Gaussian Filter");
        gaussianFilterCheckBox.setOnAction(e -> {
            // Если чекбокс выбран, устанавливаем значение useGaussianFilter в true
            useGaussianFilter = gaussianFilterCheckBox.isSelected();
            // Получаем номер проекции из текстового поля
            int projectionNumber = Integer.parseInt(projectionNumberField.getText());
            try {
                // Пытаемся показать проекцию
                showProjection(projectionNumber);
            } catch (IOException ex) {
                // Если возникла ошибка, выбрасываем исключение
                throw new RuntimeException(ex);
            }
        });

        // Создаем текстовое поле для ввода частоты среза и устанавливаем его значение в "5"
        TextField cutoffFrequencyField = new TextField();
        cutoffFrequencyField.setText("5");

        // Устанавливаем значение cutoffFrequency
        cutoffFrequency = Double.parseDouble(cutoffFrequencyField.getText());

        // Добавляем слушателя к текстовому полю, который будет реагировать на изменение текста
        cutoffFrequencyField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                // Пытаемся преобразовать новое значение в double
                double value = Double.parseDouble(newValue);
                if (value < 0) {
                    // Если значение отрицательное, показываем уведомление пользователю
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка ввода");
                    alert.setHeaderText(null);
                    alert.setContentText("Значение не может быть отрицательным");
                    alert.showAndWait();
                } else {
                    // Если значение не отрицательное, устанавливаем его как новую частоту среза
                    cutoffFrequency = value;
                    // Получаем номер проекции из текстового поля
                    int projectionNumber = Integer.parseInt(projectionNumberField.getText());
                    try {
                        // Пытаемся показать проекцию
                        showProjection(projectionNumber);
                    } catch (IOException e) {
                        // Если возникла ошибка, выбрасываем исключение
                        throw new RuntimeException(e);
                    }
                }
            } catch (NumberFormatException e) {
                // Если новое значение не является числом, показываем уведомление пользователю
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка ввода");
                alert.setHeaderText(null);
                alert.setContentText("Введите числовое значение");
                alert.showAndWait();
            }
        });

        // Создаем кнопку для загрузки проекций
        loadButton = new Button("Load Projections");
        loadButton.setOnAction(e -> {
            // Показываем диалог выбора файла
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                // Если файл выбран, загружаем проекции
                loadProjections(file);
            }
        });

        // Создаем кнопку для показа проекции
        Button showButton = new Button("Show Projection");
        showButton.setOnAction(e -> {
            // Получаем номер проекции из текстового поля
            int projectionNumber = Integer.parseInt(projectionNumberField.getText());
            try {
                // Пытаемся показать проекцию
                showProjection(projectionNumber);
            } catch (IOException ex) {
                // Если возникла ошибка, выбрасываем исключение
                throw new RuntimeException(ex);
            }
        });

        // Создаем чекбокс для выбора показа оригинальной проекции
        CheckBox originalCheckBox = new CheckBox("Show Original");
        originalCheckBox.setSelected(true);
        originalCheckBox.setOnAction(e -> {
            // Если чекбокс выбран, показываем оригинальную проекцию
            XYPlot plot = chart.getXYPlot();
            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesVisible(0, originalCheckBox.isSelected());
        });

        // Создаем чекбокс для выбора показа отфильтрованной проекции
        CheckBox filteredCheckBox = new CheckBox("Show Filtered");
        filteredCheckBox.setSelected(true);
        filteredCheckBox.setOnAction(e -> {
            // Если чекбокс выбран, показываем отфильтрованную проекцию
            XYPlot plot = chart.getXYPlot();
            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesVisible(1, filteredCheckBox.isSelected());
        });

        // Создаем чекбокс для выбора показа подготовленной проекции
        CheckBox preparedCheckBox = new CheckBox("Show Prepared");
        preparedCheckBox.setSelected(true);
        preparedCheckBox.setOnAction(e -> {
            // Если чекбокс выбран, показываем подготовленную проекцию
            XYPlot plot = chart.getXYPlot();
            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesVisible(2, preparedCheckBox.isSelected());

            adjustXAxisRange();
        });

        // Создаем панель управления и добавляем на нее все элементы
        GridPane controlPanel = new GridPane();
        controlPanel.add(loadButton, 0, 0);
        controlPanel.add(projectionNumberField, 1, 0);
        controlPanel.add(showButton, 2, 0);
        controlPanel.add(gaussianFilterCheckBox, 0, 1);
        controlPanel.add(new Label("Cutoff Frequency:"), 0, 2);
        controlPanel.add(cutoffFrequencyField, 1, 2);
        controlPanel.add(originalCheckBox, 0, 3);
        controlPanel.add(filteredCheckBox, 1, 3);
        controlPanel.add(preparedCheckBox, 2, 3);
        controlPanel.setPadding(new javafx.geometry.Insets(5, 0, 0, 5));
        controlPanel.setHgap(5); // Устанавливаем горизонтальный отступ
        controlPanel.setVgap(5); // Устанавливаем вертикальный отступ

        // Создаем корневой элемент и добавляем на него панель управления
        BorderPane root = new BorderPane();
        root.setTop(controlPanel);

        // Создаем сцену и добавляем на нее корневой элемент
        Scene scene = new Scene(root, 400, 200);

        // Устанавливаем сцену для окна
        primaryStage.setScene(scene);
        // Устанавливаем минимальные размеры окна
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(860);
        // Показываем окно
        primaryStage.show();
    }


    public void drawSinogram(int numRows, int numEls, double[] projections, String filename) {
        // Устанавливаем размер пикселя
        int pixelSize = 1;

        // Создаем изображение с заданными размерами и типом
        BufferedImage image = new BufferedImage(numEls * pixelSize, numRows * pixelSize, BufferedImage.TYPE_INT_RGB);

        // Проходим по каждой строке синограммы
        for (int i = 0; i < numRows; i++) {
            // Проходим по каждому пикселю в строке
            for (int j = 0; j < numEls; j++) {
                // Получаем значение пикселя из массива проекций
                int pixelValue = (int) projections[i * numEls + j];
                // Создаем цвет на основе полученного значения пикселя
                Color color = new Color(pixelValue, pixelValue, pixelValue);

                // Рисуем пиксель на изображении
                for (int dy = 0; dy < pixelSize; dy++) {
                    for (int dx = 0; dx < pixelSize; dx++) {
                        image.setRGB(j * pixelSize + dx, i * pixelSize + dy, color.getRGB());
                    }
                }
            }
        }

        // Пытаемся сохранить изображение в файл
        try {
            ImageIO.write(image, "png", new File(filename));
        } catch (IOException e) {
            // В случае ошибки выводим стектрейс
            e.printStackTrace();
        }
    }


//    public void drawSinogram(int numRows, int numEls, double[] projections, String filename) {
//        int pixelSizeWidth = 200 / numEls; // масштабирование по ширине
//        int pixelSizeHeight = 200 / numRows; // масштабирование по высоте
//
//        // Создаем изображение
//        BufferedImage image = new BufferedImage(numEls * pixelSizeWidth, numRows * pixelSizeHeight, BufferedImage.TYPE_INT_RGB);
//
//        // Отрисовываем каждую строку синограммы
//        for (int i = 0; i < numRows; i++) {
//            // Отрисовываем каждый пиксель в строке
//            for (int j = 0; j < numEls; j++) {
//                int pixelValue = (int) projections[i * numEls + j];
//                Color color = new Color(pixelValue, pixelValue, pixelValue);
//
//                // Рисуем пиксель
//                for (int dy = 0; dy < pixelSizeHeight; dy++) {
//                    for (int dx = 0; dx < pixelSizeWidth; dx++) {
//                        image.setRGB(j * pixelSizeWidth + dx, i * pixelSizeHeight + dy, color.getRGB());
//                    }
//                }
//            }
//        }
//
//        // Сохраняем изображение
//        try {
//            ImageIO.write(image, "png", new File(filename));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void loadProjections(File file) {
        // Пытаемся открыть файл для чтения
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            // Читаем первую строку файла
            String line = reader.readLine();
            // Если первая строка не равна "data", то файл не содержит проекций
            if (!line.equals("data")) {
                System.out.println("Файл не содержит проекций");
                // Закрываем файл и выходим из функции
                reader.close();
                return;
            }
            // Читаем следующие три строки файла, которые содержат количество точек в проекции, количество проекций и угловой шаг
            N = Integer.parseInt(reader.readLine()); // Количество точек в проекции
            M = Integer.parseInt(reader.readLine()); // Количество проекций
            dQ = Double.parseDouble(reader.readLine()); // Угловой шаг

            // Создаем массив для хранения экспериментальных проекций
            discretP_Experimental = new double[N * M];
            // Пропускаем строку с углом
            reader.readLine();

            // Читаем все проекции из файла
            for (int i = 0; i < M; i++) {
                // Разделяем строку на отдельные числа
                String[] numbers = reader.readLine().trim().split(" ");
                // Заполняем массив проекций
                for (int j = 0; j < N; j++) {
                    // Если строка не пустая, преобразуем ее в число и добавляем в массив
                    if (!numbers[j].isEmpty()) {
                        discretP_Experimental[j + i * N] = Double.parseDouble(numbers[j]);
                    }
                }
                // Пропускаем строку с углом
                reader.readLine();
            }
            // Закрываем файл
            reader.close();

        } catch (IOException e) {
            // В случае ошибки выводим стектрейс
            e.printStackTrace();
        }
    }


    private void showProjection(int projectionNumber) throws IOException {
        // Создаем массив для хранения оригинальных данных проекции
        double[] projectionDataOriginal = new double[N];
        // Копируем данные из массива discretP_Experimental в projectionDataOriginal
        System.arraycopy(discretP_Experimental, projectionNumber * N, projectionDataOriginal, 0, N);

        // Создаем массив для хранения отфильтрованных данных проекции
        double[] projectionDataFiltered = new double[N];
        // Если выбран Гауссов фильтр, применяем его к оригинальным данным и копируем результат в projectionDataFiltered
        if (useGaussianFilter) {
            System.arraycopy(applyGaussianFilter(projectionDataOriginal, cutoffFrequency), 0, projectionDataFiltered, 0, N);
        } else {
            // Если Гауссов фильтр не выбран, просто копируем оригинальные данные в projectionDataFiltered
            System.arraycopy(projectionDataOriginal, 0, projectionDataFiltered, 0, N);
        }

        // Подготавливаем данные проекции для отображения
        double[] projectionDataPrepared = prepareProjection(projectionDataOriginal);

        // Отображаем FFT (быстрое преобразование Фурье) подготовленных данных
        showFFT(projectionDataPrepared);

        // Создаем серии данных для отображения на графике
        seriesOriginal = new XYSeries("Original");
        seriesFiltered = new XYSeries("Filtered");
        seriesPrepared = new XYSeries("Prepared");
        // Заполняем серии данными
        for (int i = 0; i < N; i++) {
            seriesOriginal.add(i, projectionDataOriginal[i]);
            seriesFiltered.add(i, projectionDataFiltered[i]);
        }
        for (int i = 0; i < projectionDataPrepared.length; i++) {
            seriesPrepared.add(i, projectionDataPrepared[i]);
        }

        // Создаем набор данных для графика и добавляем в него серии
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesOriginal);
        dataset.addSeries(seriesFiltered);
        dataset.addSeries(seriesPrepared);
        // Создаем график
        chart = ChartFactory.createXYLineChart(
                "График проекции",
                "Номер точки",
                "Значение",
                dataset
        );

        // Получаем ссылку на объект XYPlot для управления параметрами графика
        XYPlot plot = chart.getXYPlot();
        // Устанавливаем диапазон оси X
        plot.getDomainAxis().setRange(0, Math.max(seriesOriginal.getMaxX(), Math.max(seriesFiltered.getMaxX(),
                seriesPrepared.getMaxX())));

        // Получаем ссылку на объект XYLineAndShapeRenderer для управления параметрами отображения серий
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        // Устанавливаем толщину линий для каждой серии
        renderer.setSeriesStroke(0, new BasicStroke(2.0f)); // Original
        renderer.setSeriesStroke(1, new BasicStroke(2.0f)); // Filtered
        renderer.setSeriesStroke(2, new BasicStroke(2.0f)); // Prepared

        // Создаем SwingNode для отображения графика в JavaFX приложении
        SwingNode chartNode = new SwingNode();
        chartNode.setContent(new ChartPanel(chart));
        SwingNode chartNodeFFT = new SwingNode();
        chartNodeFFT.setContent(new ChartPanel(chartFFT));

        // Синтезируем изображение из экспериментальных данных
        BufferedImage[] images = synthesizeImage(discretP_Experimental, N, M);

        // Сохраняем изображения в файлы
        File spectrumFile = new File("spectrum.png");
        ImageIO.write(images[0], "png", spectrumFile);
        File restoredFile = new File("restored.png");
        ImageIO.write(images[1], "png", restoredFile);

        // Создаем ImageView для отображения изображений в JavaFX приложении
        ImageView spectrumView = new ImageView(SwingFXUtils.toFXImage(images[0], null));
        spectrumView.setPreserveRatio(true);
        spectrumView.setOnMouseClicked(e -> {
            // При клике на изображение открываем его во внешнем приложении
            try {
                Desktop.getDesktop().open(spectrumFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        ImageView restoredView = new ImageView(SwingFXUtils.toFXImage(images[1], null));
        restoredView.setPreserveRatio(true);
        restoredView.setOnMouseClicked(e -> {
            // При клике на изображение открываем его во внешнем приложении
            try {
                Desktop.getDesktop().open(restoredFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        // Масштабируем массив проекций
        double[] scaledProjections = scaleArrayN(discretP_Experimental);
        // Создаем синограмму
        drawSinogram(M, N, scaledProjections, "sinogram.png");

        // Загружаем синограмму в ImageView
        File sinogramFile = new File("sinogram.png");
        BufferedImage sinogramImage = ImageIO.read(sinogramFile);
        ImageView sinogramView = new ImageView(SwingFXUtils.toFXImage(sinogramImage, null));
        sinogramView.setPreserveRatio(true);
        sinogramView.setOnMouseClicked(e -> {
            // При клике на синограмму открываем ее во внешнем приложении
            try {
                Desktop.getDesktop().open(sinogramFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        // Создаем панели с заголовками для каждого изображения
        TitledPane spectrumPane = new TitledPane("Spectrum Image", spectrumView);
        TitledPane restoredPane = new TitledPane("Restored Image", restoredView);
        TitledPane sinogramPane = new TitledPane("Sinogram Image", sinogramView);

        // Создаем контейнеры для графиков и изображений
        VBox charts = new VBox(chartNode, chartNodeFFT);
        charts.setSpacing(10);
        VBox images2 = new VBox(spectrumPane, restoredPane, sinogramPane);
        images2.setSpacing(10);
        HBox chartsAndImages = new HBox(charts, images2);
        chartsAndImages.setSpacing(10);

        // Устанавливаем отступы
        chartsAndImages.setPadding(new javafx.geometry.Insets(0, 0, 0, 20));

        // Добавляем HBox в центр root
        BorderPane root = (BorderPane) loadButton.getScene().getRoot();
        root.setCenter(chartsAndImages);
    }


    private void adjustXAxisRange() {
        // Получаем объект XYPlot, который представляет собой область отображения данных на графике
        XYPlot plot = chart.getXYPlot();
        // Инициализируем переменные для хранения минимального и максимального значения X
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        // Перебираем все серии данных на графике
        for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
            // Если текущая серия видима на графике
            if (plot.getRenderer().isSeriesVisible(i)) {
                // Получаем объект серии данных
                XYSeries series = ((XYSeriesCollection) plot.getDataset()).getSeries(i);
                // Обновляем минимальное и максимальное значение X
                minX = Math.min(minX, series.getMinX());
                maxX = Math.max(maxX, series.getMaxX());
            }
        }

        // Устанавливаем диапазон оси X на графике
        plot.getDomainAxis().setRange(minX, maxX);
    }

    private void divSize(double[] array, int M, int N) {
        // Перебираем все элементы массива
        for (int i = 0; i < M*N; i++) {
            // Делим каждый элемент массива на произведение M и N
            array[2 * i] /= M*N;
            array[2 * i + 1] /= M*N;
        }
    }

    private void multiplyByMinusOnePowerSum(double[] array, int M, int N) {
        // Перебираем все строки массива
        for (int i = 0; i < M; i++) {
            // Перебираем все столбцы массива
            for (int j = 0; j < N; j++) {
                // Вычисляем индекс элемента в массиве
                int index = i * M + j;
                // Умножаем каждый элемент массива на (-1) в степени суммы индексов i и j
                array[2 * index] *= Math.pow(-1, i + j);
                array[2 * index + 1] *= Math.pow(-1, i + j);
            }
        }
    }

    private double[] getFourierTransformsOfProjections(double[] projections, int buffN, int M) {
        // Вычисляем размер спектра
        int N = buffN * 4;
        // Создаем массив для хранения результатов
        double[] result = new double[N * M * 2];
        // Перебираем все проекции
        for (int k = 0; k < M; k++) {
            // Получаем текущую проекцию
            double[] currentProjection = getProjection(projections, k, buffN);
            // Подготавливаем проекцию для преобразования Фурье
            double[] preparedProjection = prepareProjection(currentProjection);

            // Если выбран Гауссов фильтр, применяем его к подготовленной проекции
            if (useGaussianFilter) {
                preparedProjection = applyGaussianFilter(preparedProjection, cutoffFrequency);
            }
            // Выполняем быстрое преобразование Фурье (FFT)
            List<double[]> fftResult = Fftw1Dimension.calculateFFT(preparedProjection);
            double[] projFFT = fftResult.get(0);
            // Копируем результат FFT в итоговый массив
            System.arraycopy(projFFT, 0, result, k * N * 2, projFFT.length);
        }
        return result;
    }

//    private double[][] calculateMagnitude(double[][] complexArray) {
//        int N = complexArray.length;
//        double[][] magnitude = new double[N][N];
//        for (int i = 0; i < N; i++) {
//            for (int j = 0; j < N; j++) {
//                double real = complexArray[i][2*j];
//                double imag = complexArray[i][2*j+1];
//                magnitude[i][j] = Math.sqrt(real*real + imag*imag);
//            }
//        }
//        return magnitude;
//    }
private double[] calculateMagnitude(double[] complexArray, int M, int N) {
    // Создаем массив для хранения магнитуды
    double[] magnitude = new double[M * N];
    // Перебираем все элементы комплексного массива
    for (int i = 0; i < M * N; i++) {
        // Вычисляем реальную и мнимую части комплексного числа
        double real = complexArray[2 * i];
        double imag = complexArray[2 * i + 1];
        // Вычисляем магнитуду комплексного числа
        magnitude[i] = Math.sqrt(real*real + imag*imag);
        // Применяем логарифмическую шкалу к магнитуде
        magnitude[i] = Math.log(1 + magnitude[i]);
    }
    return magnitude;
}

    public static double[] scaleArrayN(double[] arr) {
        // Инициализируем минимальное и максимальное значения массива
        double min = arr[0];
        double max = arr[0];
        // Перебираем все элементы массива для поиска минимального и максимального значения
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        // Масштабируем значения от 0 до 255
        double[] scaledArray = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            scaledArray[i] = Math.round(((arr[i] - min) / (float) (max - min)) * 255);
        }

        return scaledArray;
    }

    // Метод для синтеза изображения из проекций
    private BufferedImage[] synthesizeImage(double[] projections, int buffN, int M) {
        // Устанавливаем размер изображения
        int N = buffN * 4;
        // Создаем массив для хранения изображений
        BufferedImage[] images = new BufferedImage[2];
        // Создаем массив для хранения синтезированного FFT
        double[] synthesized_fft = new double[(N * N)*2];
        // Инициализируем переменную для хранения угла
        double buffQ = 0;

        // Получаем спектры проекций
        double[] projectionsDft = getFourierTransformsOfProjections(projections, buffN, M);

        // Перебираем все проекции
        for (int p = 0; p < M; p++) {
            // Вычисляем косинус и синус угла
            double cosA = Math.cos(Math.toRadians(buffQ));
            double sinA = Math.sin(Math.toRadians(buffQ));

            // Перебираем все точки в проекции
            for (int s = 0; s < N; s++) {
                // Вычисляем координаты точки в пространстве
                double t = (s - (N / 2));
                double wx = t * cosA;
                double wy = t * sinA;
                int x = (int) Math.round(wx + (N / 2));
                int y = (int) Math.round(wy + (N / 2));

                // Если координаты в пределах изображения, копируем данные в синтезированный FFT
                if (x < N && x > -1 && y < N && y > -1) {
                    synthesized_fft[2 * (x * N + y)] = projectionsDft[2 * ((p * N) + s)];
                    synthesized_fft[2 * (x * N + y) + 1] = projectionsDft[2 * ((p * N) + s) + 1];
                }
            }
            // Увеличиваем угол
            buffQ += dQ;
        }

        // Создаем копию синтезированного FFT для вывода спектра
        double[] buffSythesizedFft = synthesized_fft;
        // Вычисляем магнитуду FFT
        double[] magnitude = calculateMagnitude(buffSythesizedFft, N, N);
        // Масштабируем магнитуду
        double[] scaledMagnitudeSpectrum = scaleArrayN(magnitude);
        // Создаем изображение спектра
        images[0] = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int index = i * N + j;
                int rgb = (int)scaledMagnitudeSpectrum[index];
                int color = (rgb << 16);
                images[0].setRGB(i, j, color);
            }
        }
        // Обрезаем изображение до оригинального размера
        images[0] = cropImage(images[0], buffN);

        // Для вывода восстановленного изображения
        // Умножаем каждый элемент синтезированного FFT на (-1)^(i+j)
        multiplyByMinusOnePowerSum(synthesized_fft, N, N);
        // Выполняем обратное преобразование Фурье
        double[] fftResult = Fftw1Dimension.calculateFFT2D(synthesized_fft, N, N);
        // Делим каждый элемент результата на размер изображения
        divSize(fftResult, N, N);
        // Умножаем каждый элемент результата на (-1)^(i+j)
        multiplyByMinusOnePowerSum(fftResult, N, N);
        // Масштабируем результат
        double[] scaledMagnitudeRestored = scaleArrayN(fftResult);
        // Создаем изображение восстановленного объекта
        images[1] = new BufferedImage(N, N, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int index = i * N + j;
                int rgb = (int)scaledMagnitudeRestored[2*index];
                int color = (rgb << 16);
                images[1].setRGB(i, j, color);
            }
        }
        // Обрезаем изображение до оригинального размера
        images[1] = cropImage(images[1], buffN);

        // Возвращаем массив изображений
        return images;
    }

    // Метод для обрезки изображения до оригинального размера
    private BufferedImage cropImage(BufferedImage image, int originalSize) {
        // Вычисляем размер отступа
        int padding = (image.getWidth() - originalSize) / 2;
        // Возвращаем подизображение
        return image.getSubimage(padding, padding, originalSize, originalSize);
    }

    // Метод для получения проекции из массива проекций
    private double[] getProjection(double[] projections, int p, int N) {
        // Создаем массив для хранения проекции
        double[] projection = new double[N];
        // Копируем проекцию из массива проекций
        System.arraycopy(projections, p*N, projection, 0, N);
        // Возвращаем проекцию
        return projection;
    }

    // Метод для подготовки данных проекции
    private double[] prepareProjection(double[] projectionData) {
        // 1. Дополняем данные нулями
        double[] paddedData = new double[4 * N];
        int padding = (paddedData.length) / 2 - N/2;
        System.arraycopy(projectionData, 0, paddedData, padding, N);

        // 2. Применяем фильтр
        double[] filteredData;
        if (useGaussianFilter) {
            filteredData = applyGaussianFilter(paddedData, cutoffFrequency);
        } else {
            filteredData = paddedData;
        }

        // 3. Делаем циклическое смещение
        double[] shiftedData = new double[paddedData.length];
        int shift = paddedData.length / 2;
        for (int i = 0; i < paddedData.length; i++) {
            shiftedData[i] = filteredData[(i + shift) % paddedData.length];
        }

        // Возвращаем подготовленные данные
        return shiftedData;
    }

    // Метод для отображения FFT (быстрого преобразования Фурье)
    private void showFFT(double[] data) {
        // Применяем FFT к данным
        List<double[]> fftResults = Fftw1Dimension.calculateFFT(data);
        double[] fftSpectrum = fftResults.get(1); // Получаем спектр

        // Создаем серию для FFT
        seriesFFT = new XYSeries("FFT");
        for (int i = 0; i < fftSpectrum.length; i++) {
            seriesFFT.add(i, fftSpectrum[i]);
        }

        // Создаем график для FFT
        XYSeriesCollection datasetFFT = new XYSeriesCollection();
        datasetFFT.addSeries(seriesFFT);
        chartFFT = ChartFactory.createXYLineChart(
                "FFT Spectrum",
                "Frequency",
                "Magnitude",
                datasetFFT
        );
    }

    // Метод для применения Гауссова фильтра к данным
    private double[] applyGaussianFilter(double[] array, double sigma) {
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
        launch(args);
    }
}


