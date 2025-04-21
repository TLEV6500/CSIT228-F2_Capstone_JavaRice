package com.example.javarice_capstone.javarice_capstone.datatypes;

public class GameRound implements SerializableGameData {
    private int id;
    private int[] playerIds;
    private int winner;
    private long duration;
    private boolean isOngoing;

    GameRound() {
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
