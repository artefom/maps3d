#include <stdio.h>
#include <stdlib.h>
#include <math.h>

/* run this program using the console pauser or add your own getch, system("pause") or input loop */


typedef char byte;
typedef unsigned char bool;

typedef struct {
	byte a;
	byte r;
	byte g;
	byte b;		
} ARGB;

typedef struct {
	int width;
	int height;	
	
	ARGB* data;
} ARGBImage ;

typedef struct {
	int width;
	int height;
	
	byte* data;	
} grayscaleImage;

typedef struct {
	double min_x;
	double min_y;
	double max_x;
	double max_y;
} Envelope;


byte getGray(byte image[], int image_width, int image_height, int x, int y) {

    x = x % image_width;
    if (x < 0)
        x += image_width;

    y = y % image_height;
    if (y < 0)
        y += image_height;

    return image[y*image_width+x];
}
    
#define max(v1,v2) (v1 > v2 ? v1 : v2)
#define min(v1,v2) (v1 < v2 ? v1 : v2)

int clamp(int val, int from, int to) {
    return max(from, min(to,val));
}
    
double map(double value, double inMin, double inMax, double outMin, double outMax) {
    return outMin + (value - inMin)*(outMax - outMin)/(inMax - inMin);
}

byte bilinearInterpolation(double x, double y, byte col_00, byte col_10, byte col_01, byte col_11) {
    double area_11 = (x)*(y);
    double area_01 = (1-x)*(y);
    double area_10 = (x)*(1-y);
    double area_00 = (1-x)*(1-y);
    
    return col_00;

//    double d_g = (double)col_00*area_00+(double)col_10*area_10+(double)col_01*area_01+(double)col_11*area_11;
//
//    return (byte)clamp(d_g+0.5,-128,127);
}

byte interpolate(Envelope image_envelope, byte image[], int image_width, int image_height, double x, double y) {
    double image_x = map(x,image_envelope.min_x,image_envelope.max_x,0,image_width-1)+0.5;
    double image_y = map(y,image_envelope.min_y,image_envelope.max_y,0,image_height-1)+0.5;

    int min_x = (int)floor(image_x);
    int min_y = (int)floor(image_y);
    int max_y = min_y+1;
    int max_x = min_x+1;

    byte col_00 = getGray(image,image_width,image_height,min_x,min_y);
    byte col_10 = getGray(image,image_width,image_height,max_x,min_y);
    byte col_01 = getGray(image,image_width,image_height,min_x,max_y);
    byte col_11 = getGray(image,image_width,image_height,max_x,max_y);

    return bilinearInterpolation(image_x-min_x,image_y-min_y,col_00,col_10,col_01,col_11);
}

void overlay( byte image[], int image_width, int image_height, byte tex[], int tex_width, int tex_height, Envelope envelope ) {
    for (int column = 0; column != image_width; ++column) {
        for (int row = 0; row != image_height; ++row) {

            double gray_accum = 0;
            int count = 0;

            for (int interp_x = 0; interp_x != 4; ++interp_x) {
                for (int interp_y = 0; interp_y != 4; ++interp_y) {
                    double x = column+0.25*interp_x+0.125;
                    double y = row+0.25*interp_y+0.125;
                    byte c = interpolate(envelope,tex,tex_width,tex_height,x,y);
                    gray_accum += c;
                    count += 1;
                }
            }

            if (count != 0) {
                gray_accum /= count;
            }

            byte gray = (byte)clamp( (int)round(gray_accum), -128, 127);

            image[row*image_width+column] = gray;
        }
    }
}

int main(int argc, char *argv[]) {

	
	int image1_width = 5;
	int image1_height = 5;
	byte image1_data[image1_width*image1_height];
	for (int i = 0; i != image1_width*image1_height; ++i) {
		image1_data[i] = -128;
	}
	
	int image2_width = 2;
	int image2_height = 2;
	byte image2_data[image2_width*image2_height];
	for (int i = 0; i != image2_width*image2_height; ++i) {
		image2_data[i] = -128;
	}
	
	for (int i = 0; i != image2_width; ++i) {
		image2_data[i+image2_width] = 0;
	}
		
	
	grayscaleImage img;
	img.width = image1_width;
	img.height = image1_height;
	img.data = (byte*)(image1_data);


	grayscaleImage img2;
	img2.width = image2_width;
	img2.height = image2_height;
	img2.data = (byte*)(image2_data);
	
	Envelope envelope;
	
	envelope.min_x = 0;
	envelope.min_y = 0;
	envelope.max_x = 2;
	envelope.max_y = 2;
	
	overlay(image1_data,image1_width,image1_height,image2_data,image2_width,image2_height,envelope);
	
	for (int i = 0; i != 5; ++i) {
		for (int j = 0; j != 5; ++j) {	
			printf("%d\t",(int)img.data[i*5+j]);
		}
		printf("\n");
	}
		
	
//	byte result = bilinearInterpolation(0.5, 0.5, 0, 50, 100, 200);
//	printf("result: %d",(int)result);
	return 0;
}
