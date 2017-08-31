import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

public class MemoryViewPane extends BorderPane {

    private final Timeline animation;
    private final List<MemoryUsageChart> memoryUsageCharts = new ArrayList<>();
    private final StringProperty totalUsedHeap = new SimpleStringProperty();
    private final StringProperty gcCollectionCount = new SimpleStringProperty();
    private final StringProperty gcCollectionTime = new SimpleStringProperty();
    private final StringProperty maxHeap = new SimpleStringProperty();
    private final StringProperty uptTime = new SimpleStringProperty();
    private final MemoryMXBean memoryMBean;


    public MemoryViewPane() {
        memoryMBean = ManagementFactory.getMemoryMXBean();
        setTop(createControlPanel());
        Pane box = createMainContent();
        ScrollPane scrollPane = new ScrollPane(box);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);
        final KeyFrame frame =
                new KeyFrame(javafx.util.Duration.millis(1_000),
                        (ActionEvent actionEvent) -> updateHeapInformation());
        animation = new Timeline();
        animation.getKeyFrames().add(frame);
        animation.setCycleCount(Animation.INDEFINITE);
    }

    private Pane createMainContent() {
        VBox box = new VBox();
        ObservableList<Node> vBoxChildren = box.getChildren();

        long now = System.currentTimeMillis();
        MemoryUsageChart memoryUsageChartGlobal = new MemoryUsageChart("Heap", memoryMBean::getHeapMemoryUsage, now);
        addToList(memoryUsageChartGlobal, vBoxChildren);
        Separator separator = new Separator();
        separator.setPrefHeight(2);
        vBoxChildren.add(separator);
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (mpBean.getType() == MemoryType.HEAP) {
                MemoryUsageChart memoryUsageChart = new MemoryUsageChart(mpBean.getName(), mpBean::getUsage, now);
                addToList(memoryUsageChart, vBoxChildren);
            }
        }
        return box;
    }

    private void addToList(MemoryUsageChart memoryUsageChart, ObservableList<Node> vBoxChildren) {
        memoryUsageChart.setPrefHeight(250);
        memoryUsageCharts.add(memoryUsageChart);
        vBoxChildren.add(memoryUsageChart);
        VBox.setVgrow(memoryUsageChart, Priority.ALWAYS);
    }

    private void updateHeapInformation() {
        MemoryUsage heapMemoryUsage = memoryMBean.getHeapMemoryUsage();
        long used = heapMemoryUsage.getUsed();
        totalUsedHeap.setValue(formatByteSize(used));

        long max = heapMemoryUsage.getMax();
        maxHeap.setValue(formatByteSize(max));

        for (MemoryUsageChart memoryUsageChart : memoryUsageCharts) {
            memoryUsageChart.update();
        }
        updateGCStats();
    }

    private void updateGCStats(){
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long mbeanUptime = rb.getUptime();
        String formattedUptime = formatTimeDifference(mbeanUptime);
        uptTime.setValue(formattedUptime);

        long garbageCollectionTime = 0;
        List<String> gcCollections=new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCollections.add(gc.getName()+"="+gc.getCollectionCount());
            garbageCollectionTime += gc.getCollectionTime();
        }
        String formattedGCTime = formatTimeDifference(garbageCollectionTime);
        gcCollectionCount.setValue(String.join(", ",gcCollections));
        gcCollectionTime.setValue(formattedGCTime);

    }


    private static String formatByteSize(long bytes) {
        int unit =  1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = Character.toString("kMGTPE".charAt(exp-1)) ;
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    private String formatTimeDifference(long durationInMilli){
        long secondsInMilli = 1_000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long days = durationInMilli / daysInMilli;
        durationInMilli = durationInMilli % daysInMilli;

        long hours = durationInMilli / hoursInMilli;
        durationInMilli = durationInMilli % hoursInMilli;

        long minutes = durationInMilli / minutesInMilli;
        durationInMilli = durationInMilli % minutesInMilli;

        long seconds = durationInMilli / secondsInMilli;

        return String.format("%S day, %02d hr, %02d min, %02d sec", days, hours, minutes, seconds);

    }

    private Pane createControlPanel() {
        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(createLeftPane());
        borderPane.setRight(createRightPane());
        borderPane.setPadding(new Insets(5, 5, 5, 5));
        Border border = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT, new Insets(5)));
        borderPane.setBorder(border);
        return borderPane;
    }

    private Pane createRightPane(){
        VBox vBox = new VBox();
        vBox.setSpacing(5);
        ObservableList<Node> children = vBox.getChildren();

        Button garbageCollect = new Button("Garbage Collect");
        garbageCollect.setMaxWidth(Double.MAX_VALUE);
        garbageCollect.setOnAction(e -> System.gc());
        children.add(garbageCollect);

        Button heapDump = new Button("Save Heap Dump");
        heapDump.setMaxWidth(Double.MAX_VALUE);
        heapDump.setOnAction(e -> saveHeapDump());
        children.add(heapDump);

        return vBox;
    }

    private void saveHeapDump() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Heap Dump");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Heap Dump File", "*.hprof"));
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            HeapDumper.dumpHeap(file.getAbsolutePath());
        }
    }

    private Pane createLeftPane() {
        GridPane gridPane = new GridPane();

        gridPane.add(createLabel("Used Heap : "), 0, 0);
        Label usedHeadLabel = createRightSideLabel(totalUsedHeap);
        gridPane.add(usedHeadLabel, 1, 0);

        gridPane.add(createLabel("Max Heap : "), 0, 1);
        Label maxHeapLabel = createRightSideLabel(maxHeap);
        gridPane.add(maxHeapLabel, 1, 1);

        gridPane.add(createLabel("Up Time : "), 0, 2);
        Label uptimeLabel = createRightSideLabel(uptTime);
        gridPane.add(uptimeLabel, 1, 2);

        gridPane.add(createLabel("GC Count : "), 0, 3);
        Label gcCountLabel = createRightSideLabel(gcCollectionCount);
        gridPane.add(gcCountLabel, 1, 3);

        gridPane.add(createLabel("Total GC Time : "), 0, 4);
        Label getTimeLabel = createRightSideLabel(gcCollectionTime);
        gridPane.add(getTimeLabel, 1, 4);
        return gridPane;
    }

    private Label createRightSideLabel(StringProperty totalUsedHeap) {
        Label label = new Label();
        label.textProperty().bind(totalUsedHeap);
        GridPane.setHalignment(label, HPos.LEFT);
        label.setTextAlignment(TextAlignment.LEFT);
        label.setPrefWidth(300);
        return label;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        GridPane.setHalignment(label, HPos.RIGHT);
        label.setTextAlignment(TextAlignment.RIGHT);
        return label;
    }

    public void startUpdates() {
        animation.play();
    }

    public void stopUpdates() {
        animation.pause();
    }

}
