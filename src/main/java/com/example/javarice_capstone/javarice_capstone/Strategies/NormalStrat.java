package com.example.javarice_capstone.javarice_capstone.Strategies;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NormalStrat implements ComputerStrategy {

    @Override
    public int selectCardToPlay(List<AbstractCard> hand, AbstractCard topCard, Colors currentColor) {
        List<Integer> suitableIndices = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            AbstractCard card = hand.get(i);
            if (card.canPlayOn(topCard) || card.getColor() == currentColor) suitableIndices.add(i);
        }
        if (suitableIndices.isEmpty()) return -1;
        Random random = new Random();
        return suitableIndices.get(random.nextInt(suitableIndices.size()));
    }

}