package Utils.JNIWrappers;

import Utils.RasterUtils;
import Utils.TextureUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Artyom.Fomenko on 02.09.2016.
 */
public class TextureBlenderTest {
    @Test
    public void drawOverTiled() throws Exception {

        int image_width = 1024;
        int image_height = 1024;
        byte[] image = new byte[image_height*image_width];
        for (int i = 0; i != image.length; ++i) image[i] = 0;

        int tex_width = 50;
        int tex_height = 50;
        byte[] tex = new byte[tex_width*tex_height];
        for (int i = 0; i != tex.length; ++i) tex[i] = -127;

        for (int row = 0; row < tex_height; row+=5) {
            for (int column = 0; column < tex_width; ++column) {
                tex[row*tex_width+column] = (byte)127;
            }
        }

        RasterUtils.save(image,image_width,image_height,"before.png");
        RasterUtils.save(tex,tex_width,tex_height,"test_tex.png");

//        TextureUtils.drawOver(image,image_width,image_height,tex,tex_width,tex_height,
//                new Envelope(
//                        new Coordinate(0,0),
//                        new Coordinate(50,50)),true);

        System.out.println("Begin!");
        TextureBlender.drawOverTiled(image,image_width,image_height,tex,tex_width,tex_height,0,200,0,200);
        System.out.println("End!");
        RasterUtils.save(image,image_width,image_height,"after.png");

        //TextureBlender.drawOverTiled();

    }

}