package com.thenullproject.etherminewebscraper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.thenullproject.etherminewebscraper.controllers.MainController;


public class MainApp extends Application {

    private MainController mainController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("scene.fxml"));
        Parent root = loader.load();
        mainController = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ClassLoader.getSystemResource("styles.css").toExternalForm());

        stage.setTitle("Ethermine.org Page Scraper");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(600);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        mainController.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }

}