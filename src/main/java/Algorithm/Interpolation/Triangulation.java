package Algorithm.Interpolation;

import Utils.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import javafx.scene.canvas.Canvas;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Artyom.Fomenko on 05.08.2016.
 */
public class Triangulation {

    double[][] sobel;
    double[][] heightmap;
    public Triangulation(double[][] heightmap, double[][] sobel) {
        this.sobel = sobel;
        this.heightmap = heightmap;
    }

    class Cell {

        public final int minColumn;
        public final int maxColumn;
        public final int minRow;
        public final int maxRow;

        Cell(int row, int column, int size) {
            minRow = row;
            minColumn = column;
            maxRow = minRow+size;
            maxColumn = minColumn+size;
        }

        Cell(int minRow, int minColumn, int rowSize, int columnSize) {
            this.minRow = minRow;
            this.minColumn = minColumn;
            this.maxRow = minRow+rowSize;
            this.maxColumn = minColumn+columnSize;
        }

        /**
         * Divide current cell into 4 sub-cells.
         * Cells are not necessary equal (since cell boundaries are integer values)
         * @return 4 sub-cells of current cell.
         */
        public List<Cell> split() {
            ArrayList<Cell> ret = new ArrayList<>(4);
            int rowFloor = (maxRow-minRow)/2;
            int rowCeil = rowFloor+(maxRow-minRow)%2;
            int columnFloor = (maxColumn-minColumn)/2;
            int columnCeil = columnFloor+(maxColumn-minColumn)%2;

            Cell c00 = new Cell(minRow,minColumn,rowCeil,columnCeil);
            Cell c01 = new Cell(c00.maxRow,minColumn,rowFloor,columnCeil);
            Cell c10 = new Cell(minRow,c00.maxColumn,rowCeil,columnFloor);
            Cell c11 = new Cell(c00.maxRow,c00.maxColumn,rowFloor,columnFloor);

            ret.add(c00);
            ret.add(c01);
            ret.add(c10);
            ret.add(c11);
            return ret;
        }

        public int minSize() {
            return Math.min(maxRow-minRow,maxColumn-minColumn);
        }

    }

    /**
     * Value, used to determine, whether cell should split.
     * @param c
     * @return summ of sobel(sobel(heightmap)) pixels inside cell.
     * Mathematically speaking, 2d integral of derivative of heightmap over cell.
     */
    public double getSplitScore(Cell c) {
        int startRow = GeomUtils.clamp(c.minRow,0,sobel.length-1);
        int startColumn = GeomUtils.clamp(c.minColumn,0,sobel[0].length-1);
        int endRow = GeomUtils.clamp(c.maxRow,0,sobel.length-1);
        int endColumn = GeomUtils.clamp(c.maxColumn,0,sobel[0].length-1);
        if (startColumn==endColumn || startRow == endRow) return 0;

        double score = 0;
        for (int row = startRow; row != endRow; ++row) {
            for (int column = startColumn; column != endColumn; ++column) {
                score += sobel[row][column];
            }
        }
        return score;
    }


    /**
     * Get coordinate from cell.
     * @param c
     * @return Coordinate in the middle of the cell with z=average height of pixel inside cell
     */
    public Coordinate getCoord(Cell c) {
        int startRow = GeomUtils.clamp(c.minRow,0,sobel.length-1);
        int startColumn = GeomUtils.clamp(c.minColumn,0,sobel[0].length-1);
        int endRow = GeomUtils.clamp(c.maxRow,0,sobel.length-1);
        int endColumn = GeomUtils.clamp(c.maxColumn,0,sobel[0].length-1);
        if (startColumn==endColumn || startRow == endRow) return null;

        double heightSumm = 0;

        for (int row = startRow; row != endRow; ++row) {
            for (int column = startColumn; column != endColumn; ++column) {
                heightSumm += heightmap[row][column];
            }
        }
        heightSumm=heightSumm/(endRow-startRow)/(endColumn-startColumn);
        return new Coordinate((startColumn+endColumn)*0.5,(startRow+endRow)*0.5,heightSumm);
    }

//    public Coordinate getSobelMaximumCoord(Cell c) {
//        int startRow = GeomUtils.clamp(c.minRow,0,sobel.length-1);
//        int startColumn = GeomUtils.clamp(c.minColumn,0,sobel[0].length-1);
//        int endRow = GeomUtils.clamp(c.maxRow,0,sobel.length-1);
//        int endColumn = GeomUtils.clamp(c.maxColumn,0,sobel[0].length-1);
//        if (startColumn==endColumn || startRow == endRow) return null;
//        Coordinate ret = new Coordinate(startColumn,startRow);
//        double maxValue = sobel[startRow][startColumn];
//        double rowAccum = 0;
//        double columnAccum = 0;
//        double totalWeight = 0;
//        for (int row = startRow; row != endRow; ++row) {
//            for (int column = startColumn; column != endColumn; ++column) {
//                rowAccum+= row * sobel[row][column];
//                columnAccum+= column * sobel[row][column];
//                totalWeight += sobel[row][column];
//            }
//        }
//        if (totalWeight == 0) {
//            ret.y = (c.minRow + c.maxRow)*0.5;
//            ret.x = (c.minColumn+c.maxColumn)*0.5;
//        } else {
//            ret.y = rowAccum / totalWeight;
//            ret.x = columnAccum / totalWeight;
//        }
//        return ret;
//    }

    public List<Coordinate> ScatterPoints(double threshold) {

        final int initCellSize = 100;
        final int minCellSize = 3;
        final double divisionThreshold = threshold;

        final int rowCount = sobel.length;
        final int columnCount = sobel[0].length;

        // populate cells;
        final int rowFit = (int)Math.ceil((double)rowCount/initCellSize);
        final int columnFit = (int)Math.ceil((double)columnCount/initCellSize);
        int startRow = -(rowFit*initCellSize-rowCount)/2;
        int startColumn = -(columnFit*initCellSize-columnCount)/2;
        ArrayList<Cell> cells = new ArrayList<>();
        for (int row = 0; row != rowFit; ++row) {
            for (int column = 0; column != columnFit; ++column) {
                cells.add(new Cell(row*initCellSize+startRow,column*initCellSize+startColumn,initCellSize));
            }
        }
        // divide cells;

//        Iterator<Cell> it = cells.iterator();
//        //List<Cell> newCells = new ArrayList<>();
//        while (it.hasNext()) {
//            Cell c = it.next();
//            double val = getSobelRange(c);
//            if (val > divisionThreshold) {
//                cells.addAll(c.split());
//                it.remove();
//            }
//        }
        int i = 0;
        while (i < cells.size()) {
            Cell c = cells.get(i);
            double val = getSplitScore(c);
            if ( val > divisionThreshold && c.minSize()>minCellSize) {
                List<Cell> newCells = c.split();
                for (int j = 0; j < newCells.size()-1; ++j) {
                    cells.add(newCells.get(j));
                }
                cells.set(i,newCells.get(newCells.size()-1));
            } else {
                ++i;
            }
        }

        List<Coordinate> ret = new ArrayList<>();

        int count = 0;
        for (Cell c : cells) {
            Coordinate coord = getCoord(c);
            if (coord == null) continue;
            if (coord != null) {
                count += 1;
                ret.add(coord);
            }
        }

        System.out.println("values.append([ "+threshold+", "+count+"])");

        return ret;
    }

    public void writeToFile(String path) {
        List<Coordinate> points = ScatterPoints(15);

        GeometryFactory gf = new GeometryFactory();

        DelaunayTriangulationBuilder triangulator = new DelaunayTriangulationBuilder();
        triangulator.setSites( points );

        Geometry triangles = triangulator.getTriangles(gf);
        Envelope envelope = triangles.getEnvelopeInternal();

        // coordinates and their indexes
        int last_index = -1;
        // Use hashmap, so it's easy to find specific coordinate
        HashMap<Coordinate,Integer> coordinates = new HashMap<>();

        // sequences of coordinate indexes
        List<int[]> tris = new ArrayList<>();

        // Convert geometry to coordinates and triangles
        for (int i = 0; i != triangles.getNumGeometries(); ++i) {
            int[] tri = new int[3];
            LinearRing ls = (LinearRing)((Polygon)triangles.getGeometryN(i)).getExteriorRing();
            for (int j = 0; j < 3 && j < ls.getNumPoints(); ++j) {
                Coordinate c = ls.getCoordinateN(j);
                if (!coordinates.containsKey(c)) {
                    coordinates.put(c,++last_index);
                }
                int coordinate_index = coordinates.get(c);
                tri[j] = coordinate_index;
            }
            tris.add(tri);
        }

        PrintWriter out;
        try {
            out = new PrintWriter(path+".obj");
        } catch (FileNotFoundException ex){
            throw new RuntimeException("Could not save " + path + ".obj");
        }

        // Convert hashmap to array
        Coordinate[] coord_array = new Coordinate[last_index+1];
        coordinates.forEach((c,i)->coord_array[i]=c);

        // Normalize coordinates. So map is 0-centered and scaled properly
        for (int i = 0; i != coord_array.length; ++i) {
            // Multiply by step, so it will convert rasterized heightmap pixel coordinates to real coordinates.
            coord_array[i].x = (coord_array[i].x-(envelope.getMaxX()-envelope.getMinX())*0.5)*Constants.INTERPOLATION_STEP;
            // Invert y, so map not looks mirrored.
            coord_array[i].y = -(coord_array[i].y-(envelope.getMaxY()-envelope.getMinY())*0.5)*Constants.INTERPOLATION_STEP;
        }

        // Write vertexes
        out.println("#Vertexes");
        for (int i = 0; i != coord_array.length; ++i) {
            Coordinate c = coord_array[i];
            double z = c.z;
            out.println("v "+c.x+" "+z+" "+c.y);
        }

        out.println("#Triangle faces");
        out.println("g map");
        for (int[] tri : tris) {
            // Indexes in .obj format start from 1, so add 1 to indexes before writing
            // Swap second and third indexes, so triangels will face up.
            out.println("f "+(tri[0]+1)+" "+(tri[1]+1)+" "+(tri[2]+1));
        }

        out.close();
    }

}