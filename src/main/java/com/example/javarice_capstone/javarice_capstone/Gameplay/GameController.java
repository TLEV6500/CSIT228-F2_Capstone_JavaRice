package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Factory.GameSetupDialogController;
import com.example.javarice_capstone.javarice_capstone.Models.*;
import com.example.javarice_capstone.javarice_capstone.enums.*;
import com.example.javarice_capstone.javarice_capstone.Abstracts.*;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Collectors;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.PlayerInfo;

public class GameController implements Initializable {

    @FXML private HBox playerHand;
    @FXML private Label MainPlayerHandCount;
    @FXML private ImageView discardPileView;
    @FXML private ImageView drawPileView;
    @FXML private Label statusLabel;
    @FXML private StackPane notificationArea;
    @FXML private BorderPane rootBorderPane;
    @FXML private Button exitButton;

    @FXML private HBox opponent1Hand, opponent2Hand, opponent3Hand;
    @FXML private VBox  opponent4Hand, opponent5Hand;
    @FXML private Label opponent1Name, opponent2Name, opponent3Name, opponent4Name, opponent5Name;
    @FXML private Label opponent1HandCount, opponent2HandCount, opponent3HandCount, opponent4HandCount, opponent5HandCount;

    @FXML private Label direction;
    @FXML private Label prev_move_Label;
    @FXML private Label MainPlayerLabel;

    private Game game;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService multiplayerPollingTimer = Executors.newSingleThreadScheduledExecutor();
    private boolean isFirstTurn = true;
    private boolean isSingleplayer = true;
    private boolean isShuttingDown = false;

    private static final int MAX_RENDERED_COMPUTER_CARDS = 20;
    private static final double PLAYER_CARD_WIDTH = 65;
    private static final double PLAYER_CARD_HEIGHT = 105;
    private static final double OPPONENT_CARD_WIDTH = 45;
    private static final double OPPONENT_CARD_HEIGHT = 70;
    private static final double COMPUTER_CARD_OVERLAP = 20;
    private static final double PLAYER_CARD_OVERLAP = 15;
    private static final int COMPUTER_OVERLAP_EXPAND_CARD_STEP = 2;
    private static final double COMPUTER_OVERLAP_EXPAND_AMOUNT = 5;

    private AbstractCard lastPlayedCard = null;
    private boolean isComputerTurnActive = false;
    private ComputerActionResult lastAIAction = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Do NOT create the game here!
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

        // Only set up listeners here, do not call updateUI or checkAndStartComputerTurn
        drawPileView.setOnMouseClicked(e -> handleDrawCard());
    }

    private void startMultiplayerPolling() {
        if (game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) {
            String lobbyCode = ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).getLobbyCode();
            multiplayerPollingTimer.scheduleAtFixedRate(() -> {
                if (isShuttingDown) {
                    multiplayerPollingTimer.shutdown();
                    return;
                }
                Platform.runLater(() -> {
                    ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).updateGameState();
                    updateUI();
                });
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    public void setGame(Game game) {
        this.game = game;
        if (game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) {
            isSingleplayer = false;
            startMultiplayerPolling();
        }
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

    private void checkTurnAndUpdateColors() {
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

    public void updateUI() {
        System.out.println("[UI] updateUI called. Top card: " + game.getTopCard());
        playerHand.getChildren().clear();
        AbstractPlayer humanPlayer = game.getPlayers().get(0);
        playerHand.setAlignment(Pos.CENTER);

        updatePlayerHandCount(humanPlayer);
        renderPlayerHand(humanPlayer);

        clearOpponentHands();
        Image cardBack = loadCardBackImage();

        // Multiplayer: fetch and update opponent hand sizes from DB
        if (game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) {
            String lobbyCode = ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).getLobbyCode();
            java.util.List<com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.PlayerInfo> playerInfos =
                com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getPlayersInLobby(lobbyCode);
            
            // Update hand sizes for all players (including local player) from the database
            for (int i = 0; i < game.getPlayers().size(); i++) {
                if (i == 0) continue; // Local player: keep real cards
                AbstractPlayer player = game.getPlayers().get(i);
                // Find the matching PlayerInfo by name
                PlayerInfo info = playerInfos.stream()
                    .filter(pi -> pi.name.equals(player.getName()))
                    .findFirst()
                    .orElse(null);
                if (info != null) {
                    player.getHand().clear();
                    for (int j = 0; j < info.handSize; j++) {
                        player.getHand().add(null); // Placeholder for card
                    }
                }
            }

            // Check for recent game moves
            String lastMove = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getLastGameMove(lobbyCode);
            if (lastMove != null) {
                if (lastMove.equals("draw")) {
                    // Get the player who drew the card
                    List<com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.MoveInfo> moves = 
                        com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getGameMoves(lobbyCode);
                    if (!moves.isEmpty()) {
                        com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.MoveInfo lastMoveInfo = moves.get(moves.size() - 1);
                        String playerWhoDrew = lastMoveInfo.playerName;
                        
                        // Show draw notification with player name
                        Platform.runLater(() -> {
                            showNotification(playerWhoDrew + " drew a card!", Color.BLUE);
                        });
                    }
                }
            }

            // Force update discard pile from database
            String dbTopCard = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.fetchDiscardPile(lobbyCode);
            if (dbTopCard != null) {
                System.out.println("[POLL] Updating discard pile from DB: " + dbTopCard);
                
                // Handle special card effects
                if (dbTopCard.equals("wild") || dbTopCard.equals("wild_draw4")) {
                    // For wild cards, we need to get the chosen color from the game state
                    Colors currentColor = game.getCurrentColor();
                    if (currentColor != Colors.WILD) {
                        String colorPrefix = currentColor.name().toLowerCase();
                        if (dbTopCard.equals("wild")) {
                            dbTopCard = colorPrefix + "_wild";
                        } else {
                            dbTopCard = colorPrefix + "_draw4";
                        }
                    }
                }
                
                Colors dbColor = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getCurrentColor(lobbyCode);
                game.setCurrentColor(dbColor);
                game.updateDiscardPile(dbTopCard);
                
                // Handle special card effects
                if (dbTopCard.endsWith("_reverse")) {
                    // Get the game direction from database
                    boolean isClockwise = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getGameDirection(lobbyCode);
                    
                    // Only update if the direction has changed
                    if (game.isCustomOrderClockwise() != isClockwise) {
                        game.setCustomOrderClockwise(isClockwise);
                        Platform.runLater(() -> {
                            updateGameDirectionLabel();
                            showNotification("REVERSE!", Color.ORANGE);
                        });
                    }
                    
                    // Update previous move label
                    if (prev_move_Label != null) {
                        String playerName = "";
                        List<com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.MoveInfo> moves = 
                            com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getGameMoves(lobbyCode);
                        if (!moves.isEmpty()) {
                            playerName = moves.get(moves.size() - 1).playerName;
                        }
                        prev_move_Label.setText("PREV MOVE: " + playerName + " played a REVERSE card");
                    }
                } else if (dbTopCard.endsWith("_skip")) {
                    Platform.runLater(() -> {
                        showNotification("SKIP!", Color.RED);
                    });
                } else if (dbTopCard.endsWith("_draw4")) {
                    Platform.runLater(() -> {
                        showNotification("+4 CARDS", Color.DARKRED);
                    });
                }
            }

            // Check if turn has changed
            String dbCurrentPlayer = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getCurrentPlayer(lobbyCode);
            if (dbCurrentPlayer != null) {
                System.out.println("[UI] Current player from DB: " + dbCurrentPlayer);
                if (!dbCurrentPlayer.equals(game.getCurrentPlayer().getName())) {
                    System.out.println("[UI] Turn changed to: " + dbCurrentPlayer);
                    game.setCurrentPlayer(dbCurrentPlayer);
                    // Force UI update after turn change
                    Platform.runLater(() -> {
                        updateUI();
                    });
                }
            }
        }

        updateOpponentHands(cardBack);
        updateDiscardAndDrawPiles();
        updateWildCardColor();

        checkTurnAndUpdateColors();
        Platform.runLater(() -> {
        updateGameDirectionLabel();
        });
        updateStackingNotification();

        if (prev_move_Label != null && lastPlayedCard != null) {
            prev_move_Label.setText("PREV MOVE: " + game.getActionDescription(lastPlayedCard));
        }
    }

    private void updatePlayerHandCount(AbstractPlayer player) {
        if (MainPlayerHandCount != null) MainPlayerHandCount.setText("(" + player.getHand().size() + " cards)");
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
            if (computerCount >= i + 1) ((Runnable)opponents[i][3]).run();
            else clearOpponentHandUI((Label)opponents[i][0], (javafx.scene.layout.Pane)opponents[i][1], (Label)opponents[i][2]);
        }
    }

    private void updateDiscardAndDrawPiles() {
        AbstractCard topCard = game.getTopCard();
        System.out.println("[UI] Updating discard pile view with card: " + topCard);
        try {
            if (topCard != null) {
                String imagePath = topCard.getImagePath();
                System.out.println("[UI] Loading image from path: " + imagePath);
                Image cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                discardPileView.setImage(cardImage);
            }
            drawPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/cards/card_back.png"))));
        } catch (Exception e) {
            System.err.println("[UI] Error updating discard pile view: " + e.getMessage());
            e.printStackTrace();
        }
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

    private void setOpponentHandVBox(int index, Label nameLabel, VBox handBox, Label handCountLabel, Image cardBack) {
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
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @FXML
    private void handleExit() {
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

    private void handleCardClick(int cardIndex) {
        if (!game.isPlayersTurn(0)) return;
        AbstractPlayer player = game.getPlayers().get(0);
        AbstractCard card = player.getHand().get(cardIndex);

        boolean playResult;
        Colors chosenColor = null;
        if (card.getType() == Types.WILD || card.getType() == Types.DRAW_FOUR) {
            chosenColor = showColorSelectionDialog();
            playResult = chosenColor != null && game.playWildCard(cardIndex, chosenColor);
        } else {
            playResult = game.playCard(cardIndex);
        }

        if (playResult) {
            lastPlayedCard = card;
            if (prev_move_Label != null) prev_move_Label.setText(player.getName() + " PLAYED " + card.toString());
            handleCardNotification(card);

            // --- Update discard pile in DB after a successful play ---
            if (game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) {
                String lobbyCode = ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).getLobbyCode();
                AbstractCard playedCard = game.getLastPlayedCard();
                String discardPileCard;
                String action = "play";
                
                // Format the card info based on its type
                if (playedCard.getType() == Types.REVERSE) {
                    discardPileCard = playedCard.getColor() + "_reverse";
                    action = "reverse";
                    // Update game direction in database
                    boolean newDirection = !game.isCustomOrderClockwise();
                    game.setCustomOrderClockwise(newDirection);
                    com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.updateGameDirection(lobbyCode, newDirection);
                } else if (playedCard.getType() == Types.SKIP) {
                    discardPileCard = playedCard.getColor() + "_skip";
                    action = "skip";
                } else if (playedCard.getType() == Types.DRAW_TWO) {
                    discardPileCard = playedCard.getColor() + "_draw2";
                    action = "draw2";
                } else if (playedCard.getType() == Types.WILD) {
                    discardPileCard = chosenColor.toString().toLowerCase() + "_wild";
                    action = "wild";
                } else if (playedCard.getType() == Types.DRAW_FOUR) {
                    discardPileCard = chosenColor.toString().toLowerCase() + "_wild_four";
                    action = "draw4";
                } else {
                    discardPileCard = playedCard.getColor() + "_" + playedCard.getValue();
                }
                
                // Update player's hand size immediately after playing
                com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.updatePlayerHandSize(
                    lobbyCode,
                    player.getName(),
                    player.getHand().size()
                );
                
                // Record the game move
                com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.recordGameMove(
                    lobbyCode,
                    player.getName(),
                    discardPileCard,
                    action,
                    ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).getTurnNumber() + 1
                );
                
                // Push to database and verify
                boolean pushSuccess = false;
                int retryCount = 0;
                while (!pushSuccess && retryCount < 3) {
                    com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.pushDiscardPile(lobbyCode, discardPileCard);
                    
                    // Verify the card was pushed successfully
                    try {
                        Thread.sleep(500); // Wait for database update
                        String pushedCard = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.fetchDiscardPile(lobbyCode);
                        pushSuccess = discardPileCard.equals(pushedCard);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    if (!pushSuccess) {
                        retryCount++;
                    }
                }
                
                if (!pushSuccess) {
                    System.err.println("Failed to update discard pile in database after 3 attempts");
                }
            }
            // --------------------------------------------------------

            updateUI();
            if (!(game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame)) {
            game.handleGameRulesAfterTurn();
            }
            checkGameStatus();
            checkAndStartComputerTurn();
        }
    }

    private void handleCardNotification(AbstractCard card) {
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

    private void showNotification(String message, Color color) {
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

    private void updateGameDirectionLabel() {
        if (direction != null) {
            direction.setText(game.isCustomOrderClockwise()? "↻" : "↺");
        }
    }

    private Colors showColorSelectionDialog() {
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

    private void handleDrawCard() {
        if (!game.isPlayersTurn(0)) return;
        // Draw the card
        AbstractCard drawnCard = game.drawCard();
        if (drawnCard != null) {
            game.getCurrentPlayer().addCard(drawnCard);
            // For multiplayer, ensure the draw is recorded and synchronized
            if (game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) {
                String lobbyCode = ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).getLobbyCode();
                
                // Update player's hand size immediately after drawing
                com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.updatePlayerHandSize(
                    lobbyCode, 
                    game.getCurrentPlayer().getName(), 
                    game.getCurrentPlayer().getHand().size()
                );
                
                // Record the draw action
                com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.recordGameMove(
                    lobbyCode,
                    game.getCurrentPlayer().getName(),
                    "",
                    "draw",
                    ((com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame) game).getTurnNumber() + 1
                );
                
                // Force UI update to reflect the changes
                Platform.runLater(() -> {
        updateUI();
                });
            }
        }
        if (!(game instanceof com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame)) {
        game.handleGameRulesAfterTurn();
        }
        checkGameStatus();
        checkAndStartComputerTurn();
    }

    private void checkGameStatus() {
        AbstractPlayer winner = game.getWinner();
        if (winner != null) {
            if (isSingleplayer) {
                showSingleplayerWinDialog(winner.getName());
            } else {
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
                    updateGameDirectionLabel();
                }
            }
            return;
        }
        AbstractPlayer unoPlayer = game.getUnoPlayer();
        if (unoPlayer != null) {
            statusLabel.setText(unoPlayer.getName() + " has UNO!");
            showNotification("UNO!", Color.PURPLE);
        }
    }

    private void showSingleplayerWinDialog(String winnerName) {
        try {
            isShuttingDown = true;
            isComputerTurnActive = false;

            shutdown();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/SetupDialog.fxml"));
            Parent dialogRoot = loader.load();

            GameSetupDialogController dialogController = loader.getController();
            dialogController.setCustomWinModeMainMenuOnly(winnerName + " has won the Game!", this::goToMainMenu);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Game Over");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            goToMainMenu();
        }
    }

    private void goToMainMenu() {
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

    private void updateStackingNotification() {
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

    protected void shutdown() {
        isShuttingDown = true;
        isComputerTurnActive = false;

        try {
            computerPlayerTimer.shutdown();
            multiplayerPollingTimer.shutdown();
            if (!computerPlayerTimer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                computerPlayerTimer.shutdownNow();
            }
            if (!multiplayerPollingTimer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                multiplayerPollingTimer.shutdownNow();
            }
        } catch (InterruptedException e) {
            computerPlayerTimer.shutdownNow();
            multiplayerPollingTimer.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    private void checkAndStartComputerTurn() {
        if (isShuttingDown) return;

        AbstractPlayer currentPlayer = game.getCurrentPlayer();
        boolean isComputer = currentPlayer instanceof PlayerComputer;
        if (isComputer) {
            Platform.runLater(() -> {
                statusLabel.setText("TURN: " + currentPlayer.getName());
            });

            isComputerTurnActive = true;
            lastAIAction = null;

            try {
                scheduleNextAIStep();
            } catch (RejectedExecutionException ignored) {
            }
        }
    }

    private void scheduleNextAIStep() {
        if (!isComputerTurnActive || isShuttingDown) return;

        try {
            computerPlayerTimer.schedule(() -> Platform.runLater(this::stepComputerTurn), 1250, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void stepComputerTurn() {
        if (!isComputerTurnActive || isShuttingDown) return;

        AbstractPlayer computer = game.getCurrentPlayer();
        if (!(computer instanceof PlayerComputer)) {
            isComputerTurnActive = false;
            return;
        }

        PlayerComputer pc = (PlayerComputer) computer;
        ComputerActionResult result = pc.stepTurn(game);

        if (result == ComputerActionResult.PLAYED) {
            AbstractCard lastCard = game.getLastPlayedCard();
            lastPlayedCard = lastCard;
            handleCardNotification(lastCard);

            updateUI();
            game.handleGameRulesAfterTurn();
            checkGameStatus();
            isComputerTurnActive = false;
            if (!isShuttingDown) checkAndStartComputerTurn();
        } else if (result == ComputerActionResult.DRAWN && !isShuttingDown) {
            try {
                scheduleNextAIStep();
            } catch (RejectedExecutionException ignored) {
            }
        }
    }
}