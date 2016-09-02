#include <jni.h>
#include "Utils_JNIWrappers_TextureBlender.h"
#include <stdio.h>
#include <stdlib.h>

typedef char byte;
typedef unsigned char bool;

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

inline int fast_floor(double x);
int fast_floor(double x)
{
    return (int) x - (x < (int) x); // as dgobbi above, needs less than for floor
}

inline int fast_round(double d);
int fast_round(double d)
{
    d += 6755399441055744.0;
    return * ( int * ) &d;
}

inline byte getGrayTiled( grayscaleImage image, int x, int y);
byte getGrayTiled( grayscaleImage image, int x, int y) {
    x = x % image.width;
    if (x < 0)
        x += image.width;

    y = y % image.height;
    if (y < 0)
        y += image.height;
        
    return image.data[y*image.width+x];
}

inline double map(double value, double inMin, double inMax, double outMin, double outMax);
double map(double value, double inMin, double inMax, double outMin, double outMax) {
    return outMin + (value - inMin)*(outMax - outMin)/(inMax - inMin);
}


inline byte bilinearInterpolation(double x, double y, byte col_00, byte col_10, byte col_01, byte col_11);
byte bilinearInterpolation(double x, double y, byte col_00, byte col_10, byte col_01, byte col_11) {
    double area_11 = (x)*(y);
    double area_01 = (1-x)*(y);
    double area_10 = (x)*(1-y);
    double area_00 = (1-x)*(1-y);

    int d_g = fast_round( ((double)col_00*area_00+(double)col_10*area_10+(double)col_01*area_01+(double)col_11*area_11) );
	if (d_g < -128) d_g = -128;
	else if (d_g > 127) d_g = 127;
    return (byte)d_g;
}
    
inline byte interpolateTiled(Envelope image_envelope, grayscaleImage image, double x, double y);
byte interpolateTiled(Envelope image_envelope, grayscaleImage image, double x, double y) {
    double image_x = map(x,image_envelope.min_x,image_envelope.max_x,0,image.width);
    double image_y = map(y,image_envelope.min_y,image_envelope.max_y,0,image.height);

    int min_x = fast_floor(image_x);
    int min_y = fast_floor(image_y);
    int max_y = min_y+1;
    int max_x = min_x+1;

    byte col_00;
    byte col_10;
    byte col_01;
    byte col_11;

	col_00 = getGrayTiled(image,min_x,min_y);
    col_10 = getGrayTiled(image,max_x,min_y);
    col_01 = getGrayTiled(image,min_x,max_y);
    col_11 = getGrayTiled(image,max_x,max_y);

	return bilinearInterpolation(image_x-min_x,image_y-min_y,col_00,col_10,col_01,col_11);
}
    
#define third  0.3333333
#define third2 0.6666666
void overlay(grayscaleImage image, grayscaleImage tex, Envelope envelope) {
	
	double x;
	double y;
	int row;
	int column;
	double gray_accum = 0;
    int count = 0;
    int gray;

    for (column = 0; column != image.width; ++column) {
        for (row = 0; row != image.height; ++row) {

            gray_accum = 0;


			gray_accum += interpolateTiled(envelope,tex,	column+third,	row+third);
			gray_accum += interpolateTiled(envelope,tex,	column+third,	row+third2);
			gray_accum += interpolateTiled(envelope,tex,	column+third2,	row+third);
			gray_accum += interpolateTiled(envelope,tex,	column+third2,	row+third2);

            gray_accum /= 4;

			gray = (int)(gray_accum+0.5);
			if (gray < -128) gray = -128;
			else if (gray > 127) gray = 127;

            image.data[row*image.width+column] = (byte)gray;
        }
    }
		
}

JNIEXPORT void JNICALL Java_Utils_JNIWrappers_TextureBlender_drawOverTiled
  (JNIEnv* env, jclass cls, 
  jbyteArray image_arr	, jint image_width	, jint image_height, 
  jbyteArray tex_arr	, jint tex_width	, jint tex_height, 
  jdouble env_minX, jdouble env_maxX, jdouble env_minY, jdouble env_maxY) {

	//printf("OVERLAYING!\n");
	grayscaleImage image;
	grayscaleImage tex;
	Envelope envelope;

	image.data 		= (byte*)((*env)->GetByteArrayElements(env, image_arr, 0));
	tex.data 		= (byte*)((*env)->GetByteArrayElements(env, tex_arr	, 0));
	

	image.width = image_width;
	image.height = image_height;
	
	tex.width = tex_width;
	tex.height = tex_height;
	
	envelope.max_x = env_maxX;
	envelope.max_y = env_maxY;
	envelope.min_x = env_minX;
	envelope.min_y = env_minY;

	//printf("OVERLAYING!");
	overlay(image,tex,envelope);
	
	(*env)->ReleaseByteArrayElements(env, image_arr, (jbyte*)image.data, 0);
	(*env)->ReleaseByteArrayElements(env, tex_arr, 	(jbyte*)tex.data, 0);
	//printf("SUCCESS!\n");
};

