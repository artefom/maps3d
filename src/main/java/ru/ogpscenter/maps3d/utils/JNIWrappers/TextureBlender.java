package ru.ogpscenter.maps3d.utils.JNIWrappers;

public class TextureBlender {

    static {
        System.load("C:\\Users\\Artyom.Fomenko\\maps3d\\src\\main\\cpp\\src\\Utils_JNIWrappers_TextureBlender.dll"); // Load native library at runtime
        // hello.dll (Windows) or libhello.so (Unixes)
    }

    public static native void drawOverTiled(byte[] image,
                                int image_width,
                                int image_height,
                                byte[] tex,
                                int tex_width,
                                int tex_height,
                                double envelope_min_x,
                                double envelope_max_x,
                                double envelope_min_y,
                                double envelope_max_y);


    public static native void drawOver(int[] image, int width, int height,
                                byte[] tex, int tex_width, int tex_height,
                                byte[] mask,
                                double envelope_min_x,
                                double envelope_max_x,
                                double envelope_min_y,
                                double envelope_max_y,
                                boolean tile, int blend_mode);

//
//        // Test Driver
//    public static void main(String[] args) {
//
//        //System.out.println( TextureUtils.bilinearInterpolation(0.5, 0.5, (byte)0, (byte)50, (byte)100, (byte)200) );
//
////        TextureBlender tb = new TextureBlender();
////        tb.sayHello();
////
////        tb.calcSumm( new int[] {1,2,1000});
////
////        overlay(null,null,0,0,0,0);
//    }

}
