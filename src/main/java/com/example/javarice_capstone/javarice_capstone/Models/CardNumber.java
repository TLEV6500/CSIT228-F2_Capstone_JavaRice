package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

public class CardNumber extends AbstractCard {
    private final int number;

    public CardNumber(Colors color, int number) {
        super(color, Types.NUMBER);
        this.number = number;
    }

    @Override
    public int getValue() {
        return number;
    }

    @Override
    public boolean canPlayOn(AbstractCard other) {
        return this.getColor() == other.getColor() || (other instanceof CardNumber && ((CardNumber) other).getValue() == this.number);
    }

    @Override
    public String toString() {
        return getColor() + " " + number;
    }

    @Override
    public String getImagePath() {
        String colorStr = getColor().toString().toLowerCase();
        return "/images/cards/" + colorStr + "_" + number + ".png";
    }
}