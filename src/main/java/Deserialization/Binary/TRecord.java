package Deserialization.Binary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Artyom.Fomenko on 19.08.2016.
 */
public class TRecord{
    private String name;
    private int type;
    private int objectIndex;
    private String first;
    private HashMap<Character,String> values;

    public static HashMap<String,Integer> recordIDs;
    public static HashMap<Integer,String> recordNames;

    static {
        recordIDs = new HashMap<>();
        recordIDs.put("null",       0);
        recordIDs.put("CsObject",	1);
        recordIDs.put("Course",	    2);
        recordIDs.put("CsClass",	3);
        recordIDs.put("DataSet",	4);
        recordIDs.put("DbObject",	5);
        recordIDs.put("OimFile",	6);
        recordIDs.put("PrevObj",	7);
        recordIDs.put("BackgroundMap",	8);
        recordIDs.put("Color",	    9);
        recordIDs.put("SpotColor",	10);
        recordIDs.put("FileInfo_OCAD10",	11);
        recordIDs.put("Zoom",	    12);
        recordIDs.put("ImpLayer",	13);
        recordIDs.put("OimFind",	14);
        recordIDs.put("SymTree",	15);
        recordIDs.put("CryptInfo",	16);
        recordIDs.put("Bookmark",	18);
        recordIDs.put("Selection",	19);
        recordIDs.put("GpsAdjustPar",	21);
        recordIDs.put("GpsAdjustPoints",	22);
        recordIDs.put("Group",	    23);
        recordIDs.put("RecentDocs",	24);
        recordIDs.put("CsAutoCdAllocationTable",	25);
        recordIDs.put("RulerGuidesList",	26);
        recordIDs.put("LayoutObjects",	27);
        recordIDs.put("LayoutFontAttributes",	28);
        recordIDs.put("PrintAndExportRectangleList",	29);
        recordIDs.put("DisplayPar",	1024);
        recordIDs.put("OimPar",	    1025);
        recordIDs.put("PrintPar",	1026);
        recordIDs.put("CdPrintPar",	1027);
        recordIDs.put("DefaultBackgroundMapsPar",	1028);
        recordIDs.put("EpsPar",	    1029);
        recordIDs.put("ViewPar",	1030);
        recordIDs.put("CoursePar",	1031);
        recordIDs.put("TiffPar",	1032);
        recordIDs.put("TilesPar",	1033);
        recordIDs.put("DbPar",	    1034);
        recordIDs.put("ExportPar",	1035);
        recordIDs.put("CsExpTextPar",	1037);
        recordIDs.put("CsExpStatPar",	1038);
        recordIDs.put("ScalePar",	1039);
        recordIDs.put("DbCreateObjPar",	1040);
        recordIDs.put("SelectedSpotColors",	1041);
        recordIDs.put("XmlScriptPar",	1042);
        recordIDs.put("BackupPar",	1043);
        recordIDs.put("ExportPartOfMapPar",	1044);
        recordIDs.put("DemPar",	    1045);
        recordIDs.put("GpsImportFilePar",	1046);
        recordIDs.put("ImportXyz",	1047);
        recordIDs.put("RelayCoursesDialog",	1048);
        recordIDs.put("CsAutoControlDescription",	1049);
        recordIDs.put("GpxExportPar",	1050);
        recordIDs.put("KmlInfo",	1051);
        recordIDs.put("GpsRouteAnalyzer",	1052);
        recordIDs.put("CoordinateSystemPar",	1053);
        recordIDs.put("GraticulePar",	1054);
        recordIDs.put("GraticuleNameIndexPar",	1055);
        recordIDs.put("KmzExportPar",	1056);
        recordIDs.put("LegendPar",	1057);
        recordIDs.put("RulersPar",	1058);
        recordIDs.put("RulerGuidesPar",	1059);
        recordIDs.put("DbOptions",	1060);
        recordIDs.put("MapNotes",	1061);
        recordIDs.put("SendFileByEmail",	1062);
        recordIDs.put("MapGridPar",	1063);
        recordIDs.put("DemSlopePar",	1064);
        recordIDs.put("DemProfilePar",	1065);
        recordIDs.put("DemHillshadingPar",	1066);
        recordIDs.put("DemHypsometricMapPar",	1067);
        recordIDs.put("DemClassifyVegetationPar",	1068);
        recordIDs.put("ShapeExportPar",	1069);
        recordIDs.put("DxfExportPar",	1070);
        recordIDs.put("DemImportLasPar",	1071);
        recordIDs.put("MapRoutingPar",	1072);

        recordNames = new HashMap<>();
        for (Map.Entry<String,Integer> e : recordIDs.entrySet()) {
            recordNames.put(e.getValue(),e.getKey());
        }
    }


    public TRecord() {
        name = null;
        type = -1;
        objectIndex = -1;
        first = null;
        values = null;
    }

    public double getValueOrDefaultAsInt(Character key, double def) {
        try {
            return Double.parseDouble(values.getOrDefault(key, Double.toString(def)));
        } catch (Exception ex) {
            return  def;
        }
    }
    public double getValueAsDouble(Character key) {
        return Double.parseDouble(getValue(key));
    }

    public String getValue(Character key) {
        return values.get(key);
    }

    public String getFirst() {
        return first;
    }

    public boolean isValid() {
        return values != null;
    }

    public static TRecord fromTStringIndex(TStringIndex si) {

        TRecord ret = new TRecord();

        ret.type = si.RecType;
        ret.objectIndex = si.ObjIndex;
        ret.name = recordNames.getOrDefault(ret.type,null);


        ArrayList< HashMap<Character,String> > result = new ArrayList<>();

        String[] values = si.getString().split("\\s+");
        if (values.length == 0)
            return new TRecord();

        if (values[0].length() != 0) {
            ret.first = values[0];
        }

        ret.values = new HashMap<>();

        for (int i = 1; i < values.length; ++i) {
            ret.values.put(values[i].charAt(0),values[i].substring(1));
        }

        return ret;
    }

    public int getTypeID() {
        return type;
    }

    public String getTypeName() {
        return name;
    }

    @Override
    public String toString() {
        String ret = name+" (type: "+type+"): ";
        if (first != null) ret += "first: "+first; else ret += "First empty";
        for (Map.Entry<Character,String> val : values.entrySet()) {
            ret += ", "+val.getKey()+": "+val.getValue();
        }
        return ret;
    }
}
