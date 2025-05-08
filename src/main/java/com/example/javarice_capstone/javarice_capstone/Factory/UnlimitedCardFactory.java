package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.CardFactory;
import com.example.javarice_capstone.javarice_capstone.Models.CardAction;
import com.example.javarice_capstone.javarice_capstone.Models.CardNumber;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.Random;

public class UnlimitedCardFactory implements CardFactory {
    private final Random random = new Random();

    @Override
    public AbstractCard createCard() {
        int cardType = random.nextInt(2);
        Types[] actions = {Types.SKIP, Types.REVERSE, Types.DRAW_TWO, Types.WILD, Types.DRAW_FOUR};
        if (cardType == 0) {
            Colors color = Colors.values()[random.nextInt(4)];
            int number = random.nextInt(10);
            return new CardNumber(color, number);
        } else {
            Types type = actions[random.nextInt(actions.length)];
            Colors color;

            if (type == Types.WILD || type == Types.DRAW_FOUR) color = Colors.WILD;
            else color = Colors.values()[random.nextInt(4)];

            return new CardAction(color, type);
        }
    }
}