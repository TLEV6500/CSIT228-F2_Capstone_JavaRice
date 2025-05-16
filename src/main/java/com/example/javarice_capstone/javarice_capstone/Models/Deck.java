package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<AbstractCard> cards;
    private final List<AbstractCard> discardPile;
    public Deck() {
        cards = new ArrayList<>();
        discardPile = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    protected void initializeDeck() {
        cards.clear();
        cards.addAll(createStandardUnoDeck());
    }

    private List<AbstractCard> createStandardUnoDeck() {
        List<AbstractCard> deck = new ArrayList<>(108);

        Colors[] colors = {Colors.RED, Colors.YELLOW, Colors.GREEN, Colors.BLUE};

        for (Colors color : colors) {
            deck.add(new CardNumber(color, 0));
            for (int i = 1; i <= 9; i++) {
                deck.add(new CardNumber(color, i));
                deck.add(new CardNumber(color, i));
            }

            for (int i = 0; i < 2; i++) {
                deck.add(new CardAction(color, Types.SKIP));
                deck.add(new CardAction(color, Types.REVERSE));
                deck.add(new CardAction(color, Types.DRAW_TWO));
            }
            
        }

        for (int i = 0; i < 4; i++) {
            deck.add(new CardAction(Colors.WILD, Types.WILD));
            deck.add(new CardAction(Colors.WILD, Types.DRAW_FOUR));
        }

        return deck;
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public AbstractCard drawCard() {
        if (cards.isEmpty()) {
            initializeDeck();
            shuffle();
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

    public List<AbstractCard> getDiscardPile() {
        return discardPile;
    }

}