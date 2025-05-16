package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Factory.PlayerFactory;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;
import com.example.javarice_capstone.javarice_capstone.enums.ComputerActionResult;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

public class SinglePlayerGame extends Game {
    private final ScheduledExecutorService computerPlayerTimer;
    private boolean isComputerTurnActive = false;
    private ComputerActionResult lastAIAction = null;

    public SinglePlayerGame(int numPlayers) {
        super(numPlayers);
        this.computerPlayerTimer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public boolean playCard(int cardIndex) {
        boolean result = super.playCard(cardIndex);
        if (result) {
            checkAndStartComputerTurn();
        }
        return result;
    }

    @Override
    public void playerDrawCard(int playerIndex) {
        super.playerDrawCard(playerIndex);
        checkAndStartComputerTurn();
    }

    public void checkAndStartComputerTurn() {
        AbstractPlayer currentPlayer = getCurrentPlayer();
        boolean isComputer = currentPlayer instanceof PlayerComputer;
        if (isComputer) {
            isComputerTurnActive = true;
            lastAIAction = null;
            scheduleNextAIStep();
        }
    }

    private void scheduleNextAIStep() {
        if (!isComputerTurnActive) return;

        try {
            computerPlayerTimer.schedule(() -> stepComputerTurn(), 1250, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void stepComputerTurn() {
        if (!isComputerTurnActive) return;

        AbstractPlayer computer = getCurrentPlayer();
        if (!(computer instanceof PlayerComputer)) {
            isComputerTurnActive = false;
            return;
        }

        PlayerComputer pc = (PlayerComputer) computer;
        ComputerActionResult result = pc.stepTurn(this);

        if (result == ComputerActionResult.PLAYED) {
            AbstractCard lastCard = getLastPlayedCard();
            handleGameRulesAfterTurn();
            isComputerTurnActive = false;
            checkAndStartComputerTurn();
        } else if (result == ComputerActionResult.DRAWN) {
            scheduleNextAIStep();
        }
    }

    @Override
    public void handleGameRulesAfterTurn() {
        if (lastPlayedCard != null) {
            switch (lastPlayedCard.getType()) {
                case DRAW_FOUR:
                    // Force next player to draw 4 cards
                    nextPlayer();
                    AbstractPlayer drawFourPlayer = getCurrentPlayer();
                    System.out.println("[DEBUG] WILD_DRAW4: " + drawFourPlayer.getName() + " receives 4 cards");
                    for (int i = 0; i < 4; i++) {
                        drawFourPlayer.addCard(deck.drawCard());
                    }
                    hasHandledForcedDraw = false;  // Reset for next player
                    break;
                case SKIP:
                    currentColor = lastPlayedCard.getColor();
                    if (players.size() == 2) {
                        // In 2-player mode, skip should return to the player who played it
                    } else {
                        nextPlayer();
                        nextPlayer();
                    }
                    break;
                case REVERSE:
                    currentColor = lastPlayedCard.getColor();
                    boolean clockwise = isCustomOrderClockwise();
                    setCustomOrderClockwise(!clockwise);
                    if (players.size() == 2) {
                        nextPlayer();
                    }
                    nextPlayer();
                    break;
                case DRAW_TWO:
                    currentColor = lastPlayedCard.getColor();
                    nextPlayer();
                    AbstractPlayer nextPlayer = getCurrentPlayer();
                    System.out.println("[DEBUG] DRAW_TWO: " + nextPlayer.getName() + " receives 2 cards");
                    nextPlayer.addCard(deck.drawCard());
                    nextPlayer.addCard(deck.drawCard());
                    nextPlayer();
                    break;
                case WILD:
                    nextPlayer();
                    break;
            }
        }
        applyStackDrawRule();
    }

    public void shutdown() {
        isComputerTurnActive = false;
        try {
            computerPlayerTimer.shutdown();
            if (!computerPlayerTimer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                computerPlayerTimer.shutdownNow();
            }
        } catch (InterruptedException e) {
            computerPlayerTimer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    protected void handleSpecialCard(AbstractCard card) {
        switch (card.getType()) {
            case SKIP:
                currentColor = card.getColor();
                if (players.size() == 2) {
                    // In 2-player mode, skip should return to the player who played it
                } else {
                    nextPlayer();
                    nextPlayer();
                }
                break;
            case REVERSE:
                currentColor = card.getColor();
                boolean clockwise = isCustomOrderClockwise();
                setCustomOrderClockwise(!clockwise);
                if (players.size() == 2) {
                    nextPlayer();
                }
                nextPlayer();
                break;
            case DRAW_TWO:
                currentColor = card.getColor();
                nextPlayer();
                AbstractPlayer nextPlayer = getCurrentPlayer();
                nextPlayer.addCard(deck.drawCard());
                nextPlayer.addCard(deck.drawCard());
                nextPlayer();
                break;
            case WILD:
                nextPlayer();
                break;
            case DRAW_FOUR:
                // Only the next player after the card is played receives +4 cards
                nextPlayer();
                AbstractPlayer drawFourPlayer = getCurrentPlayer();
                for (int i = 0; i < 4; i++) drawFourPlayer.addCard(deck.drawCard());
                nextPlayer();
                break;
        }
    }
} 