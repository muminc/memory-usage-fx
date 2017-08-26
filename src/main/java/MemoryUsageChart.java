import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;


import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;


public class MemoryUsageChart extends BorderPane {

    private static final long KB_CONVERSION = 1000 * 1000;
    private static final String MEMORY_USAGE_CHART_CSS = "MemoryUsageChart.css";
    private static final int Y_AXIS_TICK_COUNT = 16;
    private static final long MAX_MILLI = 120_000;

    private static final int X_AXIS_TICK_UNIT = 10_000;
    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final long startCounter;
    private final long initialUpperBound;

    private XYChart.Series<Number, Number> maxMemorySeries;
    private XYChart.Series<Number, Number> usageSeries;
    private NumberAxis xAxis;
    private String title;
    private Supplier<MemoryUsage> memoryUsageSupplier;
    private long counter;


    private boolean firstUpdateCall = true;
    private NumberAxis yAxis;

    public MemoryUsageChart(String title,  Supplier<MemoryUsage> memoryUsageSupplier, long startCounter){
        this.title = title;
        this.memoryUsageSupplier = memoryUsageSupplier;
        this.counter = startCounter;
        this.startCounter = startCounter;
        this.initialUpperBound = startCounter + MAX_MILLI;

        setCenter(createContent());
    }


    private Parent createContent() {
        MemoryUsage memoryUsage = memoryUsageSupplier.get();
        long used =  memoryUsage.getUsed() / KB_CONVERSION ;
        long max = memoryUsage.getMax() / KB_CONVERSION;
        xAxis = new NumberAxis(startCounter, initialUpperBound, X_AXIS_TICK_UNIT);
        double tickSize = max / Y_AXIS_TICK_COUNT;
        double rounding = 10d;
        yAxis = new NumberAxis(0, Math.max(max,used) , Math.round(tickSize / rounding) * rounding);
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        // setup chart
        final String stockLineChartCss = getClass().getResource(MEMORY_USAGE_CHART_CSS).toExternalForm();
        chart.getStylesheets().add(stockLineChartCss);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        chart.setTitle(title);
        xAxis.setLabel("Time");
        xAxis.setForceZeroInRange(false);
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                long millis = object.longValue();
                LocalDateTime date =
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
                return date.format(FORMATTER);
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        yAxis.setLabel("Memory (MB)");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, "", null));

        chart.setLegendSide(Side.TOP);

        // add starting data
        maxMemorySeries = new XYChart.Series<>();
        maxMemorySeries.setName("Max");
        usageSeries = new XYChart.Series<>();
        usageSeries.setName("Used");

        chart.getData().add(maxMemorySeries);
        chart.getData().add(usageSeries);
        return chart;
    }

    private void updateMemoryUsage() {
        MemoryUsage memoryUsage = memoryUsageSupplier.get();
        long used =  memoryUsage.getUsed() / KB_CONVERSION ;
        long max =  memoryUsage.getMax() / KB_CONVERSION ;
        counter = System.currentTimeMillis();
        if (firstUpdateCall){
            xAxis.setLowerBound(counter);
            firstUpdateCall = false;
        }
        yAxis.setUpperBound(Math.max(used,max));

        final ObservableList<XYChart.Data<Number, Number>> usedHeapSizeList = usageSeries.getData();
        final ObservableList<XYChart.Data<Number, Number>> maxHeapSizeList = maxMemorySeries.getData();
        usedHeapSizeList.add(new XYChart.Data<>(counter, used));
        maxHeapSizeList.add(new XYChart.Data<>(counter,max));
        // if we go over upperboud, delete old data, and change the bounds
        if (counter > initialUpperBound) {
            XYChart.Data<Number, Number> numberNumberData = usedHeapSizeList.get(1);
            Number secondValue = numberNumberData.getXValue();
            xAxis.setLowerBound(secondValue.doubleValue());
            xAxis.setUpperBound(counter);
            usedHeapSizeList.remove(0);
            maxHeapSizeList.remove(0);

        }
    }

    void update() {
        updateMemoryUsage();
    }
}
