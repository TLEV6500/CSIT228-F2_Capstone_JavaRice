package com.example.javarice_capstone.javarice_capstone.Abstracts;

import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

public abstract class AbstractCard {

    protected Colors color;
    private final Types type;

    public AbstractCard(Colors color, Types type) {
        this.color = color;
        this.type = type;
    }

    public Colors getColor() { return color; }
    public Types getType() { return type; }

    public void setColor(Colors color) {
        this.color = color;
    }

    public abstract boolean canPlayOn(AbstractCard other);

    @Override
    public String toString() {
        return color + " " + type;
    }

    public abstract String getImagePath();

    public abstract int getValue();
}