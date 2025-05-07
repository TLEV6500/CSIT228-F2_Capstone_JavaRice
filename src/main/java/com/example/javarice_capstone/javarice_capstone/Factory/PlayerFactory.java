package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerComputer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerHuman;
import com.example.javarice_capstone.javarice_capstone.Strategies.NormalStrat;

public class PlayerFactory {

    // Why use a Factory for player creation in the Game class?
    //
    // - Keeps Game class simpler by not dealing with player details.
    // - Easy to add new player types (like AI or network players) later.
    // - All player creation code is in one place (the Factory).
    // - Makes it easy to pass special settings or strategies to players.
    // - Follows good design practices (Open/Closed Principle).

    public static AbstractPlayer createPlayer(String type, String name) {
        switch (type) {
            case "HUMAN":
                return new PlayerHuman(name);
            case "COMPUTER - S":
                return new PlayerComputer(name, new NormalStrat());
            default:
                throw new IllegalArgumentException("Unknown player type: " + type);
        }
    }

}