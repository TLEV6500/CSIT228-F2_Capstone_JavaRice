package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.*;

public class PlayerComputer extends AbstractPlayer {
    private final ComputerStrategy strategy;

    private boolean hasDrawnCardThisTurn = false;

    public PlayerComputer(String name, ComputerStrategy strategy) {
        super(name);
        this.strategy = strategy;
    }

    @Override
    public int selectCardToPlay(AbstractCard topCard, Colors currentColor) {
        return strategy.selectCardToPlay(getHand(), topCard, currentColor);
    }

    public ComputerActionResult stepTurn(Game game) {
        int cardToPlay = selectCardToPlay(game.getTopCard(), game.getCurrentColor());
        if (cardToPlay >= 0) {
            // Play a card from hand
            AbstractCard selectedCard = getHand().get(cardToPlay);
            Types type = selectedCard.getType();
            game.playCard(cardToPlay);

            if (type == Types.WILD || type == Types.DRAW_FOUR) {
                Colors newColor = Game.getRandomColorExcept(game.getCurrentColor());
                game.setCurrentColor(newColor);
            }

            hasDrawnCardThisTurn = false; // Reset for next turn
            return ComputerActionResult.PLAYED;
        } else if (!hasDrawnCardThisTurn) {
            // No playable card, draw a card
            game.drawCard();
            hasDrawnCardThisTurn = true; // Mark that we have drawn this turn
            return ComputerActionResult.DRAWN;
        } else {
            // Just drew a card, now try to play it immediately
            int drawnIndex = getHand().size() - 1;
            AbstractCard justDrawn = getHand().get(drawnIndex);
            AbstractCard topCard = game.getTopCard();
            Colors currentColor = game.getCurrentColor();
            if (justDrawn.canPlayOn(topCard) || justDrawn.getColor() == currentColor) {
                // Play the just-drawn card
                Types type = justDrawn.getType();
                game.playCard(drawnIndex);
                if (type == Types.WILD || type == Types.DRAW_FOUR) {
                    Colors newColor = Game.getRandomColorExcept(game.getCurrentColor());
                    game.setCurrentColor(newColor);
                }
                hasDrawnCardThisTurn = false;
                return ComputerActionResult.PLAYED;
            } else {
                // Cannot play, end turn
                hasDrawnCardThisTurn = false;
                return ComputerActionResult.DONE;
            }
        }
    }

    public void resetAITurnState() {
        hasDrawnCardThisTurn = false;
    }
}