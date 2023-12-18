package com.example.courseworkgui;

public class HighPassFilter {
    private double D0;

    public HighPassFilter(double D0) {
        this.D0 = D0;
    }

    public double[] applyFilter(double[] F) {
        int N = F.length;

        double[] H = new double[N];
        int centerU = N / 2;

        for (int u = 0; u < N; u++) {
            int distance = Math.abs(u - centerU);
            H[u] = (distance <= D0) ? 0 : 1;
        }

        double[] result = new double[N];
        for (int u = 0; u < N; u++) {
            result[u] = F[u] * H[u];
        }

        return result;
    }
}

