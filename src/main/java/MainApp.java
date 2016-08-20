/**
 * Created by Artyom.Fomenko on 15.07.2016.
 */

import Display.Drawer;
import Display.GeometryWrapper;
import Display.Renderer;
import Isolines.IIsoline;
import Isolines.IsolineContainer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainApp extends Application implements Initializable {

    private Stage stage;
    @FXML protected AnchorPane ap;
    @FXML protected Button openButton;
    @FXML protected Text statusText;
    @FXML protected Canvas display;
    @FXML protected AnchorPane display_ap;
    @FXML protected CheckBox original_ch_b;
    @FXML protected Button btn_edges;
    @FXML protected Button btn_lines;
    @FXML protected Button btn_graph;
    @FXML protected Button btn_interpolate;
    @FXML protected Button texBtn;


    private MainController mc;
    private Renderer renderer;
    private Drawer drawer;

    private IIsoline highlighted_yellow_last;
    private IIsoline highlighted_yellow;
    private IIsoline highlighted_red_last;
    private IIsoline highlighted_red;
    private IIsoline highlighted_blue_last;
    private IIsoline highlighted_blue;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    public MainApp() {
        this.mc = new MainController();
        this.renderer = new Renderer();
        this.drawer = new Drawer( new GeometryFactory() );
        this.mousePos = new Coordinate(0,0);
        this.mouseIsDown = false;
        this.displayedContainer = null;
        this.originalContainer = null;
        this.current_isoline = null;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        String fxmlFile = "fxml/mainWindow.fxml";
        FXMLLoader loader = new FXMLLoader();
        Parent root = (Parent) loader.load(getClass().getResourceAsStream(fxmlFile));
        stage.setTitle("JavaFX and Maven");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void redraw() {
        if (displayedContainer != null) {
            List<GeometryWrapper> geometry = drawer.draw(displayedContainer,mc.edge);
            renderer.clear();
            renderer.addAll(geometry);
            renderer.addAll( drawer.draw(mc.slopeMarks) );
        };

    }

    private void render() {

        GraphicsContext gc = display.getGraphicsContext2D();

        renderer.render(gc,(float)display.getWidth(),(float)display.getHeight());

        if (highlighted_red != null) renderer.render(drawer.draw(highlighted_red, Color.RED,1),gc,(float)display.getWidth(),(float)display.getHeight());
        if (highlighted_blue != null) renderer.render(drawer.draw(highlighted_blue, Color.BLUE,1),gc,(float)display.getWidth(),(float)display.getHeight());
        if (highlighted_yellow != null) renderer.render(drawer.draw(highlighted_yellow, Color.YELLOW,1),gc,(float)display.getWidth(),(float)display.getHeight());
    }

    @FXML
    public void openButtonAction(ActionEvent event) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open map file");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File f = fileChooser.showOpenDialog(stage);
        if (f != null) {
            try {
                mc.openFile(f);
                statusText.setText("Added " + mc.IsolineCount() + " isolines. Bbox: " + mc.isolineContainer.getEnvelope());
                originalContainer = new IsolineContainer(mc.isolineContainer);
                displayedContainer = mc.isolineContainer;
                redraw();
                renderer.Fit();
                render();
            } catch (FileNotFoundException ex) {
                statusText.setText("File not found");
            } catch (IOException ex) {
                statusText.setText("File reading error: "+ex.getMessage());
            } catch (Exception ex) {
                statusText.setText("File parsing error: "+ex.getMessage());
            }
        }
    }

    boolean mouseIsDown;
    Coordinate mousePos;
    IIsoline current_isoline;

    @FXML void canvasMouseEntered(MouseEvent event) {
        mousePos.x = event.getX();
        mousePos.y = event.getY();
        current_isoline = null;
    }

    @FXML void canvasMouseMove(MouseEvent event) {
        mousePos.x = event.getX();
        mousePos.y = event.getY();
        current_isoline = null;
        highlighted_yellow = null;


        Coordinate mp = new Coordinate(mousePos);
        renderer.screenToLocal(mp,display.getWidth(),display.getHeight());

        Coordinate mp_shifted_top = new Coordinate(mousePos.x-1,mousePos.y-1);
        renderer.screenToLocal(mp_shifted_top,display.getWidth(),display.getHeight());
        Coordinate mp_shifted_bottom = new Coordinate(mousePos.x+2,mousePos.y+2);
        renderer.screenToLocal(mp_shifted_bottom,display.getWidth(),display.getHeight());
        double distance = mp_shifted_top.distance(mp_shifted_bottom);

        if (displayedContainer == null) {
            UpdateHighLights();
            return;
        }
        List<IIsoline> isolines = mc.getIsolinesInCircle(mp_shifted_top.x, mp_shifted_top.y, distance, displayedContainer).collect(Collectors.toList());
        if (isolines.size() > 1) {
            statusText.setText("Hover over multiple isolines");
        } else if (isolines.size() == 0) {
            statusText.setText("No isolines under mouse");
        } else {

            IIsoline il = isolines.get(0);
            current_isoline = il;
            highlighted_yellow = current_isoline;

            statusText.setText("Is closed: " + il.getLineString().isClosed() +
                    "; Type: " + il.getType() +
                    "; Slope side: " + il.getSlopeSide() +
                    "; Height: " + il.getHeight() +
                    "; Line begin = " + il.getLineString().getCoordinateN(0) +
                    "; Line end = " + il.getLineString().getCoordinateN(il.getLineString().getNumPoints() - 1) +
                    "; Line id = " + il.getID());
        }

        statusText.setText(statusText.getText()+" Mouse position: "+mp.x+" "+mp.y);

        UpdateHighLights();
    }

    @FXML void canvasMouseDown(MouseEvent event) {
        mouseIsDown = true;
    }

    @FXML void canvasMouseUp(MouseEvent event) {
        mouseIsDown = false;
    }


    IsolineContainer originalContainer;
    IsolineContainer displayedContainer;
    @FXML
    public void onShowOriginalCHBAction() {
        if (original_ch_b.isSelected()) {
            displayedContainer = originalContainer;
        } else {
            displayedContainer = mc.isolineContainer;
        }
        redraw();
        render();
    }


    @FXML
    public void canvasScroll(ScrollEvent event) {
        // Align map on scrolling operation
        double delta = event.getDeltaY()+event.getDeltaX();
        Coordinate localMousePos = new Coordinate(mousePos);
        renderer.screenToLocal(localMousePos, display.getWidth(), display.getHeight());
        renderer.rescale(localMousePos, Math.pow(0.995, delta));
        render();
    }

    @FXML void onBtnEdgesClick() {
        mc.detectEdge();
        redraw();
        render();
    }

    @FXML void onBtnLinesClick() {
        mc.connectLines();
        if (!original_ch_b.isSelected())
            displayedContainer = mc.isolineContainer;
        redraw();
        render();
    }

    @FXML void onBtnGraphClick() {
        mc.buildGraph();
        redraw();
        render();
    }

    @FXML void onBtnInterpolateClick() {
        mc.interpolate();
        redraw();
        render();
    }

    @FXML void onTextureGenereateClick() {
        mc.generateTexture("texture");
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialized!");

        //Hook to redraw properly on resizing
        InvalidationListener listener = o -> render();

        display.widthProperty().bind(display_ap.widthProperty());
        display.heightProperty().bind(display_ap.heightProperty());

        display.widthProperty().addListener(listener);
        display.heightProperty().addListener(listener);
    }

    public void UpdateHighLights() {
        if (!(highlighted_blue != highlighted_blue_last ||
                highlighted_red != highlighted_red_last ||
                highlighted_yellow != highlighted_yellow_last)) return;

        highlighted_yellow_last = highlighted_yellow;
        highlighted_red_last = highlighted_red;
        highlighted_blue_last = highlighted_blue;

        render();
    }
}
