package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

public class CardAction extends AbstractCard {

    public CardAction(Colors color, Types type) {
        super(color, type);
        if (type == Types.NUMBER) throw new IllegalArgumentException("ActionCard type cannot be NUMBER");
    }

    @Override
    public boolean canPlayOn(AbstractCard other) {
        if (getColor() == Colors.WILD) return true;
        return this.getColor() == other.getColor() || this.getType() == other.getType();
    }

    @Override
    public String getImagePath() {
        String typeStr = getType().toString().toLowerCase();
        if (getType() == Types.WILD || getType() == Types.DRAW_FOUR) {
            return "/images/cards/wild_" + typeStr + ".png";
        } else {
            String colorStr = getColor().toString().toLowerCase();
            return "/images/cards/" + colorStr + "_" + typeStr + ".png";
        }
    }

    @Override
    public String toString() {
        return getColor() + " " + getType();
    }
}