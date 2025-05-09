package com.example.javarice_capstone.javarice_capstone.Abstracts;

import java.util.ArrayList;
import java.util.List;

import com.example.javarice_capstone.javarice_capstone.enums.Colors;

public abstract class AbstractPlayer {
    private String name;
    private List<AbstractCard> hand;

    public AbstractPlayer(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<AbstractCard> getHand() {
        return hand;
    }

    public void addCard(AbstractCard card) {
        hand.add(card);
    }

    public AbstractCard playCard(int index) {
        if (index >= 0 && index < hand.size()) {
            return hand.remove(index);
        }
        return null;
    }

    public boolean hasValidMove(AbstractCard topCard, Colors currentColor) {
        for (AbstractCard card : hand) {
            if (card.canPlayOn(topCard) || card.getColor() == currentColor) {
                return true;
            }
        }
        return false;
    }

    public abstract int selectCardToPlay(AbstractCard topCard, Colors currentColor);

    public boolean hasUno() {
        return hand.size() == 1;
    }

    public boolean hasWon() {
        return hand.isEmpty();
    }
}