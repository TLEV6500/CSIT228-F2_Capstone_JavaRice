package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Abstracts.ComputerStrategy;
import com.example.javarice_capstone.javarice_capstone.Strategies.*;

public class StrategyFactory {
    public static ComputerStrategy createStrategy(String type) {
        switch (type) {
            case "COMPUTER - N": return new NormalStrat();
            case "COMPUTER - A": return new AggressiveStrat();
            case "COMPUTER - D": return new DefensiveStrat();
            case "COMPUTER - T": return new TrollStrat();
            default:
                throw new IllegalArgumentException("Unknown strategy type: " + type);
        }
    }
}