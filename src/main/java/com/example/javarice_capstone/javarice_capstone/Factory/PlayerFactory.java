package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerComputer;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerHuman;
import com.example.javarice_capstone.javarice_capstone.Strategies.AggressiveStrat;
import com.example.javarice_capstone.javarice_capstone.Strategies.DefensiveStrat;
import com.example.javarice_capstone.javarice_capstone.Strategies.NormalStrat;
import com.example.javarice_capstone.javarice_capstone.Strategies.TrollStrat;

import java.util.Random;

public class PlayerFactory {

    private static final String[] COMPUTER_TYPES = {"COMPUTER - N", "COMPUTER - A", "COMPUTER - D", "COMPUTER - T"};
    private static final Random RANDOM = new Random();

    public static AbstractPlayer createPlayer(String type, String name) {
        switch (type) {
            case "HUMAN":
                return new PlayerHuman(name);
            case "COMPUTER - N":
                return new PlayerComputer(name, new NormalStrat());
            case "COMPUTER - A":
                return new PlayerComputer(name, new AggressiveStrat());
            case "COMPUTER - D":
                return new PlayerComputer(name, new DefensiveStrat());
            case "COMPUTER - T":
                return new PlayerComputer(name, new TrollStrat());
            default:
                throw new IllegalArgumentException("Unknown player type: " + type);
        }
    }

    public static String getRandomComputerType() {
        return COMPUTER_TYPES[RANDOM.nextInt(COMPUTER_TYPES.length)];
    }
}