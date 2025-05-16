package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Models.Game;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerComputer;
import com.example.javarice_capstone.javarice_capstone.enums.ComputerActionResult;
import com.example.javarice_capstone.javarice_capstone.enums.Types;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.Factory.GameSetupDialogController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SinglePlayerController extends BaseGameController {
    private Game gameInstance;
    private final ScheduledExecutorService computerPlayerTimer = Executors.newSingleThreadScheduledExecutor();
    private boolean isComputerTurnActive = false;
    private ComputerActionResult lastAIAction = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Do not create the game here!
        super.initialize(location, resources);
    }

    @Override
    public void startGame(int numPlayers, List<String> playerNames) {
        gameInstance = new Game(numPlayers);
        setGame(gameInstance);
        // Set player names
        for (int i = 0; i < Math.min(numPlayers, playerNames.size()); i++) {
            gameInstance.getPlayers().get(i).setName(playerNames.get(i));
        }
        isFirstTurn = true;
        updateUI();
        updateGameDirectionLabel();
        checkAndStartComputerTurn();
    }

    @Override
    protected void showWinDialog(String winnerName) {
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

    @Override
    protected void shutdown() {
        isShuttingDown = true;
        isComputerTurnActive = false;
        try {
            computerPlayerTimer.shutdown();
            if (!computerPlayerTimer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                computerPlayerTimer.shutdownNow();
            }
        } catch (InterruptedException e) {
            computerPlayerTimer.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {}
    }

    @Override
    protected void checkAndStartComputerTurn() {
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
            } catch (RejectedExecutionException ignored) {}
        }
    }

    private void scheduleNextAIStep() {
        if (!isComputerTurnActive || isShuttingDown) return;
        try {
            computerPlayerTimer.schedule(() -> Platform.runLater(this::stepComputerTurn), 1250, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {}
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
            updateUI();
            checkGameStatus();
            isComputerTurnActive = false;
            if (!isShuttingDown) checkAndStartComputerTurn();
        } else if (result == ComputerActionResult.DRAWN && !isShuttingDown) {
            updateUI();
            try {
                scheduleNextAIStep();
            } catch (RejectedExecutionException ignored) {}
        }
    }
} 