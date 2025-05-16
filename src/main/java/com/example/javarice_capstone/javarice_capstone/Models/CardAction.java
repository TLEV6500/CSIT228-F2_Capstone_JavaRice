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
        String colorStr = getColor().toString().toLowerCase();
        
        // For DRAW_FOUR cards, always use draw_four in the path
        if (getType() == Types.DRAW_FOUR) {
            if (getColor() == Colors.WILD) {
                return "/images/cards/wild_draw_four.png";
            } else {
                return "/images/cards/" + colorStr + "_draw_four.png";
            }
        }
        // For WILD cards
        else if (getType() == Types.WILD) {
            if (getColor() == Colors.WILD) {
                return "/images/cards/wild_card.png";
            } else {
                return "/images/cards/" + colorStr + "_card.png";
            }
        }
        // For other action cards
        else {
            return "/images/cards/" + colorStr + "_" + typeStr + ".png";
        }
    }

    @Override
    public int getValue() {
        return -1;
    }

    @Override
    public String toString() {
        return getColor() + " " + getType();
    }
}