package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.CardFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    protected List<AbstractCard> cards;
    protected final List<AbstractCard> discardPile;
    private final CardFactory cardFactory;

    public Deck(CardFactory cardFactory) {
        this.cardFactory = cardFactory;
        cards = new ArrayList<>();
        discardPile = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    protected void initializeDeck() {
        for (int i = 0; i < 50; i++) {
            cards.add(cardFactory.createCard());
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public AbstractCard drawCard() {
        if (cards.isEmpty()) {
            for (int i = 0; i < 50; i++) cards.add(cardFactory.createCard());
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
}