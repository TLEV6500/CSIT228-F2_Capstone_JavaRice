package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Models.Game;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerComputer;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.util.Objects;
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
    @FXML private ImageView drawPileView;
    @FXML private Label statusLabel;
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

    private static final int MAX_RENDERED_COMPUTER_CARDS = 20;

    private static final double PLAYER_CARD_WIDTH = 70;
    private static final double PLAYER_CARD_HEIGHT = 110;
    private static final double OPPONENT_CARD_WIDTH = 50;
    private static final double OPPONENT_CARD_HEIGHT = 75;

    private static final double COMPUTER_CARD_OVERLAP = 20;
    private static final double PLAYER_CARD_OVERLAP = 10;
    private static final int COMPUTER_OVERLAP_EXPAND_CARD_STEP = 2;
    private static final double COMPUTER_OVERLAP_EXPAND_AMOUNT = 5;

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
        drawPileView.setOnMouseClicked(e -> handleDrawCard());
        checkAndStartComputerTurn();
    }

    public void startGame(int numPlayers, List<String> playerNames, GameRules rules) {
        game = new Game(numPlayers);
        for (int i = 0; i < Math.min(numPlayers, playerNames.size()); i++) {
            AbstractPlayer player = game.getPlayers().get(i);
            player.setName(playerNames.get(i));
        }
        this.gameRules = rules;
        isFirstTurn = true;
        updateUI();
        updateGameDirectionLabel(true);
    }

    private void updateUI() {
        playerHand.getChildren().clear();
        AbstractPlayer humanPlayer = game.getPlayers().get(0);
        playerHand.setAlignment(Pos.CENTER);

        updatePlayerHandCount(humanPlayer);
        renderPlayerHand(humanPlayer);

        clearOpponentHands();
        Image cardBack = loadCardBackImage();

        updateOpponentHands(cardBack);

        updateDiscardAndDrawPiles();
        updateWildCardColor();

        AbstractPlayer currentPlayer = game.getCurrentPlayer();
        statusLabel.setText("Current turn: " + currentPlayer.getName());
    }

    private void updatePlayerHandCount(AbstractPlayer player) {
        if (playerHandCount != null) {
            playerHandCount.setText("(" + player.getHand().size() + " cards)");
        }
    }

    private void renderPlayerHand(AbstractPlayer player) {
        List<AbstractCard> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            Node cardNode = createCardNode(hand.get(i), i);
            if (i > 0) {
                HBox.setMargin(cardNode, new javafx.geometry.Insets(0, 0, 0, -PLAYER_CARD_OVERLAP));
            }
            playerHand.getChildren().add(cardNode);
        }
    }

    private Node createCardNode(AbstractCard card, int cardIndex) {
        try {
            ImageView cardView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(card.getImagePath()))));
            cardView.setFitHeight(PLAYER_CARD_HEIGHT);
            cardView.setFitWidth(PLAYER_CARD_WIDTH);
            cardView.setOnMouseClicked(e -> handleCardClick(cardIndex));
            return cardView;
        } catch (Exception exception) {
            Rectangle cardRect = new Rectangle(PLAYER_CARD_WIDTH, PLAYER_CARD_HEIGHT);
            cardRect.setFill(getJavaFXColor(card.getColor()));
            cardRect.setStroke(Color.BLACK);

            Label cardLabel = new Label(card.toString());
            VBox cardBox = new VBox(cardRect, cardLabel);
            cardBox.setAlignment(Pos.CENTER);
            cardBox.setOnMouseClicked(e -> handleCardClick(cardIndex));
            return cardBox;
        }
    }

    private Image loadCardBackImage() {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cards/card_back.png")));
        } catch (Exception e) {
            return null;
        }
    }

    private void updateOpponentHands(Image cardBack) {
        int computerCount = game.getPlayers().size() - 1;
        Object[][] opponents = {
                {opponent1Name, opponent1Hand, opponent1HandCount, (Runnable)() -> setOpponentHandHBox(1, opponent1Name, opponent1Hand, opponent1HandCount, cardBack)},
                {opponent2Name, opponent2Hand, opponent2HandCount, (Runnable)() -> setOpponentHandHBox(2, opponent2Name, opponent2Hand, opponent2HandCount, cardBack)},
                {opponent3Name, opponent3Hand, opponent3HandCount, (Runnable)() -> setOpponentHandHBox(3, opponent3Name, opponent3Hand, opponent3HandCount, cardBack)},
                {opponent4Name, opponent4Hand, opponent4HandCount, (Runnable)() -> setOpponentHandVBox(4, opponent4Name, opponent4Hand, opponent4HandCount, cardBack)},
                {opponent5Name, opponent5Hand, opponent5HandCount, (Runnable)() -> setOpponentHandVBox(5, opponent5Name, opponent5Hand, opponent5HandCount, cardBack)}
        };

        for (int i = 0; i < opponents.length; i++) {
            if (computerCount >= i + 1) {
                ((Runnable)opponents[i][3]).run();
            } else {
                clearOpponentHandUI((Label)opponents[i][0], (javafx.scene.layout.Pane)opponents[i][1], (Label)opponents[i][2]);
            }
        }
    }

    private void updateDiscardAndDrawPiles() {
        AbstractCard topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(topCard.getImagePath()))));
            drawPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cards/card_back.png"))));
        } catch (Exception ignored) {}
    }

    private void clearOpponentHands() {
        if (opponent1Hand != null) opponent1Hand.getChildren().clear();
        if (opponent2Hand != null) opponent2Hand.getChildren().clear();
        if (opponent3Hand != null) opponent3Hand.getChildren().clear();
        if (opponent4Hand != null) opponent4Hand.getChildren().clear();
        if (opponent5Hand != null) opponent5Hand.getChildren().clear();
    }

    private void clearOpponentHandUI(Label nameLabel, javafx.scene.layout.Pane handBox, Label handCountLabel) {
        if (nameLabel != null) nameLabel.setText("");
        if (handBox != null) handBox.getChildren().clear();
        if (handCountLabel != null) handCountLabel.setText("");
    }

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
        }
    }

    private void updateWildCardColor() {
        AbstractCard topCard = game.getTopCard();
        Colors currentColor = game.getCurrentColor();

        if (topCard == null) return;

        if (topCard.getType() == Types.WILD || topCard.getType() == Types.DRAW_FOUR) {
            if (currentColor != Colors.WILD) {
                String imgPath = null;

                if (topCard.getType() == Types.WILD) imgPath = "/images/cards/" + currentColor.name().toLowerCase() + "_card.png";
                else if (topCard.getType() == Types.DRAW_FOUR) imgPath = "/images/cards/" + currentColor.name().toLowerCase() + "_draw_four.png";

                if (imgPath != null) {
                    try {
                        discardPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(imgPath))));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

    }

    private Color getJavaFXColor(Colors cardColor) {
        return switch (cardColor) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case WILD -> Color.BLACK;
            default -> Color.GRAY;
        };
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
        lastActionLabel.setText(game.getActionDescription(card));
        switch (card.getType()) {
            case SKIP:
                showNotification("SKIP!", Color.RED);
                break;
            case REVERSE:
                showNotification("REVERSE!", Color.ORANGE);
                updateGameDirectionLabel(false);
                break;
            case DRAW_TWO:
                showNotification("+2 CARDS", Color.RED);
                break;
            case DRAW_FOUR:
                showNotification("+4 CARDS", Color.DARKRED);
                break;
            default:
                break;
        }
    }

    private void handleGameRules() {
        if (gameRules == null) return;

        if (gameRules.isClassicAllowJumpIn()) {
            // Allow Jump-In: If a player (not in turn) has an exact match of the card just played, they can play out of turn.
            // You'd need to hook this in your playCard logic, likely in your input/event handlers.
            // Example comment:
            // TODO: Implement logic to allow jump-in when a player (not in turn) has an exact matching card.
        }

        if (gameRules.isClassicStackDrawCards()) {
            // Stack Draw Cards: If a Draw 2 or Draw 4 is played, the next player can stack another Draw card.
            // You'd check this when a player responds to a draw penalty.
            // TODO: Implement draw card stacking logic.
        }

        // --- UNO No Mercy ---
        if (gameRules.isNoMercyChainAllCards()) {
            // Chain All Cards: Player can play multiple matching cards (same value, any color) in one turn.
            // TODO: Implement logic to allow chaining cards in one move.
        }

        if (gameRules.isNoMercyJumpInWilds()) {
            // Jump-In Wilds: Allow jump-in with Wild cards (not just colored cards).
            // TODO: Extend jump-in logic to consider wilds.
        }

        if (gameRules.isNoMercyReverseStack()) {
            // Reverse Stack: Allow stacking Reverse cards to reverse direction multiple times.
            // TODO: Implement reverse stacking logic.
        }

        if (gameRules.isNoMercyDoubleAttackDraws()) {
            // Double Attack Draws: Allow a Draw 2 and Draw 4 to be combined into a single attack.
            // TODO: Implement double attack logic for stacking +2 and +4.
        }

        // --- UNO 7-0 ---
        if (gameRules.isSevenZeroSwapAnyPlayer()) {
            // Swap With Any Player: When a 7 is played, allow the player to swap hands with any player.
            // TODO: Prompt the user to select any player to swap with.
        }

        if (gameRules.isSevenZeroRotateHands()) {
            // Rotate Hands Direction: When a 0 is played, rotate all hands in the direction of play.
            // TODO: Implement hand rotation logic.
        }
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
        AbstractPlayer winner = game.getWinner();
        if (winner != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(winner.getName() + " wins!");
            alert.setContentText("Game finished.");
            alert.showAndWait();

            shutdown();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameSetupUI.fxml"));
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
        }
        AbstractPlayer unoPlayer = game.getUnoPlayer();
        if (unoPlayer != null) {
            statusLabel.setText(unoPlayer.getName() + " has UNO!");
            showNotification("UNO!", Color.PURPLE);
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
        if (!(computer instanceof PlayerComputer)) return;
        ((PlayerComputer) computer).playTurn(game);
        updateUI();
        checkGameStatus();
        checkAndStartComputerTurn();
    }

    private void shutdown() {
        computerPlayerTimer.shutdownNow();
    }

}