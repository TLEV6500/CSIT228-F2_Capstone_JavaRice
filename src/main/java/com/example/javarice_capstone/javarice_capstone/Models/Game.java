package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Factory.PlayerFactory;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private final Deck deck;
    private final List<AbstractPlayer> players;
    private int currentPlayerIndex;
    private boolean isClockwise;
    private Colors currentColor;

    public Game(int numPlayers) {
        deck = new Deck();
        players = new ArrayList<>();

        players.add(PlayerFactory.createPlayer("HUMAN", "You"));
        for (int i = 1; i < numPlayers; i++) players.add(PlayerFactory.createPlayer("COMPUTER - S", "Computer " + i));

        for (int i = 0; i < 7; i++) {
            for (AbstractPlayer player : players) {
                player.addCard(deck.drawCard());
            }
        }

        currentPlayerIndex = 0;
        isClockwise = true;

        AbstractCard firstCard = deck.drawCard();
        while (firstCard.getColor() == Colors.WILD) {
            deck.discard(firstCard);
            deck.shuffle();
            firstCard = deck.drawCard();
        }

        deck.discard(firstCard);
        currentColor = firstCard.getColor();
    }

    public boolean isClockwise() {
        return isClockwise;
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
                // For number cards, simply update color and move to next player
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
                nextPlayer(); // Skip next player
                nextPlayer();
                break;

            case REVERSE:
                currentColor = card.getColor();
                isClockwise = !isClockwise;
                if (players.size() == 2) {
                    // In a 2-player game, reverse acts like skip
                    nextPlayer();
                }
                nextPlayer();
                break;

            case DRAW_TWO:
                currentColor = card.getColor();
                nextPlayer();
                // Next player draws 2 cards
                AbstractPlayer nextPlayer = getCurrentPlayer();
                nextPlayer.addCard(deck.drawCard());
                nextPlayer.addCard(deck.drawCard());
                nextPlayer();
                break;

            case WILD:
                // currentColor will be set by the player
                nextPlayer();
                break;

            case DRAW_FOUR:
                // currentColor will be set by the player
                nextPlayer();
                // Next player draws 4 cards
                AbstractPlayer drawFourPlayer = getCurrentPlayer();
                for (int i = 0; i < 4; i++) {
                    drawFourPlayer.addCard(deck.drawCard());
                }
                nextPlayer();
                break;
        }
    }

    public void nextPlayer() {
        if (isClockwise) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
        }
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