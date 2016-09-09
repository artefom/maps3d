package Algorithm.LineConnection;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;

/**
 * Represents connection of lines and it's score.
 * (Connections with higher score are more likely to be connected
 * Line Ends are guaranteed not same, though, may be of same line
 */
public class Connection {

    private LineEnd l1;
    private LineEnd l2;
    double score;
    LineSegment connectionSegment;

    public  Connection() {
        l1 = null;
        l2 = null;
        score = -1;
    }

    private Connection(LineEnd l1, LineEnd l2, GeometryFactory gf) {
        this.l1 = l1;
        this.l2 = l2;
        score = -1;
        getConLine(gf);
    }


    public LineSegment getConnectionSegment() {
        return connectionSegment;
    }

    public static Connection fromLineEnds(LineEnd l1, LineEnd l2) {
        if (l1.equals(l2)) return null;
        return new Connection(l1,l2,l1.isoline.getFactory());
    }

    /**
     * Set line ends of current connection
     * @param l1 {@link LineEnd} of first isoline to be connected
     * @param l2 {@link LineEnd} of second isoline to be connected
     */
    public void SetLineEnds(LineEnd l1, LineEnd l2) {
        this.l1 = l1;
        this.l2 = l2;
        score = -1;
        getConLine(l1.isoline.getFactory());
    }

    private void getConLine(GeometryFactory gf) {
        connectionSegment = new LineSegment(l1.line.p1,l2.line.p1);
    }

    /**
     * Valid isoline satisfies following conditions:
     *
     * Both {@link LineEnd}s are valid (see {@link LineEnd#isValid()}).
     *
     * Slope side of two connecting isolines match.
     * Type of two connecting isolies match
     *
     * @return
     */
    public boolean isValid() {

        // Test none of isolines is null
        if (l1 == null || l2 == null || l1.isoline == null || l2.isoline == null
                || !l1.isoline.isValid() || !l2.isoline.isValid() ) return false;

        if (l1.isoline == l2.isoline) return true;

        //Get resulting slope side from first and second isolines
        int result_ss = -l1.isoline.getSlopeSide()*l1.end_index;
        int second_ss = l2.isoline.getSlopeSide()*l2.end_index;

        if (result_ss == 0) {
            result_ss = second_ss;
        } else {
            if (second_ss != 0 && result_ss != second_ss)
                return false;
        }

        //Test for type complementarity
        if (first().isoline.getType() != second().isoline.getType())
            return false;
        return true;
    };

    /**
     * Slope side of resulting isoline, that will be formed after performing a {@link LineWelder#Weld(Connection)} on this connection
     * @return
     * @throws Exception
     */
    public int resultSlopeSide() throws Exception {
        int result_ss = -l1.isoline.getSlopeSide()*l1.end_index;
        int second_ss = l2.isoline.getSlopeSide()*l2.end_index;

        if (result_ss == 0) {
            result_ss = second_ss;
        } else {
            if (second_ss != 0 && result_ss != second_ss)
                throw new  Exception("Connection not valid!");
        }
        return result_ss;
    }

    /**
     * {@link LineEnd} of first isoline, participated in this connection
     * @return
     */
    public LineEnd first() {
        return l1;
    }

    /**
     * {@link LineEnd} of second isoline, participated in this connection
     * @return
     */
    public LineEnd second() {
        return l2;
    }


    @Override
    public int hashCode() {
        return l1.hashCode()+l2.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Connection))return false;
        Connection other = (Connection)obj;
        if (this.l1.equals(other.l1) && this.l2.equals(other.l2)) return true;
        if (this.l2.equals(other.l1) && this.l1.equals(other.l2)) return true;
        return false;
    }

    @Override
    public String toString() {
        return "CN("+"cfirst="+l1+" csecond="+l2+")";
    }
}
