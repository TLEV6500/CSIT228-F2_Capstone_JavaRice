package com.example.javarice_capstone.javarice_capstone.ui.playground;

import com.example.javarice_capstone.javarice_capstone.HelloApplication;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class DraggableCardPlayground extends Application {
    public Pane bp_card_main;
    private Pair<Double, Double> curOffset;
    private Date curTimePressed;
    private final long minPressInterval = new Date(0,0,0,0,0,10).getTime();
    private Pane bp_card_cur;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(DraggableCardPlayground.class.getResource("draggable-card-playground.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Cards");
        stage.setScene(scene);
        stage.show();
    }
    public void initialize()  {
        bp_card_main.setOnMousePressed(((evt)->{
//            curTimePressed = new Date();
            saveMouseOffset.handle(evt);
            var card = createCardPane(
                    (Pane)bp_card_main.getParent(),
                    String.valueOf((int) (Math.random() * 255)),
                    bp_card_main.getLayoutX(),
                    bp_card_main.getLayoutY());
            System.out.println("Created: "+card);
            System.out.println("Pos: " + curOffset + "//" + bp_card_main.getLayoutX()+","+bp_card_main.getLayoutY());
            card.fireEvent(evt);
            bp_card_cur = card;
        }));
        bp_card_main.setOnDragDetected((evt)->{
            bp_card_cur.startFullDrag();
            evt.consume();
        });
    }

    public static void main(String[] args) {
        launch();
    }

    private Pane createCardPane(Pane parent, String bodyText, double x, double y) {
        BorderPane card = new BorderPane();
        Text bodyTxt = new Text(bodyText);
        card.setCenter(bodyTxt);
        card.setOnMouseDragged(maintainMouseOffset);
        parent.getChildren().add(card);
        card.setPrefWidth(100);
        card.setPrefHeight(140);
        card.setLayoutX(x);
        card.setLayoutY(y);
        card.setOnMousePressed(saveMouseOffset);
        card.setOnMouseDragged(maintainMouseOffset);
        card.setOnMouseClicked(deleteCardOnRightClick);
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
        Pair p = new Pair<>(offsetX, offsetY);
        System.out.println("Mouse offest: " + p);
        return p;
    }

    private final EventHandler<MouseEvent> saveMouseOffset = (evt)->{
        curOffset = getMouseOffset(bp_card_main, evt);
        System.out.println("Saving mouse offset: " + curOffset);
    };

    private final EventHandler<MouseEvent> maintainMouseOffset = (evt)->{
        System.out.println("Dragging...");
        ((Node) evt.getSource()).setLayoutX(((Node) evt.getSource()).getLayoutX() + curOffset.getKey());
        ((Node) evt.getSource()).setLayoutY(((Node) evt.getSource()).getLayoutY() + curOffset.getValue());
    };

    private final EventHandler<MouseEvent> deleteCardOnRightClick = (evt)->{
        if (!evt.isSecondaryButtonDown()) return;
        ((Pane) evt.getSource()).getChildren().remove(evt.getSource());
    };

    private long getTimeInterval(Date start) {
        return new Date().getTime() - start.getTime();
    }
}
