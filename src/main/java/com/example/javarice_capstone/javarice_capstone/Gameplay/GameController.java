package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Models.Game;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements Initializable {
    @FXML private VBox gamePane;
    @FXML private HBox playerHand;
    @FXML private VBox opponentArea;
    @FXML private ImageView discardPileView;
    @FXML private Button drawButton;
    @FXML private Label statusLabel;
    @FXML private Label currentColorLabel;
    @FXML private Rectangle colorIndicator;
    @FXML private StackPane notificationArea;
    @FXML private Label gameDirectionLabel;
    @FXML private Label lastActionLabel;

    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private boolean isFirstTurn = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        game = new Game(8);

        if (notificationArea == null) {
            notificationArea = new StackPane();
            notificationArea.setPrefHeight(50);
            gamePane.getChildren().add(1, notificationArea);
        }

        if (gameDirectionLabel == null) {
            gameDirectionLabel = new Label("Direction: Clockwise →");
            gameDirectionLabel.setStyle("-fx-font-weight: bold;");
            HBox directionBox = new HBox(gameDirectionLabel);
            directionBox.setAlignment(Pos.CENTER);
            gamePane.getChildren().add(directionBox);
        }

        if (lastActionLabel == null) {
            lastActionLabel = new Label("Game started!");
            lastActionLabel.setStyle("-fx-font-style: italic;");
            HBox lastActionBox = new HBox(lastActionLabel);
            lastActionBox.setAlignment(Pos.CENTER);
            gamePane.getChildren().add(lastActionBox);
        }

        updateUI();
        drawButton.setOnAction(e -> handleDrawCard());
        checkAndStartComputerTurn();
    }

    public void startGame(int numPlayers, java.util.List<String> playerNames) {
        game = new Game(numPlayers);
        for (int i = 0; i < Math.min(numPlayers, playerNames.size()); i++) {
            AbstractPlayer player = game.getPlayers().get(i);
            player.setName(playerNames.get(i));
        }
        isFirstTurn = true;
        updateUI();
        updateGameDirectionLabel(true);
    }

    private void updateUI() {
        playerHand.getChildren().clear();
        AbstractPlayer humanPlayer = game.getPlayers().get(0);

        for (int i = 0; i < humanPlayer.getHand().size(); i++) {
            AbstractCard card = humanPlayer.getHand().get(i);
            final int cardIndex = i;

            ImageView cardView = new ImageView();
            cardView.setFitHeight(120);
            cardView.setFitWidth(80);

            try {
                cardView.setImage(new Image(getClass().getResourceAsStream(card.getImagePath())));
            } catch (Exception exception) {
                Rectangle cardRect = new Rectangle(80, 120);
                cardRect.setFill(getJavaFXColor(card.getColor()));
                cardRect.setStroke(Color.BLACK);

                Label cardLabel = new Label(card.toString());

                VBox cardBox = new VBox(cardRect, cardLabel);
                cardBox.setAlignment(Pos.CENTER);
                playerHand.getChildren().add(cardBox);
                cardBox.setOnMouseClicked(e -> handleCardClick(cardIndex));
                continue;
            }

            cardView.setOnMouseClicked(e -> handleCardClick(cardIndex));
            playerHand.getChildren().add(cardView);
        }

        opponentArea.getChildren().clear();
        for (int i = 1; i < game.getPlayers().size(); i++) {
            AbstractPlayer opponent = game.getPlayers().get(i);
            HBox opponentBox = new HBox();
            opponentBox.setAlignment(Pos.CENTER);
            opponentBox.setSpacing(10);

            Label nameLabel = new Label(opponent.getName() + "'s hand");
            Label cardCountLabel = new Label(opponent.getHand().size() + " cards");

            if (game.getCurrentPlayer() == opponent) {
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
            }

            opponentBox.getChildren().addAll(nameLabel, cardCountLabel);
            opponentArea.getChildren().add(opponentBox);
        }

        AbstractCard topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(getClass().getResourceAsStream(topCard.getImagePath())));
        } catch (Exception e) {
        }

        updateColorIndicator();
        AbstractPlayer currentPlayer = game.getCurrentPlayer();
        statusLabel.setText("Current turn: " + currentPlayer.getName());
    }

    private void updateColorIndicator() {
        com.example.javarice_capstone.javarice_capstone.enums.Colors currentColor = game.getCurrentColor();
        colorIndicator.setFill(getJavaFXColor(currentColor));
        currentColorLabel.setText("Current Color: " + currentColor);
    }

    private Color getJavaFXColor(com.example.javarice_capstone.javarice_capstone.enums.Colors cardColor) {
        switch (cardColor) {
            case RED: return Color.RED;
            case BLUE: return Color.BLUE;
            case GREEN: return Color.GREEN;
            case YELLOW: return Color.YELLOW;
            case WILD: return Color.BLACK;
            default: return Color.GRAY;
        }
    }

    private void handleCardClick(int cardIndex) {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            return;
        }

        AbstractCard card = game.getPlayers().get(0).getHand().get(cardIndex);

        if (card.getColor() == com.example.javarice_capstone.javarice_capstone.enums.Colors.WILD) {
            showColorSelectionDialog();
            if (game.playCard(cardIndex)) {
                updateLastAction(card);
                updateUI();
                checkGameStatus();
                checkAndStartComputerTurn();
            }
        } else {
            if (game.playCard(cardIndex)) {
                updateLastAction(card);
                updateUI();
                checkGameStatus();
                checkAndStartComputerTurn();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Move");
                alert.setHeaderText("You can't play this card");
                alert.setContentText("The card must match the color or number/type of the top discard card.");
                alert.showAndWait();
            }
        }
    }

    private void updateLastAction(AbstractCard card) {
        String actionText = "Played " + card.toString();

        switch (card.getType()) {
            case SKIP:
                actionText += " - Player skipped!";
                showNotification("SKIP!", Color.RED);
                break;
            case REVERSE:
                actionText += " - Direction reversed!";
                showNotification("REVERSE!", Color.ORANGE);
                updateGameDirectionLabel(false);
                break;
            case DRAW_TWO:
                actionText += " - Next player draws 2 cards!";
                showNotification("+2 CARDS", Color.RED);
                break;
            case DRAW_FOUR:
                actionText += " - Next player draws 4 cards!";
                showNotification("+4 CARDS", Color.DARKRED);
                break;
            default:
                break;
        }

        lastActionLabel.setText(actionText);
    }

    private void showNotification(String message, Color color) {
        Text notification = new Text(message);
        notification.setFont(Font.font("System", FontWeight.BOLD, 24));
        notification.setFill(color);

        notificationArea.getChildren().clear();
        notificationArea.getChildren().add(notification);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.play();

        fadeOut.setOnFinished(e -> notificationArea.getChildren().clear());
    }

    private void updateGameDirectionLabel(boolean isClockwise) {
        if (isFirstTurn) {
            gameDirectionLabel.setText("Direction: Clockwise →");
            isFirstTurn = false;
            return;
        }

        if (isClockwise) {
            gameDirectionLabel.setText("Direction: Clockwise →");
        } else {
            gameDirectionLabel.setText("Direction: Counter-clockwise ←");
        }
    }

    private void showColorSelectionDialog() {
        Dialog<com.example.javarice_capstone.javarice_capstone.enums.Colors> dialog = new Dialog<>();
        dialog.setTitle("Choose a Color");
        dialog.setHeaderText("Select a color for the Wild card.");

        Button redButton = new Button("Red");
        redButton.setOnAction(event -> {
            game.setCurrentColor(com.example.javarice_capstone.javarice_capstone.enums.Colors.RED);
            dialog.setResult(com.example.javarice_capstone.javarice_capstone.enums.Colors.RED);
            dialog.close();
        });

        Button blueButton = new Button("Blue");
        blueButton.setOnAction(event -> {
            game.setCurrentColor(com.example.javarice_capstone.javarice_capstone.enums.Colors.BLUE);
            dialog.setResult(com.example.javarice_capstone.javarice_capstone.enums.Colors.BLUE);
            dialog.close();
        });

        Button greenButton = new Button("Green");
        greenButton.setOnAction(event -> {
            game.setCurrentColor(com.example.javarice_capstone.javarice_capstone.enums.Colors.GREEN);
            dialog.setResult(com.example.javarice_capstone.javarice_capstone.enums.Colors.GREEN);
            dialog.close();
        });

        Button yellowButton = new Button("Yellow");
        yellowButton.setOnAction(event -> {
            game.setCurrentColor(com.example.javarice_capstone.javarice_capstone.enums.Colors.YELLOW);
            dialog.setResult(com.example.javarice_capstone.javarice_capstone.enums.Colors.YELLOW);
            dialog.close();
        });

        redButton.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");
        blueButton.setStyle("-fx-background-color: #6666ff; -fx-text-fill: white;");
        greenButton.setStyle("-fx-background-color: #66ff66; -fx-text-fill: white;");
        yellowButton.setStyle("-fx-background-color: #ffff66; -fx-text-fill: black;");

        HBox buttonBox = new HBox(10, redButton, blueButton, greenButton, yellowButton);
        buttonBox.setAlignment(Pos.CENTER);

        dialog.getDialogPane().setContent(buttonBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<com.example.javarice_capstone.javarice_capstone.enums.Colors> result = dialog.showAndWait();
        result.ifPresent(color -> game.setCurrentColor(color));
    }

    private void handleDrawCard() {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            return;
        }

        game.drawCardForPlayer();
        lastActionLabel.setText("You drew a card");
        updateUI();
        checkAndStartComputerTurn();
    }

    private void checkGameStatus() {
        for (AbstractPlayer player : game.getPlayers()) {
            if (player.hasWon()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText(player.getName() + " wins!");
                alert.setContentText("Game finished.");
                alert.showAndWait();

                shutdown();

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("gameSetupUI.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) statusLabel.getScene().getWindow();
                    stage.getScene().setRoot(root);
                    stage.setTitle("UNO - Setup Game");
                } catch (Exception e) {
                    e.printStackTrace();
                    game = new Game(4);
                    isFirstTurn = true;
                    updateUI();
                    updateGameDirectionLabel(true);
                }
                return;
            } else if (player.hasUno()) {
                String message = player.getName() + " has UNO!";
                statusLabel.setText(message);
                showNotification("UNO!", Color.PURPLE);
            }
        }
    }

    private void checkAndStartComputerTurn() {
        AbstractPlayer currentPlayer = game.getCurrentPlayer();
        // Suppose you mark computers with a method isComputer() or by class type
        boolean isComputer = currentPlayer.getClass().getSimpleName().toLowerCase().contains("computer");
        if (isComputer) {
            Platform.runLater(() -> {
                statusLabel.setText("Current turn: " + currentPlayer.getName() + " (thinking...)");
            });

            computerPlayerTimer.schedule(() -> {
                Platform.runLater(this::playComputerTurn);
            }, 1, TimeUnit.SECONDS);
        }
    }

    private void playComputerTurn() {
        AbstractPlayer computer = game.getCurrentPlayer();
        boolean isComputer = computer.getClass().getSimpleName().toLowerCase().contains("computer");
        if (!isComputer) {
            return;
        }

        int cardToPlay = computer.selectCardToPlay(game.getTopCard(), game.getCurrentColor());
        if (cardToPlay >= 0) {
            AbstractCard selectedCard = computer.getHand().get(cardToPlay);

            if (selectedCard.getColor() == com.example.javarice_capstone.javarice_capstone.enums.Colors.WILD) {
                com.example.javarice_capstone.javarice_capstone.enums.Colors[] colors = {
                        com.example.javarice_capstone.javarice_capstone.enums.Colors.RED,
                        com.example.javarice_capstone.javarice_capstone.enums.Colors.BLUE,
                        com.example.javarice_capstone.javarice_capstone.enums.Colors.GREEN,
                        com.example.javarice_capstone.javarice_capstone.enums.Colors.YELLOW
                };
                game.setCurrentColor(colors[(int) (Math.random() * colors.length)]);
            }

            String computerAction = computer.getName() + " played " + selectedCard.toString();

            switch (selectedCard.getType()) {
                case SKIP:
                    computerAction += " - Player skipped!";
                    showNotification("SKIP!", Color.RED);
                    break;
                case REVERSE:
                    computerAction += " - Direction reversed!";
                    showNotification("REVERSE!", Color.ORANGE);
                    updateGameDirectionLabel(false);
                    break;
                case DRAW_TWO:
                    computerAction += " - Next player draws 2 cards!";
                    showNotification("+2 CARDS", Color.RED);
                    break;
                case DRAW_FOUR:
                    computerAction += " - Next player draws 4 cards!";
                    showNotification("+4 CARDS", Color.DARKRED);
                    break;
                default:
                    break;
            }

            lastActionLabel.setText(computerAction);
            game.playCard(cardToPlay);

            updateUI();
            checkGameStatus();
            checkAndStartComputerTurn();

        } else {
            game.drawCardForPlayer();
            lastActionLabel.setText(computer.getName() + " drew a card");
            updateUI();
            checkAndStartComputerTurn();
        }
    }

    private void shutdown() {
        computerPlayerTimer.shutdownNow();
    }
}