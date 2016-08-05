//package Algorithm.BuildingIndex;
//
//// Import the basic graphics classes.
//import java.awt.*;
//import javax.swing.*;
//
//public class DebugDrawer extends JFrame{
//    QTree tree;
//    public DebugDrawer(QTree tree){
//        super();
//        this.tree = tree;
//    }
//
//    @Override
//    public void paint(Graphics g){
//        g.drawLine(0,0,150,150); // Draw a line from (10,10) to (150,150)
//    }
//
//    public static void main(String arg[]){
//        DebugDrawer frame = new DebugDrawer();
//        frame.setSize(1000,1000);
//        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // Already there
////        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        frame.setUndecorated(true);
//        frame.setVisible(true);
//    }
//}