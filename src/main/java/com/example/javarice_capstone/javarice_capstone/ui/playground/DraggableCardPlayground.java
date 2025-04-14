package com.example.javarice_capstone.javarice_capstone.ui.playground;

import com.example.javarice_capstone.javarice_capstone.HelloApplication;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;


public class DraggableCardPlayground extends Application {
    public BorderPane bp_card_main;
    private Pair<Double, Double> curOffset;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(DraggableCardPlayground.class.getResource("draggable-card-playground.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Cards");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void init() throws Exception {
        bp_card_main.setOnMousePressed(saveMouseOffset);
        bp_card_main.setOnMouseDragged((evt)->{
            var x = createCardPane(
                (Pane)bp_card_main.getParent(),
                String.valueOf(Math.random()*255),
                curOffset.getKey(),
                curOffset.getValue());
            System.out.println("Created: "+x);
        });
    }

    public static void main(String[] args) {
        launch();
    }

    private Pane createCardPane(Pane parent, String bodyText, double x, double y) {
        BorderPane card = new BorderPane();
        Text bodyTxt = new Text(bodyText);
        card.setCenter(bodyTxt);
        card.setOnMouseDragged((evt)->{

        });
        parent.getChildren().add(card);
        card.setLayoutX(x);
        card.setLayoutY(y);
        card.setOnMousePressed(saveMouseOffset);
        card.setOnMouseDragged(maintainMouseOffset);
        card.setOnMouseClicked(deleteCardOnRightClick);
        card.setPrefWidth(100);
        card.setPrefHeight(140);
        card.setStyle("-fx-border-width:2px; -fx-border-color:black; -fx-border-radius:8px;");
        return card;
    }

    private Pair<Double, Double> getMouseOffset(Node node, MouseEvent evt) {
        double mouseSceneX = evt.getSceneX();
        double mouseSceneY = evt.getSceneY();
        double nodeX = node.getLayoutX();
        double nodeY = node.getLayoutY();
        double offsetX = mouseSceneX - nodeX;
        double offsetY = mouseSceneY - nodeY;
        return new Pair<>(offsetX, offsetY);
    }

    private final EventHandler<MouseEvent> saveMouseOffset = (evt)->{
        curOffset = getMouseOffset(bp_card_main, evt);
    };

    private final EventHandler<MouseEvent> maintainMouseOffset = (evt)->{
        ((Node) evt.getSource()).setLayoutX(curOffset.getKey());
        ((Node) evt.getSource()).setLayoutY(curOffset.getValue());
    };

    private final EventHandler<MouseEvent> deleteCardOnRightClick = (evt)->{
        if (!evt.isSecondaryButtonDown()) return;
        ((Pane) evt.getSource()).getChildren().remove(evt.getSource());
    };
}
