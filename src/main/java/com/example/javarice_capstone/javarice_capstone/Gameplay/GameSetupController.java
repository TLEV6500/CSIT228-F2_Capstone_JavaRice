package com.example.javarice_capstone.javarice_capstone.Gameplay;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
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

    // --- Classic UNO ---
    @FXML private CheckBox classicAllowJumpInCheckBox;
    @FXML private CheckBox classicStackDrawCardsCheckBox;

    // --- UNO No Mercy ---
    @FXML private CheckBox noMercyChainAllCardsCheckBox;
    @FXML private CheckBox noMercyJumpInWildsCheckBox;
    @FXML private CheckBox noMercyReverseStackCheckBox;
    @FXML private CheckBox noMercyDoubleAttackDrawsCheckBox;

    // --- UNO 7-0 ---
    @FXML private CheckBox sevenZeroSwapAnyPlayerCheckBox;
    @FXML private CheckBox sevenZeroRotateHandsCheckBox;

    @FXML private Button startGameButton;
    @FXML private Button cancelButton;
    @FXML private VBox playersContainer;
    @FXML private Label dateTimeLabel;
    @FXML private Button addPlayerButton;
    @FXML private Button removePlayerButton;

    private int aiPlayerCounter = 1;
    private final String currentUser = "Player";
    private final Random random = new Random();
    private final Set<String> usedAiNames = new HashSet<>();
    private final List<String> namePool = Arrays.asList(
            "Ven", "Raimar", "Grant", "Tim", "Jay Vince", "Romar", "Aaron", "Zillion", "Raymond", "Seth", "4 AM Gaming", "5 Cans of Red Bull", "No Sleep"
    );

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 6;

    public void initialize() {
        if (startGameButton != null) startGameButton.setOnAction(e -> handleStartGame());
        if (cancelButton != null) cancelButton.setOnAction(e -> handleCancel());
        if (addPlayerButton != null) addPlayerButton.setOnAction(e -> handleAddPlayer());
        if (removePlayerButton != null) removePlayerButton.setOnAction(e -> handleRemovePlayer());
        updateDateTimeLabel();
        if (playersContainer != null) initializePlayersContainer();
        updateAddRemoveButtons();

    }

    private void updateDateTimeLabel() {
        if (dateTimeLabel != null) {
            dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void initializePlayersContainer() {
        playersContainer.getChildren().clear();
        usedAiNames.clear();
        aiPlayerCounter = 3;
        addPlayerEntry(currentUser, "You", true);
        for (int i = 0; i < 3; i++) addAiPlayer(getUniqueAiName());
        updateAddRemoveButtons();
    }

    private void removeLastAiPlayer() {
        int idx = playersContainer.getChildren().size() - 1;
        if (idx > 0 && playersContainer.getChildren().size() > MIN_PLAYERS) {
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
        if (playersContainer != null) addPlayerEntry(name, "COMPUTER", false);
        updateAddRemoveButtons();
    }

    private void handleAddPlayer() {
        if (playersContainer.getChildren().size() < MAX_PLAYERS) addAiPlayer(getUniqueAiName());
        updateAddRemoveButtons();
    }

    private void handleRemovePlayer() {
        if (playersContainer.getChildren().size() > MIN_PLAYERS) removeLastAiPlayer();
        updateAddRemoveButtons();
    }

    private void updateAddRemoveButtons() {
        int count = playersContainer.getChildren().size();
        if (addPlayerButton != null)
            addPlayerButton.setDisable(count >= MAX_PLAYERS);
        if (removePlayerButton != null)
            removePlayerButton.setDisable(count <= MIN_PLAYERS);
    }

    @FXML
    private void handleStartGame() {
        try {
            int numberOfPlayers = playersContainer.getChildren().size();
            List<String> playerNames = new ArrayList<>();
            for (var node : playersContainer.getChildren()) {
                HBox entry = (HBox) node;
                Label nameLabel = (Label) entry.getChildren().get(0);
                playerNames.add(nameLabel.getText());
            }

            GameRules rules = new GameRules(
                    classicAllowJumpInCheckBox != null && classicAllowJumpInCheckBox.isSelected(),
                    classicStackDrawCardsCheckBox != null && classicStackDrawCardsCheckBox.isSelected(),
                    noMercyChainAllCardsCheckBox != null && noMercyChainAllCardsCheckBox.isSelected(),
                    noMercyJumpInWildsCheckBox != null && noMercyJumpInWildsCheckBox.isSelected(),
                    noMercyReverseStackCheckBox != null && noMercyReverseStackCheckBox.isSelected(),
                    noMercyDoubleAttackDrawsCheckBox != null && noMercyDoubleAttackDrawsCheckBox.isSelected(),
                    sevenZeroSwapAnyPlayerCheckBox != null && sevenZeroSwapAnyPlayerCheckBox.isSelected(),
                    sevenZeroRotateHandsCheckBox != null && sevenZeroRotateHandsCheckBox.isSelected()
            );

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
            Parent root = loader.load();
            GameController gameUIController = loader.getController();

            // Pass rules to your game controller
            gameUIController.startGame(numberOfPlayers, playerNames, rules);

            Stage stage = (Stage) startGameButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
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