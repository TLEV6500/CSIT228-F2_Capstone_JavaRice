package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.util.Random;

public class GenerateLobbyCode {
    public static String GenerateLobbyCode(){
        String lobbyCode = "";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Random rand = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(rand.nextInt(chars.length())));
        }
        lobbyCode = code.toString();
        return lobbyCode;
    }
}
