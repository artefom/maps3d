package Utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Artyom.Fomenko on 18.08.2016.
 */
public class RandomForestRegressor {

    private static class Tree {
        int[] children_left;
        int[] children_right;
        double[] threshold;
        int[] feature;
        double[] value;

        public Tree(int[] children_left, int[] children_right, double[] threshold, int[] feature, double[] value) {
            this.children_left = children_left;
            this.children_right = children_right;
            this.threshold = threshold;
            this.feature = feature;
            this.value = value;
        }

        public double predict(double[] sample) {
            int node_id = 0;

            while (feature[node_id] != -2) {
                if (sample[feature[node_id]] > threshold[node_id])
                    node_id = children_right[node_id];
                else
                    node_id = children_left[node_id];
            }

            return value[node_id];
        }
    }

    public RandomForestRegressor() {

    }

    private static int[] readIntArray(String s){
        String[] values = s.split(" ");
        int[] arr = new int[values.length];
        for (int i = 0; i != arr.length; ++i) arr[i] = Integer.parseInt(values[i]);
        return arr;
    }

    private static double[] readDoubleArray(String s){
        String[] values = s.split(" ");
        double[] arr = new double[values.length];
        for (int i = 0; i != arr.length; ++i) arr[i] = Double.parseDouble(values[i]);
        return arr;
    }

    private Tree[] trees;

    public void loadModelFromFile(InputStream f) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(f));

        int features_num = Integer.parseInt(br.readLine());
        int forest_size = Integer.parseInt(br.readLine());

        trees = new Tree[forest_size];

        for (int tree_id = 0; tree_id != forest_size; ++tree_id) {
            int node_count = Integer.parseInt(br.readLine());
            int[] children_left = readIntArray(br.readLine());
            int[] children_right = readIntArray(br.readLine());
            double[] threshold = readDoubleArray(br.readLine());
            int[] feature = readIntArray(br.readLine());
            double[] value = readDoubleArray(br.readLine());
            trees[tree_id] = new Tree(children_left,children_right,threshold,feature,value);
        }
    }

    public double predict(double[] sample) {
        double summ = 0;
        for (int tree_id = 0; tree_id != trees.length; ++tree_id) {
            summ+=trees[tree_id].predict(sample)/trees.length;
        }
        return summ;
    }

}
