package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.Strategies.*;

public class StrategyFactory {

    public static ComputerStrategy createStrategy(String type) {
        return switch (type) {
            case "COMPUTER - N" -> new NormalStrat();
            case "COMPUTER - A" -> new AggressiveStrat();
            case "COMPUTER - D" -> new DefensiveStrat();
            default -> throw new IllegalArgumentException("Unknown strategy type: " + type);
        };
    }

}