package com.example.javarice_capstone.javarice_capstone;

import com.example.javarice_capstone.javarice_capstone.Server.XAMPP_Initializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1230, 690);
        stage.setTitle("PLAY UNO");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        XAMPP_Initializer.start();
        launch();
    }
}