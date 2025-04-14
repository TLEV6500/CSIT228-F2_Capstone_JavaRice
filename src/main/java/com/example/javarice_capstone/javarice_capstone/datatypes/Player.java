package com.example.javarice_capstone.javarice_capstone.datatypes;

public class Player implements SerializableGameData {
    private int id;
    private String name;
    private GameScore highestScore;

    Player() {
        id = getNextId();
    }

    @Override
    public int getId() {
        return id;
    }
    private static int ID = 0;
    @Override
    public int getNextId() {
        return ID++;
    }
}
