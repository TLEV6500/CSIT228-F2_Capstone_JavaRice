package com.example.javarice_capstone.javarice_capstone.data;

public class GameRound implements SerializableGameData {
    private int id;
    private int[] playerIds;
    private int winner;
    private long duration;
    private boolean isOngoing;

    @Override
    public int getId() {
        return id;
    }
}
