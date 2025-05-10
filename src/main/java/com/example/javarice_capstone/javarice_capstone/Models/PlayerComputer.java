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

    public ComputerActionResult stepTurn(Game game) {
        while (true) {
            int cardToPlay = selectCardToPlay(game.getTopCard(), game.getCurrentColor());
            if (cardToPlay >= 0) {
                AbstractCard selectedCard = getHand().get(cardToPlay);
                Types type = selectedCard.getType();
                game.playCard(cardToPlay);

                if (type == Types.WILD || type == Types.DRAW_FOUR) {
                    Colors newColor = Game.getRandomColorExcept(game.getCurrentColor());
                    game.setCurrentColor(newColor);
                }

                return ComputerActionResult.PLAYED;
            } else {
                AbstractCard drawn = game.drawCard();
                addCard(drawn);
            }
        }
    }

}