package Loader;

import Loader.Binary.OcadHeader;
import org.junit.Test;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Created by Artem on 21.07.2016.
 */
public class ByteDeserializableTest {

//    @Test
//    public void TestDeserialization() throws Exception {
//
//        Path path = Paths.get("C:\\Users\\artefom_w10\\Desktop\\ocad_converter\\sample.ocd");
//        SeekableByteChannel ch = Files.newByteChannel(path); // Defaults to read-only
//        ch.position(0);
//        OcadHeader dser = new OcadHeader();
//        dser.Deserialize(ch,0);
//        assertEquals(3245,dser.OCADMark);
//        assertEquals(0,   dser.FileType);
//        assertEquals(0,   dser.FileStatus);
//        assertEquals(11,  dser.Version);
//        assertEquals(6,  dser.Subversion);
//        assertEquals(0,  dser.SubSubversion);
//        assertEquals(24924,  dser.FirstSymbolIndexBlk);
//        assertEquals(823596,  dser.ObjectIndexBlock);
//        assertEquals(0,  dser.OfflineSyncSerial);
//        assertEquals(1,  dser.CurrentFileVersion);
//        assertEquals(0,  dser.Res2);
//        assertEquals(0,  dser.Res3);
//        assertEquals(176,  dser.FirstStringIndexBlk);
//    }

}