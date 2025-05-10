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
        return -1; // Handled by the GUI, not needed
    }

}