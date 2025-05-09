package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.*;

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

    public boolean playTurn(Game game) {
        boolean hasPlayed = false;
        while (!hasPlayed) {
            int cardToPlay = selectCardToPlay(game.getTopCard(), game.getCurrentColor());
            if (cardToPlay >= 0) {
                AbstractCard selectedCard = getHand().get(cardToPlay);
                Types type = selectedCard.getType();
                game.playCard(cardToPlay);

                if (type == Types.WILD || type == Types.DRAW_FOUR) {
                    Colors newColor = Game.getRandomColorExcept(game.getCurrentColor());
                    game.setCurrentColor(newColor);
                }

                hasPlayed = true;
            } else {
                AbstractCard drawn = game.drawCard();

                int drawnIndex = getHand().size() - 1;
                AbstractCard topCard = game.getTopCard();
                Colors currentColor = game.getCurrentColor();
                AbstractCard justDrawn = getHand().get(drawnIndex);
                if (justDrawn.canPlayOn(topCard) || justDrawn.getColor() == currentColor) {
                    Types type = justDrawn.getType();
                    game.playCard(drawnIndex);
                    if (type == Types.WILD || type == Types.DRAW_FOUR) {
                        Colors newColor = Game.getRandomColorExcept(game.getCurrentColor());
                        game.setCurrentColor(newColor);
                    }
                    hasPlayed = true;
                }
            }
        }
        return hasPlayed;
    }

}