package com.example.javarice_capstone.javarice_capstone.Strategies;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;

import java.util.List;

/**
 * Simple strategy: Play the first valid card found.
 */
public class NormalStrat implements ComputerStrategy {

    @Override
    public int selectCardToPlay(List<AbstractCard> hand, AbstractCard topCard, Colors currentColor) {
        for (int i = 0; i < hand.size(); i++) {
            AbstractCard card = hand.get(i);
            if (card.canPlayOn(topCard) || card.getColor() == currentColor) {
                return i;
            }
        }
        return -1;
    }

}