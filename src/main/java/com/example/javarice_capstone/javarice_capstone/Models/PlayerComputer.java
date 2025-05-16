package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.*;

// THIS IS FOR THE AI COMPUTER ON SINGLE PLAYER MODE
public class PlayerComputer extends AbstractPlayer {
    private final ComputerStrategy strategy;

    public PlayerComputer(String name, ComputerStrategy strategy) {
        super(name);
        this.strategy = strategy;
    }

    @Override
    public int selectCardToPlay(AbstractCard topCard, Colors currentColor) {
        return strategy.selectCardToPlay(getHand(), topCard, currentColor);
    }

    public ComputerActionResult stepTurn(Game game) {
        // If this is a forced draw turn, computer must draw first
        if (game.isForcedDrawTurn && !game.hasHandledForcedDraw) {
            AbstractCard drawn = game.drawCard();
            addCard(drawn);
            System.out.println("[DEBUG] Computer " + getName() + " drew a card: " + drawn.toString());
            System.out.println("[DEBUG] Computer " + getName() + " now has " + getHand().size() + " cards in hand");
            game.hasHandledForcedDraw = true;
            return ComputerActionResult.DRAWN;
        }

        // Try to play a card
        int cardToPlay = selectCardToPlay(game.getTopCard(), game.getCurrentColor());
        if (cardToPlay >= 0) {
            AbstractCard selectedCard = getHand().get(cardToPlay);
            Types type = selectedCard.getType();

            // Handle WILD and WILD DRAW FOUR
            if (type == Types.WILD || type == Types.DRAW_FOUR) {
                game.playCard(cardToPlay);
                Colors newColor = Game.getRandomColorExcept(game.getCurrentColor());
                game.setCurrentColor(newColor);
                return ComputerActionResult.PLAYED;
            }

            // Handle regular cards
            game.playCard(cardToPlay);
            return ComputerActionResult.PLAYED;
        }
        
        // If no card can be played, draw a card
            AbstractCard drawn = game.drawCard();
            addCard(drawn);
        System.out.println("[DEBUG] Computer " + getName() + " drew a card: " + drawn.toString());
        System.out.println("[DEBUG] Computer " + getName() + " now has " + getHand().size() + " cards in hand");
        game.hasHandledForcedDraw = true;
            return ComputerActionResult.DRAWN;
    }

}