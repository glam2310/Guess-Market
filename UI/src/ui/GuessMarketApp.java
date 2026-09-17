package ui;

import engine.IMarketEngine;
import engine.MarketEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuessMarketApp extends Application {

    private IMarketEngine engine;

    @Override
    public void init() throws Exception {
        // init engine just once
        engine = new MarketEngine();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();

        // מעבירים לקונטרולר את המנוע שלנו כדי שיוכל לעבוד מולו
        MainController controller = loader.getController();
        controller.setEngine(engine);

        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.setTitle("Guess Market");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}