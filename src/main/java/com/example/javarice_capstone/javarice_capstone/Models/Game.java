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
    protected final Deck deck;
    protected final List<AbstractPlayer> players;
    protected int currentPlayerIndex;
    protected Colors currentColor;

    private static final int[] CUSTOM_ORDER = {0, 4, 1, 2, 3, 5};
    private int customTurnIndex = -1;
    private boolean customOrderIsClockwise = true;

    protected AbstractCard lastPlayedCard = null;
    protected boolean hasHandledForcedDraw = false;  // Track if current player has handled forced draw
    protected int lastDraw4Turn = -1;  // Track when the last WILD DRAW FOUR was played
    protected boolean isForcedDrawTurn = false;  // Track if current turn is a forced draw turn
    protected int stackedDrawCards = 0;  // Track number of cards to draw from stacking
    protected boolean isDrawTwoStacking = false;  // Track if we're in a DRAW TWO stacking sequence
    protected int lastDrawTwoPlayer = -1;  // Track who played the last DRAW TWO
    protected int drawTwoStackCount = 0;  // Track how many DRAW TWO cards have been stacked
    protected boolean hasHandledGameRules = false;  // Track if we've handled game rules for this turn
    protected int lastHandledTurn = -1;  // Track which turn we last handled rules for
    protected boolean isHandlingRules = false;  // Track if we're currently handling rules
    protected boolean hasAppliedCardEffect = false;  // Track if we've applied the card's effect

    public Game(int numPlayers) {
        deck = new Deck();
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

    public boolean isPlayersTurn(int playerIndex) {
        return getCurrentPlayer() == getPlayers().get(playerIndex);
    }

    public boolean playWildCard(int cardIndex, Colors chosenColor) {
        handleWildColorSelection(chosenColor);
        boolean result = playCard(cardIndex);
        if (result) {
            AbstractPlayer player = getCurrentPlayer();
            System.out.println(player.getName() + ":\tCHANGED COLOR TO --> " + chosenColor);
        }
        return result;
    }

    public void playerDrawCard(int playerIndex) {
        if (playerIndex == currentPlayerIndex) {
            AbstractCard drawnCard = deck.drawCard();
            getCurrentPlayer().addCard(drawnCard);
            System.out.println("[DEBUG] Player " + getCurrentPlayer().getName() + " drew a card: " + drawnCard.toString());
            System.out.println("[DEBUG] Player " + getCurrentPlayer().getName() + " now has " + getCurrentPlayer().getHand().size() + " cards in hand");
            hasHandledForcedDraw = true;  // Mark that the draw has been handled
            
            // Only move to next player if this was a forced draw from WILD DRAW FOUR
            if (lastPlayedCard != null && lastPlayedCard.getType() == Types.DRAW_FOUR && !isForcedDrawTurn) {
                nextPlayer();
            }
        }
    }

    public Colors getCurrentColor() {
        return currentColor;
    }

    public boolean isCustomOrderClockwise() {
        return customOrderIsClockwise;
    }

    public void setCustomOrderClockwise(boolean clockwise) {
        this.customOrderIsClockwise = clockwise;
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
        // If this is a forced draw turn, player must draw first
        if (isForcedDrawTurn && !hasHandledForcedDraw) {
            System.out.println("[DEBUG] Must draw cards before playing");
            System.out.println("[DEBUG] Player " + getCurrentPlayer().getName() + " has " + getCurrentPlayer().getHand().size() + " cards in hand");
            return false;
        }

        AbstractPlayer player = getCurrentPlayer();
        if (cardIndex < 0 || cardIndex >= player.getHand().size()) {
            return false;
        }
        AbstractCard topCard = getTopCard();
        if (topCard == null) {
            return false;
        }
        AbstractCard cardToPlay = player.getHand().get(cardIndex);
        
        // Allow WILD DRAW FOUR to be played anytime
        if (cardToPlay.getType() == Types.DRAW_FOUR) {
            lastDraw4Turn = getTurnNumber();
        }

        if (cardToPlay.canPlayOn(topCard) || cardToPlay.getColor() == getCurrentColor() || cardToPlay.getType() == Types.DRAW_FOUR) {
            AbstractCard playedCard = player.playCard(cardIndex);
            if (playedCard == null) {
                return false;
            }
            deck.discard(playedCard);
            lastPlayedCard = playedCard;
            System.out.println(player.getName() + ":\tPLAYED --> " + playedCard.toString());
            
            // Reset handling flags for the new card
            hasHandledGameRules = false;
            lastHandledTurn = -1;
            isHandlingRules = false;
            hasAppliedCardEffect = false;

            if (cardToPlay.getType() == Types.DRAW_FOUR) {
                // Handle WILD DRAW FOUR immediately
                nextPlayer();
                AbstractPlayer drawFourPlayer = getCurrentPlayer();
                if (!hasAppliedCardEffect) {
                    System.out.println("[DEBUG] DRAW_FOUR: " + drawFourPlayer.getName() + " receives 4 cards");
                    for (int i = 0; i < 4; i++) {
                        drawFourPlayer.addCard(deck.drawCard());
                    }
                    System.out.println("[DEBUG] " + drawFourPlayer.getName() + " now has " + drawFourPlayer.getHand().size() + " cards in hand");
                    hasAppliedCardEffect = true;
                }
                isForcedDrawTurn = true;
                hasHandledForcedDraw = true;
                nextPlayer();
            } else if (cardToPlay.getType() != Types.NUMBER) {
                handleGameRulesAfterTurn();
            } else {
                currentColor = cardToPlay.getColor();
                nextPlayer();
            }
            return true;
        }
        return false;
    }

    public AbstractCard drawCard() {
        return deck.drawCard();
    }

    public void handleWildColorSelection(Colors color) {
        setCurrentColor(color);
    }

    public void nextPlayer() {
        currentPlayerIndex = getNextValidPlayerIndex(customTurnIndex, customOrderIsClockwise);
        customTurnIndex = getCustomOrderIndex(currentPlayerIndex);
        // Reset flags for the new player's turn
        hasHandledForcedDraw = false;
        isForcedDrawTurn = false;
        hasHandledGameRules = false;  // Reset game rules handling flag
        lastHandledTurn = -1;  // Reset the last handled turn
        isHandlingRules = false;  // Reset the handling flag
        hasAppliedCardEffect = false;  // Reset the card effect flag
    }

    public void handleGameRulesAfterTurn() {
        if (lastPlayedCard == null || isHandlingRules) {
            return;
        }

        int currentTurn = getTurnNumber();
        if (hasHandledGameRules && lastHandledTurn == currentTurn) {
            System.out.println("[DEBUG] Already handled rules for turn " + currentTurn);
            return;
        }

        System.out.println("[DEBUG] Handling game rules for card: " + lastPlayedCard.toString());
        isHandlingRules = true;  // Set flag to prevent recursive handling
        hasHandledGameRules = true;
        lastHandledTurn = currentTurn;

        try {
            switch (lastPlayedCard.getType()) {
                case SKIP:
                    if (!hasAppliedCardEffect) {
                        currentColor = lastPlayedCard.getColor();
                        if (players.size() == 2) {
                            // In 2-player mode, skip should return to the player who played it
                        } else {
                            nextPlayer();
                            nextPlayer();
                        }
                        hasAppliedCardEffect = true;
                    }
                    break;
                case REVERSE:
                    if (!hasAppliedCardEffect) {
                        currentColor = lastPlayedCard.getColor();
                        boolean clockwise = isCustomOrderClockwise();
                        setCustomOrderClockwise(!clockwise);
                        if (players.size() == 2) {
                            nextPlayer();
                        }
                        nextPlayer();
                        hasAppliedCardEffect = true;
                    }
                    break;
                case DRAW_TWO:
                    if (!hasAppliedCardEffect) {
                        currentColor = lastPlayedCard.getColor();
                        System.out.println("[DEBUG] DRAW TWO played by " + getCurrentPlayer().getName());
                        
                        // If we're already in a stacking sequence, add to it
                        if (isDrawTwoStacking) {
                            System.out.println("[DEBUG] Continuing DRAW TWO stack - adding 2 more cards");
                            stackedDrawCards += 2;
                            drawTwoStackCount++;
                        } else {
                            // Start a new stacking sequence
                            System.out.println("[DEBUG] Starting new DRAW TWO stack sequence");
                            stackedDrawCards = 2;
                            isDrawTwoStacking = true;
                            lastDrawTwoPlayer = currentPlayerIndex;
                            drawTwoStackCount = 1;
                        }

                        // Move to next player and make them draw
                        nextPlayer();
                        AbstractPlayer nextPlayer = getCurrentPlayer();
                        System.out.println("[DEBUG] DRAW_TWO: " + nextPlayer.getName() + " receives 2 cards");
                        nextPlayer.addCard(deck.drawCard());
                        nextPlayer.addCard(deck.drawCard());
                        System.out.println("[DEBUG] " + nextPlayer.getName() + " now has " + nextPlayer.getHand().size() + " cards in hand");
                        hasHandledForcedDraw = true;

                        // Check if next player can stack
                        boolean canStack = false;
                        for (AbstractCard card : nextPlayer.getHand()) {
                            if (card != null && card.getType() == Types.DRAW_TWO) {
                                canStack = true;
                                break;
                            }
                        }

                        if (!canStack) {
                            System.out.println("[DEBUG] Next player cannot stack - ending sequence");
                            resetDrawTwoStacking();
                            nextPlayer();
                        }
                        hasAppliedCardEffect = true;
                    }
                    break;
                case WILD:
                    if (!hasAppliedCardEffect) {
                        nextPlayer();
                        hasAppliedCardEffect = true;
                    }
                    break;
            }
        } finally {
            isHandlingRules = false;  // Always reset the handling flag
        }
    }

    public boolean isStackingActive() {
        AbstractCard topCard = getTopCard();
        return (topCard.getType() == Types.DRAW_TWO || topCard.getType() == Types.DRAW_FOUR) && stackedDrawCards > 0;
    }

    public int getStackedDrawCards() {
        return stackedDrawCards;
    }

    public Types getStackType() {
        AbstractCard topCard = getTopCard();
        return topCard.getType();
    }

    protected void applyStackDrawRule() {
        AbstractCard topCard = getTopCard();
        System.out.println("[DEBUG] Checking stack rule - Top card: " + (topCard != null ? topCard.toString() : "null"));
        
        // Only handle DRAW TWO stacking if we're not already in a sequence
        if (topCard != null && topCard.getType() == Types.DRAW_TWO && !isDrawTwoStacking) {
            System.out.println("[DEBUG] Found DRAW TWO on top but not in sequence - resetting");
            resetDrawTwoStacking();
        }
    }

    private void resetDrawTwoStacking() {
        System.out.println("[DEBUG] Resetting stack state - Previous state: Cards=" + stackedDrawCards + 
            ", Stacking=" + isDrawTwoStacking + ", Last player=" + lastDrawTwoPlayer + ", Count=" + drawTwoStackCount);
        stackedDrawCards = 0;
        isDrawTwoStacking = false;
        lastDrawTwoPlayer = -1;
        drawTwoStackCount = 0;
        System.out.println("[DEBUG] Stack state reset complete");
    }

    public boolean canCurrentPlayerStackDraw() {
        AbstractCard topCard = getTopCard();
        if (topCard.getType() == Types.DRAW_TWO || topCard.getType() == Types.DRAW_FOUR) {
            AbstractPlayer player = getCurrentPlayer();
            for (AbstractCard card : player.getHand()) {
                if (card.getType() == topCard.getType()) {
                    return true;
                }
            }
        }
        return false;
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

    public void updateDiscardPile(String cardInfo) {
        if (cardInfo == null || cardInfo.isEmpty()) {
            return;
        }

        // Convert wild_four to draw_four in the card info
        if (cardInfo.contains("_wild_four")) {
            cardInfo = cardInfo.replace("_wild_four", "_draw_four");
        }

        // Handle wild cards first
        if (cardInfo.startsWith("wild_")) {
            String[] parts = cardInfo.split("_");
            AbstractCard card;
            if (parts[0].equals("wild")) {
                if (parts.length > 1 && (parts[1].equals("draw4") || parts[1].equals("draw_four"))) {
                    card = new CardAction(Colors.WILD, Types.DRAW_FOUR);
                } else {
                    card = new CardAction(Colors.WILD, Types.WILD);
                }
            } else {
                card = new CardAction(Colors.WILD, Types.DRAW_FOUR);
            }
            // Set the chosen color from the card info
            if (parts.length > 2) {
                Colors chosenColor = Colors.valueOf(parts[2].toUpperCase());
                card.setColor(chosenColor);
                currentColor = chosenColor;
            }
            deck.discard(card);
            lastPlayedCard = card;
            return;
        }

        // Handle regular cards
        String[] parts = cardInfo.split("_");
        if (parts.length < 2) {
            return;
        }

        Colors color = Colors.valueOf(parts[0].toUpperCase());
        AbstractCard card;
        
        String value = parts[1];
        if (value.equals("reverse")) {
            card = new CardAction(color, Types.REVERSE);
        } else if (value.equals("skip")) {
            card = new CardAction(color, Types.SKIP);
        } else if (value.equals("draw2") || value.equals("draw_two")) {
            card = new CardAction(color, Types.DRAW_TWO);
        } else if (value.equals("draw4") || value.equals("draw_four")) {
            card = new CardAction(color, Types.DRAW_FOUR);
        } else if (value.equals("wild") || value.equals("card")) {
            card = new CardAction(color, Types.WILD);
        } else {
            try {
                int numValue = Integer.parseInt(value);
                card = new CardNumber(color, numValue);
            } catch (NumberFormatException e) {
                return;
            }
        }
        
        deck.discard(card);
        lastPlayedCard = card;
        currentColor = card.getColor();
    }

    public List<AbstractCard> getDiscardPile() {
        return deck.getDiscardPile();
    }

    public void setCurrentPlayer(String playerName) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(playerName)) {
                currentPlayerIndex = i;
                break;
            }
        }
    }

    protected int getTurnNumber() {
        return currentPlayerIndex;  // Use currentPlayerIndex as turn number
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
}