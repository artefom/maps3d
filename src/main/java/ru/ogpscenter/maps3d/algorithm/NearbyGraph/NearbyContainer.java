package ru.ogpscenter.maps3d.algorithm.NearbyGraph;

import ru.ogpscenter.maps3d.isolines.IIsoline;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Artyom.Fomenko on 26.07.2016.
 */
public class NearbyContainer {

    private ArrayList<AttributedIsoline> isolines;

    public NearbyContainer(Collection<IIsoline> isolines) {
        this.isolines = new ArrayList<>();
        for (IIsoline iso : isolines) {
            this.isolines.add( AttributedIsoline.fromIsoline(iso) );
        }
    }

    public ArrayList<AttributedIsoline> getIsolines() {
        return isolines;
    }
}
