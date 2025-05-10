package com.example.javarice_capstone.javarice_capstone.Strategies;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.List;

public class AggressiveStrat implements ComputerStrategy {

    @Override
    public int selectCardToPlay(List<AbstractCard> hand, AbstractCard topCard, Colors currentColor) {

        // Play any action card first (not NUMBER)
        for (int i = 0; i < hand.size(); i++) {
            AbstractCard card = hand.get(i);
            if (card.getType() != Types.NUMBER &&
                    (card.canPlayOn(topCard) || card.getColor() == currentColor)) {
                return i;
            }
        }

        // Otherwise, play any valid card
        for (int i = 0; i < hand.size(); i++) {
            AbstractCard card = hand.get(i);
            if (card.canPlayOn(topCard) || card.getColor() == currentColor) {
                return i;
            }
        }
        return -1;
    }

}