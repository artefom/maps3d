package Algorithm.NearbyGraph;

import Isolines.IIsoline;

import java.util.*;

/**
 * Created by Artyom.Fomenko on 25.07.2016.
 */
public class Isoline_attributed {

    private IIsoline isoline;
    private int mark;

    public List<NearbyConnection> outcomming;
    public List<NearbyConnection> incomming;
    //public HashMap<Isoline_attributed, Integer > outcomming;

    public Isoline_attributed(IIsoline isoline) {
        mark = -1;
        outcomming = new LinkedList<>();
        incomming = new LinkedList<>();
        this.isoline = isoline;
    }

    public IIsoline getIsoline() {
        return isoline;
    }


    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

//    public void setConnections(HashMap<Isoline_attributed, Integer> weights) {
//        outcomming = new LinkedList<>();
//        for (Map.Entry<Isoline_attributed,Integer> ent : weights.entrySet()) {
//            outcomming.add(new NearbyConnection(this,ent.getKey(),ent.getValue()));
//        }
//    }

    public void addOutcomming(NearbyConnection connection) {
        outcomming.add(connection);
    }

    public void addIncomming(NearbyConnection connection) {
        incomming.add(connection);
    }

    public void resetConnecions() {
        outcomming = new LinkedList<>();
        incomming = new LinkedList<>();
    }

    public boolean destroyConnection(NearbyConnection con) {
        Iterator<NearbyConnection> it = outcomming.iterator();
        while (it.hasNext()) {
            NearbyConnection current_con = it.next();
            if (current_con == con){
                it.remove();
                return true;
            }
        }
        return false;
    }

}
