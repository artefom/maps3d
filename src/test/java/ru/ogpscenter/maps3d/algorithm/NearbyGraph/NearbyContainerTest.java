package ru.ogpscenter.maps3d.algorithm.NearbyGraph;

import TestUtils.TestUtils;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Test;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.SlopeSide;

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

        isolines.add( TestUtils.createIsoline(2, SlopeSide.NONE, "0 1, 1 2, 2 1", gf).getIsoline() );
        isolines.add( TestUtils.createIsoline(2, SlopeSide.NONE, "2 0, 0 0", gf).getIsoline() );
        isolines.add( TestUtils.createIsoline(2, SlopeSide.NONE, "3 0, 2 -2, 1 -1", gf).getIsoline() );
        isolines.add( TestUtils.createIsoline(2, SlopeSide.LEFT, "0 -4, 3 -4", gf).getIsoline() );

        NearbyContainer cont = new NearbyContainer(isolines);

        NearbyEstimator est = new NearbyEstimator(gf);
        est.setStep(0.25);
        est.setPrecision(0.0001);

        NearbyGraphWrapper graph = new NearbyGraphWrapper(est.getRelationGraph(cont));

        graph.ConvertToSpanningTree();

        graph.recoverAllSlopes();

        assertEquals(SlopeSide.LEFT,isolines.get(0).getSlopeSide());
        assertEquals(SlopeSide.RIGHT,isolines.get(1).getSlopeSide());
        assertEquals(SlopeSide.RIGHT,isolines.get(2).getSlopeSide());
        assertEquals(SlopeSide.LEFT,isolines.get(3).getSlopeSide());
    }

}