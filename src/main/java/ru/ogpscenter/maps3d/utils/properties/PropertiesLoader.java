package ru.ogpscenter.maps3d.utils.properties;

import ru.ogpscenter.maps3d.utils.CommandLineUtils;
import ru.ogpscenter.maps3d.utils.OutputUtils;
import ru.ogpscenter.maps3d.utils.SymbolIdMatcher;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

public class PropertiesLoader {

    static  {
        update();
    }

    // ocad input
    public static class ocad_input {
        public static boolean multiply_by_scale = true;
        public static double scale_multiplier = 1;

        public static String contour_1 = "103...";
        public static String contour_1_ignore = "";
        public static String contour_2 = "101...";
        public static String contour_2_ignore = "";
        public static String contour_3 = "102...";
        public static String contour_3_ignore = "";
        public static String contour_4 = "106...";
        public static String contour_4_ignore = "106002";

        public static String slope = "104...";

        public static int getLineType(int symbol_id) {
            if (SymbolIdMatcher.matches(symbol_id,contour_1) && !SymbolIdMatcher.matches(symbol_id,contour_1_ignore)) return 1;
            if (SymbolIdMatcher.matches(symbol_id,contour_2) && !SymbolIdMatcher.matches(symbol_id,contour_2_ignore)) return 2;
            if (SymbolIdMatcher.matches(symbol_id,contour_3) && !SymbolIdMatcher.matches(symbol_id,contour_3_ignore)) return 3;
            if (SymbolIdMatcher.matches(symbol_id,contour_4) && !SymbolIdMatcher.matches(symbol_id,contour_4_ignore)) return 4;
            return -1;
        }

        public static boolean isLine(int symbol_id) {
            return getLineType(symbol_id) != -1;
        }

        public static boolean isSlope(int symbol_id) {
            return SymbolIdMatcher.matches(symbol_id,slope);
        }
    }

    // mesh output
    public static class mesh_output {
        public static double scale = 1;
        public static double z_scale = 1;
        public static double z_offset = 0;
        public static boolean zero_centered = true;
    }

    public static class mesh_creation {
        public static double isoline_height_delta = 15;
        public static double crease_angle = 0.25;
        public static boolean convex_hull_cull = true;
        public static int heightmap_padding = 2;
    }

    public static class textured_patch {

        //public static boolean should_split = true;
        public static int max_vertices = 64000;
        public static double max_area = 0.4;
        public static boolean preserve_aspect = true;
        public static double padding = 0.03;
        //public static boolean uv_shift = false;
        public static int texture_width = 1024;
        public static int texture_height = 1024;
        public static double mask_points_per_unit = 0.5;
    }

    public static class interpolation {
        public static double interpolation_step = 2.5;
        public static double fade_distance = 10;
        public static double fade_strength = 3;
        //public static boolean tangent_fix;
    }

    public static class texture {
        public static String texture_folder = "./textures";
        public static double scale = 2;
    }

    public static double getInterpolationStep() {
        return interpolation.interpolation_step;
    }

    static HashMap<String, HashMap<String,String>> variables = new HashMap<>();

    public static void read_file(String path) throws FileNotFoundException {
        variables.clear();
        variables.putIfAbsent(null,new HashMap<>());
        BufferedReader br = null;
        try {
            br = new BufferedReader( new FileReader(new File(path)) );
        }catch (Exception ignored) {
            CommandLineUtils.reportException(ignored);
            return;
        }

        String current_section = null;
        try {
            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim().toLowerCase();
                if (line.length() <= 1) continue;
                if (line.charAt(0) == ';') continue;
                if (line.charAt(0) == '#') continue;
                if (line.charAt(0) == '[') {
                    current_section = line.substring(1,line.length()-1).toLowerCase();
                    variables.putIfAbsent(current_section,new HashMap<>());
                } else {
                    int equals_ind = line.indexOf('=');
                    if (equals_ind == -1) continue;

                    int comment_ind = line.length();
                    {
                        int comment_ind1 = line.indexOf(';');
                        int comment_ind2 = line.indexOf('#');
                        comment_ind = Math.min( comment_ind1 == -1 ? line.length() : comment_ind1,
                                                comment_ind2 == -1 ? line.length() : comment_ind2);
                    }

                    String parameter = line.substring(0,equals_ind).trim().toLowerCase();

                    if (equals_ind < line.length()-1) {
                        String value = line.substring(equals_ind + 1, comment_ind).trim().toLowerCase();

                        variables.get(current_section).put(parameter, value);
                    } else {
                        variables.get(current_section).put(parameter, "");
                    }
                }
            }
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
        }

        try {
            br.close();
        } catch (Exception ignored) {};

        readAllVariables();
    }

    public static void print_properties() {
        System.out.println("Properties:");
        for (Map.Entry<String,HashMap<String,String>> entry : variables.entrySet()) {
            System.out.println("["+entry.getKey()+"]");
            for (Map.Entry<String,String> kv : entry.getValue().entrySet()) {
                System.out.println(kv.getKey()+" = "+kv.getValue());
            }
            System.out.println();
        }
    }

    public static File createProperties() throws IOException {

        File file = new File("properties.ini");
        if (file.exists()) {
            return file;
        }
        PrintWriter br = new PrintWriter( new FileWriter( file ));

        for (Class c : PropertiesLoader.class.getDeclaredClasses()) {
            String section_name = c.getSimpleName();
            br.println("["+section_name+"]");
            for (Field f : c.getFields()) {
                if (f.getName().charAt(0) == '_') continue;;
                Object val = null;
                try {
                    val = f.get(val);
                    br.println(f.getName()+" = "+val);
                } catch (Exception ex) {
                    br.println(f.getName()+" = ");
                    CommandLineUtils.reportException(ex);
                }
            }
            br.println();
        }

        br.close();

        return file;
    }

    public static String read_field(String section_name, String field_name) {
        if (variables == null) return section_name;
        try {
            return variables.get(section_name).get(field_name);
        } catch (Exception ex) {
            return null;
        }
    }

    public static Object tryRead(Object obj, String section, String field) {

        HashMap<String,String> sec = variables.getOrDefault(section.toLowerCase(),null);
        if (sec == null) return null;
        String value = sec.getOrDefault(field.toLowerCase(),null);
        if (value == null) return null;

        try {
            if (obj instanceof Double) {
                obj = Double.parseDouble(value);
            } else if (obj instanceof Float) {
                obj = Float.parseFloat(value);
            } else if (obj instanceof String) {
                obj = value;
            } else if (obj instanceof Integer) {
                obj = Integer.parseInt(value);
            } else if (obj instanceof Boolean) {
                try {
                    obj = Integer.parseInt(value) != 0;
                } catch (Exception ex) {
                    obj = Boolean.parseBoolean(value);
                }
            }
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
            return null;
        }

        return obj;
    }

    public static void readAllVariables() {

        for (Class c : PropertiesLoader.class.getDeclaredClasses()) {
            String section_name = c.getSimpleName();

            for (Field f : c.getFields()) {
                if (f.getName().charAt(0) == '_') continue;;
                Object val = null;
                try {
                    val = f.get(val);
                    val = tryRead(val,section_name,f.getName());
                    if (val != null) {
                        f.set(val, val);
                    }
                } catch (Exception ex) {
                    CommandLineUtils.reportException(ex);
                }
            }

        }

    }

    private static void init() {
        Path execution_path = Paths.get(OutputUtils.GetExecutionPath());
        Path local_path = Paths.get((new File("")).getAbsolutePath());
        Path[] paths = new Path[] {
            Paths.get(OutputUtils.GetExecutionPath()),
            Paths.get((new File("")).getAbsolutePath())
        };
        String[] filenames = new String[] {
            "properties","config"
        };
        String[] file_extension = new String[] {
            ".ini",".cfg",".properties",".txt",
        };

        cfg_file = null;
        for (int path_i = 0; path_i != paths.length; ++path_i)
            for (int filename_i = 0; filename_i != filenames.length; ++filename_i)
                for (int extension_i = 0; extension_i != file_extension.length; ++extension_i) {
                    Path p = Paths.get(paths[path_i].toAbsolutePath().toString()+"\\"+
                            filenames[filename_i]+file_extension[extension_i]);
                    File f = p.toFile();

                    if (f.exists()) {
                        System.out.println("reading config from "+f.getAbsolutePath());
                        try {
                            cfg_file = p;
                            return;
                        } catch (Exception ignored) {};
                    }

                }

        try {
            if (cfg_file == null) {
                cfg_file = createProperties().toPath();
            }
        } catch (Exception ex) {
            CommandLineUtils.reportException(ex);
        }
    }


    static Path cfg_file;
    static FileTime last_modified = null;
    public static void update() {
        if (cfg_file == null) {
            init();
        }
        try {
            BasicFileAttributes attr = Files.readAttributes(cfg_file, BasicFileAttributes.class);

            attr.lastModifiedTime();
            FileTime modified = attr.lastModifiedTime();

            if (last_modified == null || last_modified.compareTo(modified) == -1) {
                read_file(cfg_file.toString());
                last_modified = modified;
            }
        }catch (Exception ex) {

        }
    }

}
