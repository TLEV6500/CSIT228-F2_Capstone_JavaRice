package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    protected List<AbstractCard> cards;
    protected final List<AbstractCard> discardPile;

    public Deck() {
        cards = new ArrayList<>();
        discardPile = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    protected void initializeDeck() {
        for (Colors color : new Colors[] {Colors.RED, Colors.BLUE, Colors.GREEN, Colors.YELLOW}) {
            cards.add(new CardNumber(color, 0));

            for (int number = 1; number <= 9; number++) {
                cards.add(new CardNumber(color, number));
                cards.add(new CardNumber(color, number));
            }

            cards.add(new CardAction(color, Types.SKIP));
            cards.add(new CardAction(color, Types.SKIP));
            cards.add(new CardAction(color, Types.REVERSE));
            cards.add(new CardAction(color, Types.REVERSE));
            cards.add(new CardAction(color, Types.DRAW_TWO));
            cards.add(new CardAction(color, Types.DRAW_TWO));
        }

        for (int i = 0; i < 4; i++) {
            cards.add(new CardAction(Colors.WILD, Types.WILD));
            cards.add(new CardAction(Colors.WILD, Types.DRAW_FOUR));
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public AbstractCard drawCard() {
        if (cards.isEmpty()) {
            if (discardPile.size() <= 1) {
                initializeDeck();
                shuffle();
            } else {
                AbstractCard topCard = discardPile.remove(discardPile.size() - 1);
                cards = new ArrayList<>(discardPile);
                discardPile.clear();
                discardPile.add(topCard);
                shuffle();
            }
        }

        return cards.remove(cards.size() - 1);
    }

    public void discard(AbstractCard card) {
        discardPile.add(card);
    }

    public AbstractCard getTopDiscard() {
        if (discardPile.isEmpty()) {
            return null;
        }
        return discardPile.get(discardPile.size() - 1);
    }
}