package Utils;

import java.util.Random;

/**
 * Created by Artyom.Fomenko on 18.08.2016.
 */
public class SimplexNoise {

    SimplexNoise_octave[] octaves;
    double[] frequencys;
    double[] amplitudes;

    int fractals;
    double persistence;
    double period;
    int seed;
    double fix;
    double min;
    double max;

    public SimplexNoise(double period, double min, double max, int fractals,double persistence, int seed){
        this.min = min;
        this.max = max;
        this.period = period;
        this.fractals=fractals;
        this.persistence=persistence;
        this.seed=seed;

        //recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
        int numberOfOctaves=(int)Math.ceil(Math.log10(fractals)/Math.log10(2.0));

        octaves=new SimplexNoise_octave[numberOfOctaves];
        frequencys=new double[numberOfOctaves];
        amplitudes=new double[numberOfOctaves];

        Random rnd=new Random(seed);

        for(int i=0;i<numberOfOctaves;i++){
            octaves[i]=new SimplexNoise_octave(rnd.nextInt());

            frequencys[i] = Math.pow(2,i);
            amplitudes[i] = Math.pow(persistence,octaves.length-i);
        }

        double x = persistence;
        this.fix = -1.0109207480965927*x*x*x-0.5824584273791961*x*x-0.9889031636615105*x-0.0002791643950629806;
    }


    public double getNoise(double x, double y){

        double result=0;

        for(int i=0;i<octaves.length;i++){
            //double frequency = Math.pow(2,i);
            //double amplitude = Math.pow(persistence,octaves.length-i);

            result=result+octaves[i].noise(x/frequencys[i]/period, y/frequencys[i]/period)* amplitudes[i];
        }


        return min + (max-min)*( result/fix/2+0.5 );

    }
}
