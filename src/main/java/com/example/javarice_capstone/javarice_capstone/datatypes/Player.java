package com.example.javarice_capstone.javarice_capstone.data;

import java.io.Serializable;

public class Player implements SerializableGameData {
    private int id;
    private String name;
    private GameScore highestScore;

    @Override
    public int getId() {
        return id;
    }
}
