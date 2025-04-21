package com.example.javarice_capstone.javarice_capstone.datatypes;

public class User implements SerializableGameData {
    private int id;
    private String userName;
    private int avatarIconId;
    private Player[] players;

    User() {
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
