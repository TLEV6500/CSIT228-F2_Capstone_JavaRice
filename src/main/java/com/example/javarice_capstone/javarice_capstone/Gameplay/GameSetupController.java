package com.example.javarice_capstone.javarice_capstone.Gameplay;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameSetupController {

    @FXML private Spinner<Integer> numberOfPlayersSpinner;
    @FXML private CheckBox allowStackingCheckBox;
    @FXML private CheckBox allowJumpInCheckBox;
    @FXML private CheckBox drawCardsCheckBox;
    @FXML private CheckBox strictWildDrawFourCheckBox;
    @FXML private Button startGameButton;
    @FXML private Button cancelButton;
    @FXML private VBox playersContainer;
    @FXML private Label dateTimeLabel;

    private int aiPlayerCounter = 1;
    private final String currentUser = "Player";
    private final Random random = new Random();
    private final Set<String> usedAiNames = new HashSet<>();
    private final List<String> namePool = Arrays.asList(
            "Ven", "Raimar", "Grant", "Tim", "Romar", "Aaron", "Zillion", "Raymond", "Seth", "4 AM Gaming", "5 Cans of Red Bull", "No Sleep"
    );

    public void initialize() {
        if (numberOfPlayersSpinner != null) {
            numberOfPlayersSpinner.getValueFactory().setValue(4);
            numberOfPlayersSpinner.valueProperty().addListener(this::onPlayerCountChanged);
        }
        if (startGameButton != null) startGameButton.setOnAction(e -> handleStartGame());
        if (cancelButton != null) cancelButton.setOnAction(e -> handleCancel());
        updateDateTimeLabel();
        if (playersContainer != null) initializePlayersContainer();
    }

    private void updateDateTimeLabel() {
        if (dateTimeLabel != null) {
            dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void initializePlayersContainer() {
        playersContainer.getChildren().clear();
        usedAiNames.clear();
        aiPlayerCounter = 1;
        addPlayerEntry(currentUser, "Host", true);
        addAiPlayer("Jay Vince");
        int aiPlayersToAdd = numberOfPlayersSpinner.getValue() - playersContainer.getChildren().size();
        for (int i = 0; i < aiPlayersToAdd; i++) addAiPlayer(getUniqueAiName());
    }

    private void onPlayerCountChanged(ObservableValue<? extends Integer> obs, Integer oldValue, Integer newValue) {
        if (playersContainer == null || newValue == null) return;
        int currentCount = playersContainer.getChildren().size();
        if (newValue > currentCount) {
            for (int i = 0; i < newValue - currentCount; i++) addAiPlayer(getUniqueAiName());
        } else if (newValue < currentCount) {
            for (int i = currentCount; i > newValue; i--) removeLastAiPlayer();
        }
    }

    private void removeLastAiPlayer() {
        int idx = playersContainer.getChildren().size() - 1;
        if (idx > 0) {
            HBox entry = (HBox) playersContainer.getChildren().get(idx);
            Label nameLabel = (Label) entry.getChildren().get(0);
            usedAiNames.remove(nameLabel.getText());
            playersContainer.getChildren().remove(idx);
        }
    }

    private String getUniqueAiName() {
        if (usedAiNames.size() >= namePool.size()) {
            return "Computer " + aiPlayerCounter++;
        }
        String name;
        do {
            name = namePool.get(random.nextInt(namePool.size()));
        } while (usedAiNames.contains(name));
        usedAiNames.add(name);
        return name;
    }

    private void addPlayerEntry(String name, String role, boolean isHost) {
        HBox entry = new HBox();
        entry.getStyleClass().addAll("player-entry", isHost ? "host-player" : "ai-player");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("player-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("player-role");
        entry.getChildren().addAll(nameLabel, spacer, roleLabel);
        playersContainer.getChildren().add(entry);
    }

    @FXML
    private void addAiPlayer(String name) {
        if (playersContainer != null) addPlayerEntry(name, "AI", false);
        int currentCount = playersContainer.getChildren().size();
        if (numberOfPlayersSpinner != null && currentCount > numberOfPlayersSpinner.getValue()) {
            numberOfPlayersSpinner.getValueFactory().setValue(currentCount);
        }
    }

    @FXML
    private void handleStartGame() {
        try {
            int numberOfPlayers = numberOfPlayersSpinner.getValue();
            List<String> playerNames = new ArrayList<>();
            for (var node : playersContainer.getChildren()) {
                HBox entry = (HBox) node;
                Label nameLabel = (Label) entry.getChildren().get(0);
                playerNames.add(nameLabel.getText());
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
            Parent root = loader.load();
            GameController gameUIController = loader.getController();
            gameUIController.startGame(numberOfPlayers, playerNames);
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UNO - Gameplay");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Setup Game");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}