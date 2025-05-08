package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Factory.PlayerFactory;
import com.example.javarice_capstone.javarice_capstone.Factory.UnlimitedCardFactory;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final Deck deck;
    private final List<AbstractPlayer> players;
    private int currentPlayerIndex;
    private Colors currentColor;

    // Always use this custom order: player (0), opp4 (4), opp1 (1), opp2 (2), opp3 (3), opp5 (5)
    private static final int[] CUSTOM_ORDER = {0, 4, 1, 2, 3, 5};
    private int customTurnIndex = -1;
    private boolean customOrderIsClockwise = true;

    public Game(int numPlayers) {
        deck = new Deck(new UnlimitedCardFactory());
        players = new ArrayList<>();

        players.add(PlayerFactory.createPlayer("HUMAN", "You"));
        for (int i = 1; i < numPlayers; i++) {
            String randomType = PlayerFactory.getRandomComputerType();
            players.add(PlayerFactory.createPlayer(randomType, "Computer " + i));
        }

        for (int i = 0; i < 7; i++) {
            for (AbstractPlayer player : players) {
                player.addCard(deck.drawCard());
            }
        }

        // Set the first player according to the fixed custom order (should be the human player, index 0)
        currentPlayerIndex = getNextValidPlayerIndex(-1, true);
        customTurnIndex = getCustomOrderIndex(currentPlayerIndex);
        // Direction starts as clockwise
        customOrderIsClockwise = true;

        AbstractCard firstCard = deck.drawCard();
        while (firstCard.getColor() == Colors.WILD) {
            deck.discard(firstCard);
            deck.shuffle();
            firstCard = deck.drawCard();
        }

        deck.discard(firstCard);
        currentColor = firstCard.getColor();
    }

    public AbstractPlayer getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public AbstractCard getTopCard() {
        return deck.getTopDiscard();
    }

    public Colors getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(Colors color) {
        this.currentColor = color;
    }

    public boolean playCard(int cardIndex) {
        AbstractPlayer player = getCurrentPlayer();
        AbstractCard card = player.getHand().get(cardIndex);
        AbstractCard topCard = getTopCard();

        if (card.canPlayOn(topCard) || card.getColor() == getCurrentColor()) {
            player.playCard(cardIndex);
            deck.discard(card);

            // Handle special cards
            if (card.getType() != Types.NUMBER) {
                handleSpecialCard(card);
            } else {
                currentColor = card.getColor();
                nextPlayer();
            }

            return true;
        }

        return false;
    }

    /**
     * Draws a card for the current player WITHOUT advancing to the next player.
     * This allows players to draw multiple cards on their turn.
     * @return The drawn card
     */
    public AbstractCard drawCard() {
        AbstractCard card = deck.drawCard();
        getCurrentPlayer().addCard(card);
        return card;
    }

    /**
     * Legacy method that draws a card and advances to the next player.
     * Used by computer players who always draw one card then end turn.
     * @return The drawn card
     */
    public AbstractCard drawCardForPlayer() {
        AbstractCard card = drawCard();
        nextPlayer();
        return card;
    }

    private void handleSpecialCard(AbstractCard card) {
        switch (card.getType()) {
            case SKIP:
                currentColor = card.getColor();
                nextPlayer();
                nextPlayer();
                break;

            case REVERSE:
                currentColor = card.getColor();
                customOrderIsClockwise = !customOrderIsClockwise; // Toggle direction
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
                nextPlayer();
                AbstractPlayer drawFourPlayer = getCurrentPlayer();
                for (int i = 0; i < 4; i++) {
                    drawFourPlayer.addCard(deck.drawCard());
                }
                nextPlayer();
                break;
        }
    }

    public void nextPlayer() {
        currentPlayerIndex = getNextValidPlayerIndex(customTurnIndex, customOrderIsClockwise);
        customTurnIndex = getCustomOrderIndex(currentPlayerIndex);
    }

    private int getNextValidPlayerIndex(int prevCustomTurnIndex, boolean isClockwise) {
        int tries = 0;
        int totalPlayers = players.size();
        int idx = prevCustomTurnIndex;
        while (tries < CUSTOM_ORDER.length) {

            if (isClockwise) idx = (idx + 1) % CUSTOM_ORDER.length;
            else idx = (idx - 1 + CUSTOM_ORDER.length) % CUSTOM_ORDER.length;

            int candidate = CUSTOM_ORDER[idx];
            if (candidate < totalPlayers && players.get(candidate) != null) return candidate;
            tries++;
        }
        return 0;
    }

    private int getCustomOrderIndex(int playerIdx) {
        for (int i = 0; i < CUSTOM_ORDER.length; i++) {
            if (CUSTOM_ORDER[i] == playerIdx) return i;
        }
        return 0;
    }

    public boolean isGameOver() {
        for (AbstractPlayer player : players) {
            if (player.hasWon()) return true;
        }
        return false;
    }

    public AbstractPlayer getWinner() {
        for (AbstractPlayer player : players) {
            if (player.hasWon()) return player;
        }
        return null;
    }

    public List<AbstractPlayer> getPlayers() {
        return players;
    }
}