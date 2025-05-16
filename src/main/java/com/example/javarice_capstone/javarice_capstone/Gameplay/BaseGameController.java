package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Factory.GameSetupDialogController;
import com.example.javarice_capstone.javarice_capstone.Models.*;
import com.example.javarice_capstone.javarice_capstone.enums.*;
import com.example.javarice_capstone.javarice_capstone.Abstracts.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.List;

public abstract class BaseGameController implements Initializable {
    @FXML protected HBox playerHand;
    @FXML protected Label MainPlayerHandCount;
    @FXML protected ImageView discardPileView;
    @FXML protected ImageView drawPileView;
    @FXML protected Label statusLabel;
    @FXML protected StackPane notificationArea;
    @FXML protected BorderPane rootBorderPane;
    @FXML protected Button exitButton;

    @FXML protected HBox opponent1Hand, opponent2Hand, opponent3Hand;
    @FXML protected VBox opponent4Hand, opponent5Hand;
    @FXML protected Label opponent1Name, opponent2Name, opponent3Name, opponent4Name, opponent5Name;
    @FXML protected Label opponent1HandCount, opponent2HandCount, opponent3HandCount, opponent4HandCount, opponent5HandCount;

    @FXML protected Label direction;
    @FXML protected Label prev_move_Label;
    @FXML protected Label MainPlayerLabel;

    protected Game game;
    protected boolean isFirstTurn = true;
    protected boolean isShuttingDown = false;

    protected static final int MAX_RENDERED_COMPUTER_CARDS = 20;
    protected static final double PLAYER_CARD_WIDTH = 65;
    protected static final double PLAYER_CARD_HEIGHT = 105;
    protected static final double OPPONENT_CARD_WIDTH = 45;
    protected static final double OPPONENT_CARD_HEIGHT = 70;
    protected static final double COMPUTER_CARD_OVERLAP = 20;
    protected static final double PLAYER_CARD_OVERLAP = 15;
    protected static final int COMPUTER_OVERLAP_EXPAND_CARD_STEP = 2;
    protected static final double COMPUTER_OVERLAP_EXPAND_AMOUNT = 5;

    protected AbstractCard lastPlayedCard = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (notificationArea == null) {
            notificationArea = new StackPane();
            notificationArea.setPrefHeight(50);
        }
        Platform.runLater(() -> {
            BorderPane root = this.rootBorderPane;
            Scene scene = root.getScene();
            if (scene != null) {
                root.maxWidthProperty().bind(scene.widthProperty().multiply(0.93));
                root.maxHeightProperty().bind(scene.heightProperty().multiply(0.90));
            }
        });

        drawPileView.setOnMouseClicked(e -> handleDrawCard());
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void startGame(int numPlayers, List<String> playerNames) {
        for (int i = 0; i < Math.min(numPlayers, playerNames.size()); i++) {
            AbstractPlayer player = game.getPlayers().get(i);
            player.setName(playerNames.get(i));
        }
        isFirstTurn = true;
        updateUI();
        updateGameDirectionLabel();
    }

    protected void checkTurnAndUpdateColors() {
        int current = game.getCurrentPlayerIndex();

        if (MainPlayerLabel != null) {
            if (current == 0) MainPlayerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
            else MainPlayerLabel.setStyle("");
        }
        Label[] opponentLabels = {opponent1Name, opponent2Name, opponent3Name, opponent4Name, opponent5Name};
        for (int i = 1; i < game.getPlayers().size(); i++) {
            Label oppLabel = opponentLabels[i-1];
            if (oppLabel != null) {
                if (i == current) oppLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
                else oppLabel.setStyle("");
            }
        }

        AbstractPlayer currentPlayer = game.getCurrentPlayer();
        if (statusLabel != null && currentPlayer != null) {
            Platform.runLater(() -> {
                if (currentPlayer instanceof PlayerComputer) statusLabel.setText("TURN: " + currentPlayer.getName() + " (THINKING...)");
                else statusLabel.setText("TURN: " + currentPlayer.getName());
            });
        }
    }

    protected void updateUI() {
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

        checkTurnAndUpdateColors();
        updateGameDirectionLabel();
        updateStackingNotification();

        if (prev_move_Label != null && lastPlayedCard != null) {
            prev_move_Label.setText("PREV MOVE: " + game.getActionDescription(lastPlayedCard));
        }
    }

    protected void updatePlayerHandCount(AbstractPlayer player) {
        if (MainPlayerHandCount != null) MainPlayerHandCount.setText("(" + player.getHand().size() + " cards)");
    }

    protected void renderPlayerHand(AbstractPlayer player) {
        List<AbstractCard> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            Node cardNode = createCardNode(hand.get(i), i);
            if (i > 0) {
                HBox.setMargin(cardNode, new javafx.geometry.Insets(0, 0, 0, -PLAYER_CARD_OVERLAP));
            }
            playerHand.getChildren().add(cardNode);
        }
    }

    protected Node createCardNode(AbstractCard card, int cardIndex) {
        try {
            ImageView cardView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(card.getImagePath()))));
            cardView.setFitHeight(PLAYER_CARD_HEIGHT);
            cardView.setFitWidth(PLAYER_CARD_WIDTH);
            cardView.setOnMouseClicked(e -> handleCardClick(cardIndex));
            return cardView;
        } catch (Exception exception) {
            Rectangle cardRect = new Rectangle(PLAYER_CARD_WIDTH, PLAYER_CARD_HEIGHT);

            Label cardLabel = new Label(card.toString());
            VBox cardBox = new VBox(cardRect, cardLabel);
            cardBox.setAlignment(Pos.CENTER);
            cardBox.setOnMouseClicked(e -> handleCardClick(cardIndex));
            return cardBox;
        }
    }

    protected Image loadCardBackImage() {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cards/card_back.png")));
        } catch (Exception e) {
            return null;
        }
    }

    protected void updateOpponentHands(Image cardBack) {
        int computerCount = game.getPlayers().size() - 1;
        Object[][] opponents = {
                {opponent1Name, opponent1Hand, opponent1HandCount, (Runnable)() -> setOpponentHandHBox(1, opponent1Name, opponent1Hand, opponent1HandCount, cardBack)},
                {opponent2Name, opponent2Hand, opponent2HandCount, (Runnable)() -> setOpponentHandHBox(2, opponent2Name, opponent2Hand, opponent2HandCount, cardBack)},
                {opponent3Name, opponent3Hand, opponent3HandCount, (Runnable)() -> setOpponentHandHBox(3, opponent3Name, opponent3Hand, opponent3HandCount, cardBack)},
                {opponent4Name, opponent4Hand, opponent4HandCount, (Runnable)() -> setOpponentHandVBox(4, opponent4Name, opponent4Hand, opponent4HandCount, cardBack)},
                {opponent5Name, opponent5Hand, opponent5HandCount, (Runnable)() -> setOpponentHandVBox(5, opponent5Name, opponent5Hand, opponent5HandCount, cardBack)}
        };

        for (int i = 0; i < opponents.length; i++) {
            if (computerCount >= i + 1) ((Runnable)opponents[i][3]).run();
            else clearOpponentHandUI((Label)opponents[i][0], (javafx.scene.layout.Pane)opponents[i][1], (Label)opponents[i][2]);
        }
    }

    protected void updateDiscardAndDrawPiles() {
        AbstractCard topCard = game.getTopCard();
        try {
            discardPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(topCard.getImagePath()))));
            drawPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cards/card_back.png"))));
        } catch (Exception ignored) {}
    }

    protected void clearOpponentHands() {
        if (opponent1Hand != null) opponent1Hand.getChildren().clear();
        if (opponent2Hand != null) opponent2Hand.getChildren().clear();
        if (opponent3Hand != null) opponent3Hand.getChildren().clear();
        if (opponent4Hand != null) opponent4Hand.getChildren().clear();
        if (opponent5Hand != null) opponent5Hand.getChildren().clear();
    }

    protected void clearOpponentHandUI(Label nameLabel, javafx.scene.layout.Pane handBox, Label handCountLabel) {
        if (nameLabel != null) nameLabel.setText("");
        if (handBox != null) handBox.getChildren().clear();
        if (handCountLabel != null) handCountLabel.setText("");
    }

    protected void setOpponentHandHBox(int index, Label nameLabel, HBox handBox, Label handCountLabel, Image cardBack) {
        if (game.getPlayers().size() > index) {
            AbstractPlayer opponent = game.getPlayers().get(index);
            if (nameLabel != null) nameLabel.setText(opponent.getName());

            if (handBox != null && cardBack != null) {
                handBox.getChildren().clear();
                int opponentHandSize = opponent.getHand().size();
                if (handCountLabel != null) handCountLabel.setText("(" + opponentHandSize + " cards)");
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
                    if (j > 0) HBox.setMargin(cardView, new javafx.geometry.Insets(0, 0, 0, -overlap));
                    handBox.getChildren().add(cardView);
                }
            }
        }
    }

    protected void setOpponentHandVBox(int index, Label nameLabel, VBox handBox, Label handCountLabel, Image cardBack) {
        if (game.getPlayers().size() > index) {
            AbstractPlayer opponent = game.getPlayers().get(index);
            if (nameLabel != null) nameLabel.setText(opponent.getName());

            if (handBox != null && cardBack != null) {
                handBox.getChildren().clear();
                int opponentHandSize = opponent.getHand().size();
                if (handCountLabel != null) handCountLabel.setText("(" + opponentHandSize + " cards)");
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
                    if (j > 0) VBox.setMargin(cardView, new javafx.geometry.Insets(-overlap, 0, 0, 0));
                    handBox.getChildren().add(cardView);
                }
            }
        }
    }

    protected void updateWildCardColor() {
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
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @FXML
    protected void handleExit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/SetupDialog.fxml"));
            Parent dialogRoot = loader.load();
            GameSetupDialogController dialogController = loader.getController();

            dialogController.showExitGameDialog("Are you sure you want to exit the current game session?",
                    () -> {
                        goToMainMenu();
                        Stage stage = (Stage) dialogRoot.getScene().getWindow();
                        if (stage != null) stage.close();
                    }, () -> {

                    }
            );

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Exit Game");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void handleCardClick(int cardIndex) {
        if (!game.isPlayersTurn(0)) return;
        AbstractPlayer player = game.getPlayers().get(0);
        AbstractCard card = player.getHand().get(cardIndex);

        boolean playResult;
        if (card.getType() == Types.WILD || card.getType() == Types.DRAW_FOUR) {
            Colors chosenColor = showColorSelectionDialog();
            playResult = chosenColor != null && game.playWildCard(cardIndex, chosenColor);
        } else {
            playResult = game.playCard(cardIndex);
        }

        if (playResult) {
            lastPlayedCard = card;
            if (prev_move_Label != null) prev_move_Label.setText(player.getName() + " PLAYED " + card.toString());
            handleCardNotification(card);

            updateUI();
            game.handleGameRulesAfterTurn();
            updateUI();
            checkGameStatus();
            checkAndStartComputerTurn();
        }
    }

    protected void handleCardNotification(AbstractCard card) {
        switch (card.getType()) {
            case SKIP:
                showNotification("SKIP!", Color.RED);
                break;
            case REVERSE:
                showNotification("REVERSE!", Color.ORANGE);
                updateGameDirectionLabel();
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

    protected void showNotification(String message, Color color) {
        Text notification = new Text(message);
        notification.setFont(Font.font("System", FontWeight.BOLD, 24));
        notification.setFill(color);

        if (notificationArea != null) {
            notificationArea.getChildren().clear();
            notificationArea.getChildren().add(notification);
        }

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), notification);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(1.5));
        fadeOut.play();

        fadeOut.setOnFinished(e -> {
            if (notificationArea != null) notificationArea.getChildren().clear();
        });
    }

    protected void updateGameDirectionLabel() {
        if (direction != null) {
            direction.setText(game.isCustomOrderClockwise()? "↻" : "↺");
        }
    }

    protected Colors showColorSelectionDialog() {
        Dialog<Colors> dialog = new Dialog<>();
        dialog.setTitle("Choose a Color");
        dialog.setHeaderText("Select a color for the Wild card.");

        Button redButton = new Button("Red");
        redButton.setOnAction(event -> {
            dialog.setResult(Colors.RED);
            dialog.close();
        });

        Button blueButton = new Button("Blue");
        blueButton.setOnAction(event -> {
            dialog.setResult(Colors.BLUE);
            dialog.close();
        });

        Button greenButton = new Button("Green");
        greenButton.setOnAction(event -> {
            dialog.setResult(Colors.GREEN);
            dialog.close();
        });

        Button yellowButton = new Button("Yellow");
        yellowButton.setOnAction(event -> {
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
        return result.orElse(null);
    }

    protected void handleDrawCard() {
        if (!game.isPlayersTurn(0)) return;
        game.playerDrawCard(0);
        updateUI();
        game.handleGameRulesAfterTurn();
        updateUI();
        checkGameStatus();
        checkAndStartComputerTurn();
    }

    protected void checkGameStatus() {
        AbstractPlayer winner = game.getWinner();
        if (winner != null) {
            showWinDialog(winner.getName());
            return;
        }
        AbstractPlayer unoPlayer = game.getUnoPlayer();
        if (unoPlayer != null) {
            statusLabel.setText(unoPlayer.getName() + " has UNO!");
            showNotification("UNO!", Color.PURPLE);
        }
    }

    protected abstract void showWinDialog(String winnerName);

    protected void goToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Main Menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateStackingNotification() {
        if (game.isStackingActive() && game.isPlayersTurn(0)) {
            Types stackType = game.getStackType();
            int amount = game.getStackedDrawCards();
            boolean canStack = game.canCurrentPlayerStackDraw();

            if (canStack) {
                String cardName = stackType == Types.DRAW_TWO ? "+2" : "+4";
                String msg = String.format("You can stack a %s card to pass %d cards to the next player!", cardName, amount + (stackType == Types.DRAW_TWO ? 2 : 4));
                showNotification(msg, Color.ORANGE);
            } else {
                String msg = String.format("You must draw %d cards, you cannot stack.", amount);
                showNotification(msg, Color.RED);
            }
        }
    }

    protected abstract void shutdown();

    protected void checkAndStartComputerTurn() {
        // No-op in base; overridden in SinglePlayerController
    }
} 