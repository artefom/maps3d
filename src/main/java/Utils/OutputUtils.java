package Utils;

import Algorithm.Interpolation.Triangulation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by fdl on 8/10/16.
 */
public class OutputUtils {
    private static String name = "sample";
    public static void setName(String name) {
        OutputUtils.name = name;
    }

    public static void saveAsOBJ(Triangulation triangulation){
        triangulation.writeToFile(name);
        CommandLineUtils.report(" file dumped");
    }

    public static void saveAsTXT(double [][] heightmap){
        PrintWriter out;
        try {
            out = new PrintWriter(name + ".txt");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + name + ".txt");
        }
        int y_steps = heightmap.length;
        int x_steps = heightmap[0].length;

        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                out.print(heightmap[i][j]+" ");
            }
            out.println();
        }
        out.close();
        CommandLineUtils.report(" file dumped");
    }

    public static void saveAsPNG(double[][] heightmap){
        int y_steps = heightmap.length;
        int x_steps = heightmap[0].length;

        //getting bounds of possible height values
        double minHeight = heightmap[0][0], maxHeight = heightmap[0][0];
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                minHeight = Math.min(minHeight, heightmap[i][j]);
                maxHeight = Math.max(maxHeight, heightmap[i][j]);
            }
        }

        //creating visual heightmap
        BufferedImage image = new BufferedImage(x_steps, y_steps, BufferedImage.TYPE_INT_RGB);
        for (int i = y_steps-1; i >= 0; --i) {
            for (int j = 0; j != x_steps; ++j) {
                int grey = 255-(int)GeomUtils.map(heightmap[i][j], minHeight, maxHeight, 255, 0);
                image.setRGB(j, y_steps-i-1, (((grey << 8) + (int)(grey)) << 8) + (int)(grey));
            }
        }

        //writing it to file
        try {
            File png = new File(name + ".png");
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new RuntimeException("Could not save " + name + ".png");
        }
        CommandLineUtils.report(" file dumped");
    }
}
