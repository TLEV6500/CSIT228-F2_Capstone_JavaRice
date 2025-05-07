package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Factory.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;

public class PlayerComputer extends AbstractPlayer {
    private final ComputerStrategy strategy;

    public PlayerComputer(String name, ComputerStrategy strategy) {
        super(name);
        this.strategy = strategy;
    }

    @Override
    public int selectCardToPlay(AbstractCard topCard, Colors currentColor) {
        return strategy.selectCardToPlay(getHand(), topCard, currentColor);
    }
}