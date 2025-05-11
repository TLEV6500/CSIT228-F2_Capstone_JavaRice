package com.example.javarice_capstone.javarice_capstone.Factory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.StageStyle;

import java.util.Optional;

public class GameSetupDialogController {
    @FXML private StackPane contentPane;
    @FXML private Label titleLabel;

    public enum MultiplayerType { NONE, HOST, JOIN }
    private MultiplayerType multiplayerType = MultiplayerType.NONE;

    public enum Mode { SINGLEPLAYER, HOST, JOIN, WIN }
    private Mode mode = Mode.SINGLEPLAYER;

    private Optional<Integer> selectedPlayerCount = Optional.empty();
    private Optional<MultiplayerSetupResult> hostGameResult = Optional.empty();
    private Optional<MultiplayerSetupResult> joinGameResult = Optional.empty();

    @FXML
    public void initialize() {
        setMode(Mode.SINGLEPLAYER);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        try {
            String fxml;
            String title;
            switch (mode) {
                case SINGLEPLAYER:
                    fxml = "/com/example/javarice_capstone/javarice_capstone/Dialog/SinglePlayerContent.fxml";
                    title = "Singleplayer Setup";
                    break;
                case HOST:
                    fxml = "/com/example/javarice_capstone/javarice_capstone/Dialog/HostGameContent.fxml";
                    title = "Host Game Setup";
                    break;
                case JOIN:
                    fxml = "/com/example/javarice_capstone/javarice_capstone/Dialog/JoinGameContent.fxml";
                    title = "Join Game Setup";
                    break;
                default:
                    fxml = "";
                    title = "";
            }
            titleLabel.setText(title);

            if (!fxml.isEmpty()) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxml));
                javafx.scene.Parent content = loader.load();

                switch (mode) {
                    case SINGLEPLAYER:
                        SingleplayerContentController singleCtrl = loader.getController();
                        singleCtrl.init(this);
                        break;
                    case HOST:
                        HostGameContentController hostCtrl = loader.getController();
                        hostCtrl.init(this);
                        break;
                    case JOIN:
                        JoinGameContentController joinCtrl = loader.getController();
                        joinCtrl.init(this);
                        break;
                }

                contentPane.getChildren().setAll(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCustomWinModeMainMenuOnly(String message, Runnable mainMenuCallback) {
        titleLabel.setText("Game Over");

        VBox vbox = new VBox(20);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffd54f; -fx-font-weight: bold;");
        msg.setWrapText(true);

        Button mainMenuBtn = new Button("Main Menu");
        mainMenuBtn.setStyle("-fx-background-color: #ef5350; -fx-text-fill: #fff; -fx-font-weight: bold; -fx-background-radius: 7;");
        mainMenuBtn.setOnAction(e -> {
            if (mainMenuCallback != null) mainMenuCallback.run();
            javafx.stage.Window win = ((Button) e.getSource()).getScene().getWindow();
            if (win instanceof Stage) ((Stage) win).close();
        });

        vbox.getChildren().addAll(msg, mainMenuBtn);
        contentPane.getChildren().setAll(vbox);
    }
    void onSingleplayerOk(int playerCount) {
        selectedPlayerCount = Optional.of(playerCount);
        closeDialog();
    }
    void onSingleplayerCancel() {
        selectedPlayerCount = Optional.empty();
        closeDialog();
    }

    void onHostGameOk(String username) {
        hostGameResult = Optional.of(new MultiplayerSetupResult(MultiplayerType.HOST, username, null, null, null));
        closeDialog();
    }
    void onHostGameCancel() {
        hostGameResult = Optional.empty();
        closeDialog();
    }

    void onJoinGameOk(String username, String lobbyAddress,String lobbyCode) {
        joinGameResult = Optional.of(new MultiplayerSetupResult(MultiplayerType.JOIN, username, null, lobbyAddress, lobbyCode));
        closeDialog();
    }
    void onJoinGameCancel() {
        joinGameResult = Optional.empty();
        closeDialog();
    }

    private void closeDialog() {
        if (contentPane == null) return;
        javafx.scene.Scene scene = contentPane.getScene();
        if (scene == null) return;
        javafx.stage.Window window = scene.getWindow();
        if (window instanceof Stage) ((Stage) window).close();
    }

    public Optional<Integer> getSelectedPlayerCount() {
        return selectedPlayerCount;
    }

    public static class MultiplayerSetupResult {
        public final MultiplayerType type;
        public final String username;
        public final Integer playerCount;
        public final String lobbyAddress;
        public final String lobbyCode;

        public MultiplayerSetupResult(MultiplayerType type, String username, Integer playerCount, String lobbyAddress, String lobbyCode) {
            this.type = type;
            this.username = username;
            this.playerCount = playerCount;
            this.lobbyAddress = lobbyAddress;
            this.lobbyCode = lobbyCode;
        }
    }

    public Optional<MultiplayerSetupResult> getHostGameResult() {
        return hostGameResult;
    }

    public Optional<MultiplayerSetupResult> getJoinGameResult() {
        return joinGameResult;
    }

    public static class SingleplayerContentController {
        @FXML private ComboBox<Integer> playerCountComboBox;
        @FXML private Button okButton;
        @FXML private Button cancelButton;

        private GameSetupDialogController parent;

        public void init(GameSetupDialogController parent) {
            this.parent = parent;
            playerCountComboBox.getItems().setAll(2, 3, 4, 5, 6);
        }

        @FXML
        private void initialize() {
            playerCountComboBox.getSelectionModel().selectFirst();
        }

        @FXML
        private void okClicked() {
            Integer count = playerCountComboBox.getValue();
            if (count != null) {
                parent.onSingleplayerOk(count);
            }
        }

        @FXML
        private void cancelClicked() {
            parent.onSingleplayerCancel();
        }
    }
}