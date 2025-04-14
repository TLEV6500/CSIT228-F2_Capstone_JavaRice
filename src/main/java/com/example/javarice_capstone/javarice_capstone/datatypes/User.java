package com.example.javarice_capstone.javarice_capstone.data;

public class User implements SerializableGameData {
    private int id;
    private String userName;
    private int avatarIconId;
    private Player[] players;

    @Override
    public int getId() {
        return id;
    }
}
