package com.example.javarice_capstone.javarice_capstone.Abstracts;

import com.example.javarice_capstone.javarice_capstone.enums.Colors;

import java.util.List;

public interface ComputerStrategy {
    int selectCardToPlay(List<AbstractCard> hand, AbstractCard topCard, Colors currentColor);
}