import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MemoryUsageViewApp extends Application{

    private MemoryViewPane memoryViewPane;

    private Pane createContent(){
        memoryViewPane = new MemoryViewPane();
        return memoryViewPane;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent(),600, 600));
        primaryStage.show();
        primaryStage.setTitle("Memory Usage");
        memoryViewPane.startUpdates();
    }


    @Override
    public void stop() throws Exception {
        memoryViewPane.stopUpdates();
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
