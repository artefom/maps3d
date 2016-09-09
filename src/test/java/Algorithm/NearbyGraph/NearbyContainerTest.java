package Algorithm.NearbyGraph;

import Isolines.IIsoline;
import TestUtils.TestUtils;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;

/**
 * Created by Artyom.Fomenko on 26.07.2016.
 */
public class NearbyContainerTest {

    GeometryFactory gf;

    public NearbyContainerTest() {
        gf = new GeometryFactory();
    }

    @Test
    public void TestContainerInitialization() {

        LinkedList<IIsoline> isolines = new LinkedList<>();

        isolines.add( TestUtils.createIsoline(2,0,"0 1, 1 2, 2 1",gf).getIsoline() );
        isolines.add( TestUtils.createIsoline(2,0,"2 0, 0 0",gf).getIsoline() );
        isolines.add( TestUtils.createIsoline(2,0,"3 0, 2 -2, 1 -1",gf).getIsoline() );
        isolines.add( TestUtils.createIsoline(2,1,"0 -4, 3 -4",gf).getIsoline() );

        NearbyContainer cont = new NearbyContainer(isolines);

        NearbyEstimator est = new NearbyEstimator(gf);
        est.setStep(0.25);
        est.setPrecision(0.0001);

        NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));

        graph.ConvertToSpanningTree();

        graph.recoverAllSlopes();

        assertEquals(1,isolines.get(0).getSlopeSide());
        assertEquals(-1,isolines.get(1).getSlopeSide());
        assertEquals(-1,isolines.get(2).getSlopeSide());
        assertEquals(1,isolines.get(3).getSlopeSide());
    }

}