package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Factory.PlayerFactory;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private final Deck deck;
    private final List<AbstractPlayer> players;
    private int currentPlayerIndex;
    private Colors currentColor;

    private static final int[] CUSTOM_ORDER = {0, 4, 1, 2, 3, 5};
    private int customTurnIndex = -1;
    private boolean customOrderIsClockwise = true;

    private AbstractCard lastPlayedCard = null;

    public Game(int numPlayers) {
        deck = new Deck();
        players = new ArrayList<>();

        players.add(PlayerFactory.createPlayer("HUMAN", "You"));
        for (int i = 1; i < numPlayers; i++) {
            String randomType = PlayerFactory.getRandomComputerType();
            players.add(PlayerFactory.createPlayer(randomType, "Computer " + i));
        }

        for (int i = 0; i < 1; i++) {
            for (AbstractPlayer player : players) {
                player.addCard(deck.drawCard());
            }
        }

        currentPlayerIndex = getNextValidPlayerIndex(-1, true);
        customTurnIndex = getCustomOrderIndex(currentPlayerIndex);
        customOrderIsClockwise = true;

        AbstractCard firstCard = deck.drawCard();
        while (firstCard.getType() != Types.NUMBER) {
            deck.discard(firstCard);
            deck.shuffle();
            firstCard = deck.drawCard();
        }
        deck.discard(firstCard);
        currentColor = firstCard.getColor();
        lastPlayedCard = firstCard;
    }

    public AbstractPlayer getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public AbstractCard getTopCard() {
        return deck.getTopDiscard();
    }

    public AbstractCard getLastPlayedCard() {
        return lastPlayedCard;
    }

    public Colors getCurrentColor() {
        return currentColor;
    }

    public boolean isCustomOrderClockwise() {
        return customOrderIsClockwise;
    }

    public List<AbstractPlayer> getPlayers() {
        return players;
    }

    public AbstractPlayer getWinner() {
        for (AbstractPlayer player : players) {
            if (player.hasWon()) return player;
        }
        return null;
    }

    public AbstractPlayer getUnoPlayer() {
        for (AbstractPlayer player : players) {
            if (player.hasUno()) return player;
        }
        return null;
    }

    public void setCurrentColor(Colors color) {
        this.currentColor = color;
    }

    public boolean playCard(int cardIndex) {
        AbstractPlayer player = getCurrentPlayer();
        AbstractCard card = player.getHand().get(cardIndex);
        AbstractCard topCard = getTopCard();

        if (card.canPlayOn(topCard) || card.getColor() == getCurrentColor()) {
            AbstractCard playedCard = player.playCard(cardIndex);
            deck.discard(playedCard);
            lastPlayedCard = playedCard;

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

    public AbstractCard drawCard() {
        AbstractCard card = deck.drawCard();
        getCurrentPlayer().addCard(card);
        return card;
    }

    public void handleWildColorSelection(Colors color) {
        setCurrentColor(color);
    }

    public void handleSevenSwap(int targetPlayerIndex) {
        AbstractPlayer currentPlayer = getCurrentPlayer();
        AbstractPlayer targetPlayer = players.get(targetPlayerIndex);
        List<AbstractCard> temp = new ArrayList<>(currentPlayer.getHand());
        replaceHand(currentPlayer, targetPlayer.getHand());
        replaceHand(targetPlayer, temp);
    }

    private void replaceHand(AbstractPlayer player, List<AbstractCard> newHand) {
        player.getHand().clear();
        player.getHand().addAll(newHand);
    }

    public void nextPlayer() {
        currentPlayerIndex = getNextValidPlayerIndex(customTurnIndex, customOrderIsClockwise);
        customTurnIndex = getCustomOrderIndex(currentPlayerIndex);
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
                customOrderIsClockwise = !customOrderIsClockwise;
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
                for (int i = 0; i < 4; i++) drawFourPlayer.addCard(deck.drawCard());
                nextPlayer();
                break;
        }
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

    public static Colors getRandomColorExcept(Colors exclude) {
        List<Colors> possibleColors = new ArrayList<>();
        for (Colors color : Colors.values())
            if (color != Colors.WILD && color != exclude) possibleColors.add(color);
        int idx = new Random().nextInt(possibleColors.size());
        return possibleColors.get(idx);
    }

    public String getActionDescription(AbstractCard card) {
        String actionText = "Played " + card.toString();
        switch (card.getType()) {
            case SKIP: actionText += " - Player skipped!"; break;
            case REVERSE: actionText += " - Direction reversed!"; break;
            case DRAW_TWO: actionText += " - Next player draws 2 cards!"; break;
            case DRAW_FOUR: actionText += " - Next player draws 4 cards!"; break;
            default: break;
        }
        return actionText;
    }
}