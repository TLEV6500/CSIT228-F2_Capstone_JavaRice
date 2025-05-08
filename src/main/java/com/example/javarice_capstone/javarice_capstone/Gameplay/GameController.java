package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Models.Game;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
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
import java.util.List;

public class GameController implements Initializable {
    @FXML private VBox gamePane;
    @FXML private HBox playerHand;
    @FXML private Label playerHandCount;
    @FXML private ImageView discardPileView;
    @FXML private Button drawButton;
    @FXML private Label statusLabel;
    @FXML private Label currentColorLabel;
    @FXML private Rectangle colorIndicator;
    @FXML private StackPane notificationArea;
    @FXML private Label gameDirectionLabel;
    @FXML private Label lastActionLabel;

    // Opponent UI references
    @FXML private HBox opponent1Hand, opponent2Hand, opponent3Hand;
    @FXML private VBox  opponent4Hand, opponent5Hand;
    @FXML private Label opponent1Name, opponent2Name, opponent3Name, opponent4Name, opponent5Name;
    @FXML private Label opponent1HandCount, opponent2HandCount, opponent3HandCount, opponent4HandCount, opponent5HandCount;

    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private boolean isFirstTurn = true;

    // Store rules for this game session
    private GameRules gameRules;

    private static final double PLAYER_CARD_WIDTH = 80 * 0.9;
    private static final double PLAYER_CARD_HEIGHT = 120 * 0.9;
    private static final double OPPONENT_CARD_WIDTH = 55 * 0.95;
    private static final double OPPONENT_CARD_HEIGHT = 80 * 0.95;
    private static final double COMPUTER_CARD_OVERLAP = 20;
    private static final double PLAYER_CARD_OVERLAP = 15;
    private static final int COMPUTER_OVERLAP_EXPAND_CARD_STEP = 5;
    private static final double COMPUTER_OVERLAP_EXPAND_AMOUNT = 8;
    private static final int MAX_RENDERED_COMPUTER_CARDS = 20;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        game = new Game(6);

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

    // Now accepts rules object
    public void startGame(int numPlayers, List<String> playerNames, GameRules rules) {
        game = new Game(numPlayers);
        for (int i = 0; i < Math.min(numPlayers, playerNames.size()); i++) {
            AbstractPlayer player = game.getPlayers().get(i);
            player.setName(playerNames.get(i));
        }
        this.gameRules = rules; // Store the rules for use in game logic
        isFirstTurn = true;
        updateUI();
        updateGameDirectionLabel(true);
    }

    private void updateUI() {
        // Update player hand
        playerHand.getChildren().clear();
        AbstractPlayer humanPlayer = game.getPlayers().get(0);

        playerHand.setAlignment(Pos.CENTER);

        // Show card count for player
        if (playerHandCount != null) {
            playerHandCount.setText("(" + humanPlayer.getHand().size() + " cards)");
        }

        for (int i = 0; i < humanPlayer.getHand().size(); i++) {
            AbstractCard card = humanPlayer.getHand().get(i);
            final int cardIndex = i;

            ImageView cardView = new ImageView();
            cardView.setFitHeight(PLAYER_CARD_HEIGHT);
            cardView.setFitWidth(PLAYER_CARD_WIDTH);

            try {
                cardView.setImage(new Image(getClass().getResourceAsStream(card.getImagePath())));
            } catch (Exception exception) {
                Rectangle cardRect = new Rectangle(PLAYER_CARD_WIDTH, PLAYER_CARD_HEIGHT);
                cardRect.setFill(getJavaFXColor(card.getColor()));
                cardRect.setStroke(Color.BLACK);

                Label cardLabel = new Label(card.toString());

                VBox cardBox = new VBox(cardRect, cardLabel);
                cardBox.setAlignment(Pos.CENTER);
                if (i > 0) {
                    HBox.setMargin(cardBox, new javafx.geometry.Insets(0, 0, 0, -PLAYER_CARD_OVERLAP));
                }
                playerHand.getChildren().add(cardBox);
                cardBox.setOnMouseClicked(e -> handleCardClick(cardIndex));
                continue;
            }

            cardView.setOnMouseClicked(e -> handleCardClick(cardIndex));
            if (i > 0) {
                HBox.setMargin(cardView, new javafx.geometry.Insets(0, 0, 0, -PLAYER_CARD_OVERLAP));
            }
            playerHand.getChildren().add(cardView);
        }
        clearOpponentHands();
        Image cardBack;

        try {
            cardBack = new Image(getClass().getResourceAsStream("/images/cards/card_back.png"));
        } catch (Exception e) {
            cardBack = null;
        }

        setOpponentHandHBox(1, opponent1Name, opponent1Hand, opponent1HandCount, cardBack);
        setOpponentHandHBox(2, opponent2Name, opponent2Hand, opponent2HandCount, cardBack);
        setOpponentHandHBox(3, opponent3Name, opponent3Hand, opponent3HandCount, cardBack);
        setOpponentHandVBox(4, opponent4Name, opponent4Hand, opponent4HandCount, cardBack);
        setOpponentHandVBox(5, opponent5Name, opponent5Hand, opponent5HandCount, cardBack);

        AbstractCard topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(getClass().getResourceAsStream(topCard.getImagePath())));
        } catch (Exception e) {}

        updateColorIndicator();
        AbstractPlayer currentPlayer = game.getCurrentPlayer();
        statusLabel.setText("Current turn: " + currentPlayer.getName());
    }

    private void clearOpponentHands() {
        if (opponent1Hand != null) opponent1Hand.getChildren().clear();
        if (opponent2Hand != null) opponent2Hand.getChildren().clear();
        if (opponent3Hand != null) opponent3Hand.getChildren().clear();
        if (opponent4Hand != null) opponent4Hand.getChildren().clear();
        if (opponent5Hand != null) opponent5Hand.getChildren().clear();
    }

    // For top opponents (HBox)
    private void setOpponentHandHBox(int index, Label nameLabel, HBox handBox, Label handCountLabel, Image cardBack) {
        if (game.getPlayers().size() > index) {
            AbstractPlayer opponent = game.getPlayers().get(index);
            if (nameLabel != null) {
                nameLabel.setText(opponent.getName());
                if (game.getCurrentPlayer() == opponent) {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
                } else {
                    nameLabel.setStyle("");
                }
            }
            if (handBox != null && cardBack != null) {
                handBox.getChildren().clear();
                int opponentHandSize = opponent.getHand().size();

                // Show card count
                if (handCountLabel != null) {
                    handCountLabel.setText("(" + opponentHandSize + " cards)");
                }

                int cardsToRender = Math.min(opponentHandSize, MAX_RENDERED_COMPUTER_CARDS);

                double overlap = COMPUTER_CARD_OVERLAP;
                if (cardsToRender >= 10) {
                    int overlapSteps = ((cardsToRender - 10) / COMPUTER_OVERLAP_EXPAND_CARD_STEP) + 1;
                    overlap += overlapSteps * COMPUTER_OVERLAP_EXPAND_AMOUNT;
                }

                for (int j = 0; j < cardsToRender; j++) {
                    ImageView cardView = new ImageView(cardBack);
                    cardView.setFitHeight(OPPONENT_CARD_HEIGHT);
                    cardView.setFitWidth(OPPONENT_CARD_WIDTH);
                    if (j > 0) {
                        HBox.setMargin(cardView, new javafx.geometry.Insets(0, 0, 0, -overlap));
                    }
                    handBox.getChildren().add(cardView);
                }
            }
        } else {
            if (nameLabel != null) nameLabel.setText("");
            if (handBox != null) handBox.getChildren().clear();
            if (handCountLabel != null) handCountLabel.setText("(0 cards)");
        }
    }

    private void setOpponentHandVBox(int index, Label nameLabel, VBox handBox, Label handCountLabel, Image cardBack) {
        if (game.getPlayers().size() > index) {
            AbstractPlayer opponent = game.getPlayers().get(index);
            if (nameLabel != null) {
                nameLabel.setText(opponent.getName());
                if (game.getCurrentPlayer() == opponent) {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
                } else {
                    nameLabel.setStyle("");
                }
            }
            if (handBox != null && cardBack != null) {
                handBox.getChildren().clear();
                int opponentHandSize = opponent.getHand().size();

                // Show card count
                if (handCountLabel != null) {
                    handCountLabel.setText("(" + opponentHandSize + " cards)");
                }

                int cardsToRender = Math.min(opponentHandSize, MAX_RENDERED_COMPUTER_CARDS);

                double overlap = COMPUTER_CARD_OVERLAP;
                if (cardsToRender >= 10) {
                    int overlapSteps = ((cardsToRender - 10) / COMPUTER_OVERLAP_EXPAND_CARD_STEP) + 1;
                    overlap += overlapSteps * COMPUTER_OVERLAP_EXPAND_AMOUNT;
                }

                for (int j = 0; j < cardsToRender; j++) {
                    ImageView cardView = new ImageView(cardBack);
                    cardView.setFitHeight(OPPONENT_CARD_WIDTH);
                    cardView.setFitWidth(OPPONENT_CARD_HEIGHT);
                    if (j > 0) {
                        VBox.setMargin(cardView, new javafx.geometry.Insets(-overlap, 0, 0, 0));
                    }
                    handBox.getChildren().add(cardView);
                }
            }
        } else {
            if (nameLabel != null) nameLabel.setText("");
            if (handBox != null) handBox.getChildren().clear();
            if (handCountLabel != null) handCountLabel.setText("(0 cards)");
        }
    }

    private void updateColorIndicator() {
        Colors currentColor = game.getCurrentColor();
        colorIndicator.setFill(getJavaFXColor(currentColor));
        currentColorLabel.setText("Current Color: " + currentColor);
    }

    private Color getJavaFXColor(Colors cardColor) {
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

        if (card.getColor() == Colors.WILD) {
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
        Dialog<Colors> dialog = new Dialog<>();
        dialog.setTitle("Choose a Color");
        dialog.setHeaderText("Select a color for the Wild card.");

        Button redButton = new Button("Red");
        redButton.setOnAction(event -> {
            game.setCurrentColor(Colors.RED);
            dialog.setResult(Colors.RED);
            dialog.close();
        });

        Button blueButton = new Button("Blue");
        blueButton.setOnAction(event -> {
            game.setCurrentColor(Colors.BLUE);
            dialog.setResult(Colors.BLUE);
            dialog.close();
        });

        Button greenButton = new Button("Green");
        greenButton.setOnAction(event -> {
            game.setCurrentColor(Colors.GREEN);
            dialog.setResult(Colors.GREEN);
            dialog.close();
        });

        Button yellowButton = new Button("Yellow");
        yellowButton.setOnAction(event -> {
            game.setCurrentColor(Colors.YELLOW);
            dialog.setResult(Colors.YELLOW);
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

        Optional<Colors> result = dialog.showAndWait();
        result.ifPresent(color -> game.setCurrentColor(color));
    }

    private void handleDrawCard() {
        if (game.getCurrentPlayer() != game.getPlayers().get(0)) {
            return;
        }

        game.drawCard();
        lastActionLabel.setText("You drew a card");
        updateUI();
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
                    game = new Game(6);
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
        boolean isComputer = currentPlayer.getClass().getSimpleName().toLowerCase().contains("computer");
        if (isComputer) {
            Platform.runLater(() -> {
                statusLabel.setText("Current turn: " + currentPlayer.getName() + " (thinking...)");
            });

            computerPlayerTimer.schedule(() -> {
                Platform.runLater(this::playComputerTurn);
            }, 1500, TimeUnit.MILLISECONDS);
        }
    }

    private void playComputerTurn() {
        AbstractPlayer computer = game.getCurrentPlayer();
        boolean isComputer = computer.getClass().getSimpleName().toLowerCase().contains("computer");
        if (!isComputer) {
            return;
        }

        // Try to play as long as possible
        boolean hasPlayed = false;
        while (!hasPlayed) {
            int cardToPlay = computer.selectCardToPlay(game.getTopCard(), game.getCurrentColor());
            if (cardToPlay >= 0) {
                AbstractCard selectedCard = computer.getHand().get(cardToPlay);
                // (handle wild cards/color selection if necessary)
                game.playCard(cardToPlay);
                hasPlayed = true;
            } else {
                game.drawCard();
            }
        }

        updateUI();
        checkGameStatus();
        checkAndStartComputerTurn();
    }

    private void shutdown() {
        computerPlayerTimer.shutdownNow();
    }

    // Optionally, provide a getter if you want to query rules elsewhere
    public GameRules getGameRules() {
        return gameRules;
    }
}