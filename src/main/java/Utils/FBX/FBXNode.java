package Utils.FBX;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by Artyom.Fomenko on 03.09.2016.
 */
public class FBXNode {

    public static enum ValidSymbols {
        T,
    }

    public static class Ammount {

        private final int ammount;
        public Ammount(int ammount) {
            this.ammount = ammount;
        }

        @Override
        public String toString() {
            return "*"+ammount;
        }
    }

    public String typename;
    public ArrayList<Object> properties;
    public ArrayList<FBXNode> subNodes;

    public FBXNode(String tpyename) {
        this.typename = tpyename;
        properties = new ArrayList<>();
        subNodes = new ArrayList<>();
    }

    private static String toStr(Object obj) {
        ArrayList<String> ret = new ArrayList<>();
        if (obj instanceof Collection) {
            Iterator it = ((Collection) obj).iterator();
            while (it.hasNext()) {
                Object next = it.next();
                ret.add( toStr(next) );
            }
        } else if (obj instanceof String) {
            ret.add( String.format("\"%s\"",(String)obj) );
        } else if (obj instanceof Double) {
            ret.add( Float.toString( ((Double)obj).floatValue() ) );
        } else
        {
            ret.add( obj.toString() );
        }

        return String.join(",",ret);
    }

    private PrintWriter writer;
    private int indent_size;

    private void ind() {
        for (int i = 0; i != indent_size; ++i)
            writer.print("\t");
    }

    private void write(Object obj) {
        writer.print(toStr(obj));
    }

    private void writeln(Object obj) {
        write(obj);
        writer.println();
    }

    private void writeln() {
        writer.println();
    }

    public void addSubNode(String name, ArrayList<Object> properties) {
        FBXNode newNode = new FBXNode(name);
        for (Object obj : properties) {
            newNode.properties.add(obj);
        }
        subNodes.add(newNode);
    }

    public void addSubNode(String name, Object... properties ) {
        FBXNode newNode = new FBXNode(name);
        for (Object obj : properties) {
            newNode.properties.add(obj);
        }
        subNodes.add(newNode);
    }

    public FBXNode tryGetPNode(String name) {
        for (FBXNode node : subNodes) {
            if (node.typename.compareTo("P") == 0) {
                Object name_prop = node.properties.get(0);
                if (name_prop instanceof String) {
                    if (((String) name_prop).compareTo(name) == 0) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    public FBXNode getPNode(String name) {
        FBXNode pNode = tryGetPNode(name);
        if (pNode == null) {
            pNode = new FBXNode("P");
            subNodes.add(pNode);
        }
        return pNode;
    }

    public void setP(String name, String type1, String type2, String unknown, Object... values ) {
        FBXNode pNode = getPNode(name);
        pNode.properties.clear();
        pNode.properties.add(name);
        pNode.properties.add(type1);
        pNode.properties.add(type2);
        pNode.properties.add(unknown);
        for (Object val : values) {
            pNode.properties.add(val);
        }
    }

    public String getPNodeType(String name) {
        FBXNode pNode = tryGetPNode(name);
        if (pNode == null) return null;

        String typename = (String)pNode.properties.get(0);
        return typename;
    }

    public void serialize(String path) throws FileNotFoundException {
        File f = new File(path);
        PrintWriter pw = new PrintWriter(f);
        serialize(pw,0);
        pw.close();
    }

    public void serialize(PrintWriter pw, int indent_size ) {
        this.writer = pw;
        this.indent_size = indent_size;

        if (typename.length() == 0) {

            pw.println("; FBX 7.3.0 project file");
            if (subNodes.size() != 0) {
                for (FBXNode node : subNodes) {
                    node.serialize(pw,this.indent_size);
                }
            };
            return;
        }

        ind();
        pw.print(typename);
        pw.print(":");
        write(properties);
        if (subNodes.size() != 0) {
            pw.println("{");
            for (FBXNode node : subNodes) {
                node.serialize(pw,this.indent_size+1);
            }
            ind(); pw.print("}");
        };
        pw.println();

        this.writer = null;
    }

}
