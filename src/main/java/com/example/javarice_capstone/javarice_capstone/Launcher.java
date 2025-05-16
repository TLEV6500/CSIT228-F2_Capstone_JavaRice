package com.example.javarice_capstone.javarice_capstone;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.XAMPP_Initializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1230, 690);
        stage.getIcons().add(new Image("images/javarice_icon.png"));
        stage.setTitle("PLAY UNO");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws SQLException {
        XAMPP_Initializer.start();
        launch();
    }
}