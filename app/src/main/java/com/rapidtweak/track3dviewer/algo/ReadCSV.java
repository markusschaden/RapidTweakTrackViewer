package com.rapidtweak.track3dviewer.algo;

import android.content.Context;
import android.util.Log;

import com.rapidtweak.track3dviewer.R;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ReadCSV {

    private ArrayList<ArrayList<Double>> data;

    public ReadCSV(Context context) throws IOException {
        data = new ArrayList<ArrayList<Double>>();

        Scanner scanner = null;
        InputStreamReader is = new InputStreamReader(context.getResources().openRawResource(R.raw.data));
        scanner = new Scanner(is);
        scanner.useDelimiter(",|\r\n");

        while (scanner.hasNextDouble()) {
            ArrayList<Double> line = new ArrayList<Double>();
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());
            line.add(scanner.nextDouble());

            data.add(line);
        }
    }

    public double[][] getData() {
        double[][] result = new double[data.size()][10];
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                result[i][j] = data.get(i).get(j);
            }
        }
        return result;
    }
}
