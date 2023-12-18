package com.example.courseworkgui;

import static org.bytedeco.fftw.global.fftw3.*;
import static java.lang.Math.*;

import org.bytedeco.fftw.global.fftw3;
import org.bytedeco.javacpp.*;

import java.util.ArrayList;
import java.util.List;

public class Fftw1Dimension {

    static final int REAL = 0;
    static final int IMAG = 1;

    private static void multiplyOnImag(DoublePointer data, int length) {
        double[] r = new double[(int)data.capacity()];
        data.get(r);
        for (int i = 0; i < length; i++) {
            r[2 * i + REAL] *= degree(i);
        }
        data.put(r);
    }
    private static List<Double> getSpectrum(DoublePointer result, int length) {
        double[] r = new double[(int)result.capacity()];
        result.get(r);
        List<Double> spectrum = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            spectrum.add(sqrt(pow(r[2 * i + REAL], 2) +
                    pow(r[2 * i + IMAG], 2)));
        }
        return spectrum;
    }

    private static double[] getArrayOut(DoublePointer result) {
        double[] r = new double[(int)result.capacity()];
        result.get(r);
        return r;
    }

    private static List<Double> getListFromDFTArray(DoublePointer result, int length) {
        double[] r = new double[(int)result.capacity()];
        result.get(r);
        List<Double> dft_array = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            dft_array.add(r[2 * i + REAL]);
        }
        return dft_array;
    }

    private static void getScale(DoublePointer result, int length) {
        double[] r = new double[(int)result.capacity()];
        result.get(r);
        for (int i = 0; i < length; i++) {
            r[2 * i + REAL] /= length;
        }
        result.put(r);
    }

    public static List<double[]> calculateFFT(double[] inputSignal) {
        int N = inputSignal.length;

        DoublePointer Array_In = new DoublePointer(2 * N);
        DoublePointer Array_Out = new DoublePointer(2 * N);

        double[] s = new double[(int)Array_In.capacity()];

        for (int i = 0; i < N; i++) {
            s[2 * i + REAL] = inputSignal[i];
            s[2 * i + IMAG] = 0;
        }
        Array_In.put(s);

//        double[] source_signal = new double[N];
//        for (int i = 0; i < N; i++) {
//            source_signal[i] = s[2 * i + REAL];
//        }

        multiplyOnImag(Array_In, N);

        fftw3.fftw_plan plan = fftw3.fftw_plan_dft_1d(N, Array_In, Array_Out, fftw3.FFTW_FORWARD, (int)FFTW_ESTIMATE);
        fftw3.fftw_execute(plan);

        double[] Array_Out_array = getArrayOut(Array_Out);

        List<Double> spectrum = getSpectrum(Array_Out, N);

        double[] spectrum_array = new double[spectrum.size()];
        for (int i = 0; i < spectrum.size(); i++) {
            spectrum_array[i] = spectrum.get(i);
        }

//        plan = fftw3.fftw_plan_dft_1d(N, Array_Out, Array_In, fftw3.FFTW_BACKWARD, (int)FFTW_ESTIMATE);
//        fftw3.fftw_execute(plan);
//
//        getScale(Array_In, N);
//
//        multiplyOnImag(Array_In, N);
//
//        List<Double> dftArray = getListFromDFTArray(Array_In, N);
//
//        double[] dft_array = new double[dftArray.size()];
//        for (int i = 0; i < dftArray.size(); i++) {
//            dft_array[i] = dftArray.get(i);
//        }

        fftw3.fftw_destroy_plan(plan);

        return List.of(Array_Out_array, spectrum_array);
    }

    public static double[] calculateFFT2D(double[] inputSignal, int M, int N) {

        DoublePointer Array_In = new DoublePointer(2 * M * N);
        DoublePointer Array_Out = new DoublePointer(2 * M * N);

        double[] s = new double[(int)Array_In.capacity()];

        for (int i = 0; i < N * M; i++) {
                s[2 * i + REAL] = inputSignal[2*i];
                s[2 * i + IMAG] = inputSignal[2*i+1];
        }
        Array_In.put(s);


        fftw3.fftw_plan plan = fftw3.fftw_plan_dft_2d(M, N, Array_In, Array_Out, fftw3.FFTW_BACKWARD, (int)FFTW_ESTIMATE);
        fftw3.fftw_execute(plan);

//        getScale(Array_In, M * N);

//        multiplyOnImag(Array_In, M * N);

        double[] dftArray = getArrayOut(Array_Out);


        fftw3.fftw_destroy_plan(plan);

        return dftArray;
    }


    public static float degree(int n) {
        return (1 - 2 * (n % 2));
    }
}
