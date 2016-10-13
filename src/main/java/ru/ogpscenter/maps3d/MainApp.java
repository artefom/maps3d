package ru.ogpscenter.maps3d;

import Deserialization.DeserializedOCAD;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.concurrent.Task;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.ogpscenter.maps3d.algorithm.dataset.Datagen;
import ru.ogpscenter.maps3d.algorithm.mesh.TexturedPatch;
import ru.ogpscenter.maps3d.display.Drawer;
import ru.ogpscenter.maps3d.display.GeometryWrapper;
import ru.ogpscenter.maps3d.display.Renderer;
import ru.ogpscenter.maps3d.isolines.IIsoline;
import ru.ogpscenter.maps3d.isolines.IsolineContainer;
import ru.ogpscenter.maps3d.mouse.*;
import ru.ogpscenter.maps3d.utils.CommandLineUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by Artyom.Fomenko on 15.07.2016.
 * Main application
 */
public class MainApp extends Application implements Initializable {

    @FXML protected AnchorPane ap;
    @FXML protected Text statusText;
    @FXML protected ProgressBar progressBar;
    @FXML protected Canvas display;
    @FXML protected AnchorPane displayAnchorPane;
    @FXML protected CheckBox showOriginalCheckBox;
    @FXML protected Button healButton;
    @FXML protected Button linesButton;
    @FXML protected Button interpolateButton;
    @FXML protected Button texButton;
    @FXML protected Label actionLabel;
    @FXML protected Label infoLabel;

    private MainController mc;
    private Renderer renderer;
    private Drawer drawer;
    private Stage stage;

    private IIsoline highlighted_yellow_last;
    private IIsoline highlighted_yellow;

    private MouseActionController mouseActions = new MouseActionController();
    private LineString last_render = null;

    private int renderAction_draw_limit = 10;
    private int renderAction_draw_count = 0;
    private DeserializedOCAD texture_ocad_cache = null;

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
        String fxmlFile = "/fxml/mainWindow.fxml";
        String styleFile = "/fxml/mainWindow.css";
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResourceAsStream(fxmlFile));
        stage.setTitle("3d map builder");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(styleFile).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void redraw() {
        if (displayedContainer != null) {
            List<GeometryWrapper> geometry = drawer.draw(displayedContainer,mc.edge);
            renderer.clear();
            renderer.addAll(geometry);
            if (mc.slopeMarks != null) renderer.addAll( drawer.draw(mc.slopeMarks) );
        }
    }

    private void renderAction( Coordinate new_point) {

        GraphicsContext graphicsContext = display.getGraphicsContext2D();
        if (new_point == null && mouseActions.getCoordinates().size() != 0) {
            graphicsContext.setLineWidth(0.2);
            graphicsContext.setStroke(Color.BLACK);
            if (last_render != null) renderer.render(last_render, graphicsContext, (float) display.getWidth(), (float) display.getHeight());
            return;
        }
        if (mouseActions.getCoordinates().size() == 0) {
            // Reset last render
            if (last_render != null) {
                graphicsContext.setLineWidth(2);

                graphicsContext.setStroke(Color.WHITE);
                renderer.render(last_render, graphicsContext, (float) display.getWidth(), (float) display.getHeight());
                last_render = null;
            }
            return;
        }
        Coordinate[] coordinates = new Coordinate[mouseActions.getCoordinates().size()+1];
        int i = 0;
        for (Coordinate c : mouseActions.getCoordinates()) {
            coordinates[i] = c;
            ++i;
        }

        coordinates[i] = new_point;

        GeometryFactory gf = mc.isolineContainer.getFactory();
        LineString new_line_string = gf.createLineString(coordinates);

        // Redraw highlights
        ++renderAction_draw_count;
        if (renderAction_draw_count > renderAction_draw_limit) {
            render();
            renderAction_draw_count = 0;
        }

        if (last_render != null) {
            graphicsContext.setLineWidth(2);

            graphicsContext.setStroke(Color.WHITE);
            renderer.render(last_render, graphicsContext, (float) display.getWidth(), (float) display.getHeight());
        }

        if (new_line_string != null) {
            graphicsContext.setLineWidth(0.2);
            graphicsContext.setStroke(Color.BLACK);
            renderer.render(new_line_string, graphicsContext, (float) display.getWidth(), (float) display.getHeight());
        }
        last_render = new_line_string;
    }

    private void render() {

        GraphicsContext gc = display.getGraphicsContext2D();

        renderer.render(gc,(float)display.getWidth(),(float)display.getHeight());

    }

    abstract class BackgroundTask extends Task<String> {

        private final String taskName;

        BackgroundTask(String taskName) {
            super();
            this.taskName = taskName;
        }

        @Override
        protected String call() throws Exception {
            callWithProgress(this::updateProgress);
            updateProgress(100,100);
            return null;
        }

        @Override
        protected void failed() {
            super.failed();
            updateProgress(0,1);
            statusText.setText(taskName + " failed");
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            updateProgress(1,1);
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            updateProgress(0,1);
            statusText.setText(taskName + " cancelled");
        }

        abstract void callWithProgress(BiConsumer<Integer, Integer> progressUpdate);
    }

    @FXML
    public void openOcadAction(ActionEvent event) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open ocad map");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File ocadFile = fileChooser.showOpenDialog(stage);
        if (ocadFile != null) {
            executeAsBackgroundTask(new BackgroundTask("Open OCAD file action") {
                @Override public void callWithProgress(BiConsumer<Integer, Integer> progressUpdate) {
                    try {
                        updateProgress(0,100);
                        mc.openFile(ocadFile, this::updateProgress);
                        statusText.setText("Added " + mc.IsolineCount() + " isolines. Bbox: " + mc.isolineContainer.getEnvelope());
                        originalContainer = new IsolineContainer(mc.isolineContainer);
                        displayedContainer = mc.isolineContainer;
                    } catch (FileNotFoundException e) {
                        statusText.setText("File not found");
                    } catch (Exception e) {
                        statusText.setText("File parsing error: "+e.getMessage());
                    }
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    redraw();
                    renderer.Fit();
                    render();
                }
            });
        }
    }

    private void executeAsBackgroundTask(final BackgroundTask task) {
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    @FXML
    public void openJsonButtonAction(ActionEvent event) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open json map");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File f = fileChooser.showOpenDialog(stage);
        if (f != null) {
            try {
                mc.openJsonFile(f);
                if (mc.isolineContainer == null) {
                    throw new IOException("Could not parse file");
                }
                statusText.setText("Added " + mc.IsolineCount() + " ru.ogpscenter.maps3d.isolines. Bbox: " + mc.isolineContainer.getEnvelope());
                originalContainer = new IsolineContainer(mc.isolineContainer);
                displayedContainer = mc.isolineContainer;
                redraw();
                renderer.Fit();
                render();
            } catch (FileNotFoundException ex) {
                statusText.setText("File not found");
            } catch (IOException ex) {
                statusText.setText("File reading error: "+ex.getMessage());
                CommandLineUtils.reportException(ex);
            } catch (Exception ex) {
                statusText.setText("File parsing error: "+ex.getMessage());
                CommandLineUtils.reportException(ex);
            }
        }
    }

    @FXML
    public void saveJsonButtonAction(ActionEvent event) throws Exception {

        FileChooser fileChooser1 = new FileChooser();
        fileChooser1.setTitle("Save json map as");


        fileChooser1.setInitialDirectory( (new File(".")).getAbsoluteFile() );

        File file = fileChooser1.showSaveDialog(stage);

        if (file == null) return;

        mc.saveJsonFile(file);

    }

    @FXML
    public void exitJsonButtonAction(ActionEvent event) throws Exception {
        Platform.exit();
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
            //UpdateHighLights();
            return;
        }

        List<IIsoline> isolines = mc.getIsolinesInCircle(mp_shifted_top.x, mp_shifted_top.y, distance, displayedContainer).collect(Collectors.toList());
        if (isolines.size() > 1) {
            infoLabel.setText("Hover over multiple ru.ogpscenter.maps3d.isolines");
        } else if (isolines.size() == 0) {
            infoLabel.setText("No ru.ogpscenter.maps3d.isolines under ru.ogpscenter.maps3d.mouse");
        } else {

            IIsoline il = isolines.get(0);
            current_isoline = il;
            highlighted_yellow = current_isoline;

            infoLabel.setText("Is closed: " + il.getLineString().isClosed() +
                    "; Type: " + il.getType() +
                    "; Slope side: " + il.getSlopeSide() +
                    "; Height: " + il.getHeight() +
                    "; Line begin = " + il.getLineString().getCoordinateN(0) +
                    "; Line end = " + il.getLineString().getCoordinateN(il.getLineString().getNumPoints() - 1) +
                    "; Line id = " + il.getID());
        }

        infoLabel.setText(infoLabel.getText()+" Mouse position: "+mp.x+" "+mp.y);

        UpdateHighLights();
        renderAction(mp);
    }

    @FXML void canvasMouseDown(MouseEvent event) {

        if (displayedContainer != mc.isolineContainer) return;

        Coordinate local = new Coordinate(event.getX(),event.getY());
        renderer.screenToLocal(local,display.getWidth(),display.getHeight());

        if (event.getButton() == MouseButton.PRIMARY) {
            mouseActions.addPoint(local);
        }
        if (event.getButton() == MouseButton.SECONDARY) {
            // Put end point (causes execution of action

            // Calculate distance threshold based on pixels.
            Coordinate mp_shifted_top = new Coordinate(mousePos.x-1,mousePos.y-1);
            renderer.screenToLocal(mp_shifted_top,display.getWidth(),display.getHeight());
            Coordinate mp_shifted_bottom = new Coordinate(mousePos.x+2,mousePos.y+2);
            renderer.screenToLocal(mp_shifted_bottom,display.getWidth(),display.getHeight());
            double distance = mp_shifted_top.distance(mp_shifted_bottom);

            try {
                mouseActions.execute(displayedContainer, distance);
            } catch (Exception ex) {
                statusText.setText("Could not perform tool action: " + ex.getMessage());
            }

            mouseActions.clear();
            renderAction(null);
            redraw();
            render();
        }

    }

    @FXML void canvasMouseUp(MouseEvent event) {
        mouseIsDown = false;
    }


    IsolineContainer originalContainer;
    IsolineContainer displayedContainer;
    @FXML
    public void onShowOriginalCHBAction() {
        if (showOriginalCheckBox.isSelected()) {
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

        renderAction(null);
        redrawHighlights();
    }

    @FXML void onAlgorithmHealPressed() {
        executeAsBackgroundTask(new BackgroundTask("Heal isolines action") {
            @Override
            void callWithProgress(BiConsumer<Integer, Integer> progressUpdate) {
                mc.heal(progressUpdate);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                redraw();
                render();
            }
        });
    }
//
//    @FXML void algorithm_edge_pressed() {
//        mc.detectEdge();
//        redraw();
//        render();
//    }

    @FXML void onAlgorithmLinesPressed() {
        executeAsBackgroundTask(new BackgroundTask("Connect action") {
            @Override
            void callWithProgress(BiConsumer<Integer, Integer> progressUpdate) {
                mc.connectLines(progressUpdate);
                displayedContainer = mc.isolineContainer;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                redraw();
                render();
            }
        });
    }

//    @FXML void algorithm_graph_pressed() {
//        mc.buildGraph();
//        redraw();
//        render();
//    }

    @FXML void onAlgorithmInterpolatePressed() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save mesh as");

        fileChooser.setInitialFileName("sample");
        fileChooser.setInitialDirectory( (new File(".")).getAbsoluteFile() );

        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();

        executeAsBackgroundTask(new BackgroundTask("Interpolate action") {
            @Override
            void callWithProgress(BiConsumer<Integer, Integer> progressUpdate) {
                mc.saveMesh(path, progressUpdate);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                redraw();
                render();
            }
        });
    }

    @FXML void onAlgorithmTexturePressed() {
        FileChooser saveFileChooser = new FileChooser();
        saveFileChooser.setTitle("Save texture as");

        saveFileChooser.setInitialFileName(TexturedPatch.getDefaultTextureNameBase());
        saveFileChooser.setInitialDirectory( (new File(".")).getAbsoluteFile() );

        File file = saveFileChooser.showSaveDialog(stage);
        if (file == null) {
          return;
        }

        executeAsBackgroundTask(new BackgroundTask("Generate textures action") {
            @Override
            void callWithProgress(BiConsumer<Integer, Integer> progressUpdate) {
                String texture_output_path = file.getAbsolutePath();
                if (!mc.generateTexture(texture_output_path, progressUpdate)) {
                    if (texture_ocad_cache == null) {
                        FileChooser ocadFileChooser = new FileChooser();
                        ocadFileChooser.setTitle("Open ocad map");
                        ocadFileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
                        File ocadFile = ocadFileChooser.showOpenDialog(stage);
                        if (ocadFile != null) {
                            try {
                                texture_ocad_cache = new DeserializedOCAD();
                                texture_ocad_cache.DeserializeMap(ocadFile, null);
                            } catch (FileNotFoundException ex) {
                                statusText.setText("File not found");
                            } catch (IOException ex) {
                                statusText.setText("File reading error: "+ex.getMessage());
                            } catch (Exception ex) {
                                statusText.setText("File parsing error: "+ex.getMessage());
                            }
                        }
                    }
                    mc.generateTexture(texture_output_path, texture_ocad_cache, progressUpdate);
                }
            }
        });
    }

    @FXML
    void onAlgorithmDataPressed() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save dataset as");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setInitialFileName("connections_dataset_0");
        fileChooser.setSelectedExtensionFilter(extFilter);
        fileChooser.setInitialDirectory( (new File(".")).getAbsoluteFile() );

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();
        executeAsBackgroundTask(new BackgroundTask("Generate data action") {
            @Override
            void callWithProgress(BiConsumer<Integer, Integer> progressUpdate) {
                Datagen.generateData(mc.isolineContainer, path, progressUpdate);
            }
        });
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Hook to redraw properly on resizing
        InvalidationListener listener = o -> render();

        display.widthProperty().bind(displayAnchorPane.widthProperty());
        display.heightProperty().bind(displayAnchorPane.heightProperty());

        display.widthProperty().addListener(listener);
        display.heightProperty().addListener(listener);

        actionLabel.setText("Current tool: none");
    }


    public void redrawHighlights() {
        if (highlighted_yellow != null) {
            GraphicsContext gc = display.getGraphicsContext2D();
            renderer.render(drawer.draw(highlighted_yellow, Color.YELLOW,5),gc,(float)display.getWidth(),(float)display.getHeight());
            renderer.render(drawer.draw(highlighted_yellow, Color.RED,1),gc,(float)display.getWidth(),(float)display.getHeight());
        }
    }

    public void UpdateHighLights() {
        if (highlighted_yellow == highlighted_yellow_last) return;

        GraphicsContext gc = display.getGraphicsContext2D();
        if (highlighted_yellow_last != null) {
            renderer.render(drawer.draw(highlighted_yellow_last, Color.WHITE,5),gc,(float)display.getWidth(),(float)display.getHeight());
            renderer.render(drawer.draw(highlighted_yellow_last, null,1),gc,(float)display.getWidth(),(float)display.getHeight());
        }

        if (highlighted_yellow != null) {
            renderer.render(drawer.draw(highlighted_yellow, Color.YELLOW,5),gc,(float)display.getWidth(),(float)display.getHeight());
            renderer.render(drawer.draw(highlighted_yellow, Color.RED,1),gc,(float)display.getWidth(),(float)display.getHeight());
        }


        highlighted_yellow_last = highlighted_yellow;
    }

    public void tool_cut_pressed() {
        mouseActions.setAction(new ActionCut());
        actionLabel.setText("Current tool: cut");
    }

    public void tool_delete_pressed() {
        mouseActions.setAction(new ActionDelete());
        actionLabel.setText("Current tool: delete");
    }

    public void tool_slope_pressed() {
        mouseActions.setAction(new ActionSlope());
        actionLabel.setText("Current tool: slope");
    }

    public void tool_connect_pressed() {
        mouseActions.setAction(new ActionConnect());
        actionLabel.setText("Current tool: connect");
    }

    public void tool_move_pressed() {
        mouseActions.setAction(new ActionMove());
        actionLabel.setText("Current tool: move");
    }

}
