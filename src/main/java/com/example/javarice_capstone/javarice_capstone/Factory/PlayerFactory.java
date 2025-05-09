package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerComputer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerHuman;

import java.util.Random;

public class PlayerFactory {

    private static final String[] COMPUTER_TYPES = {"COMPUTER - N", "COMPUTER - A", "COMPUTER - D", "COMPUTER - T"};
    private static final Random RANDOM = new Random();

    public static AbstractPlayer createPlayer(String type, String name) {
        return switch (type) {
            case "HUMAN" -> new PlayerHuman(name);
            case "COMPUTER - N", "COMPUTER - A", "COMPUTER - D", "COMPUTER - T" -> new PlayerComputer(name, StrategyFactory.createStrategy(type));
            default -> throw new IllegalArgumentException("Unknown player type: " + type);
        };
    }

    public static String getRandomComputerType() {
        return COMPUTER_TYPES[RANDOM.nextInt(COMPUTER_TYPES.length)];
    }

}