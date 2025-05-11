package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;

public class PlayerHuman extends AbstractPlayer {
    public PlayerHuman(String name) {
        super(name);
    }

    @Override
    public int selectCardToPlay(AbstractCard topCard, Colors currentColor) {
        return -1; // Wa ray gamit, Handled by GUI so return -1
    }

}