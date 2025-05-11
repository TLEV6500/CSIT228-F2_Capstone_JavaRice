package com.example.javarice_capstone.javarice_capstone.Multiplayer;

public class HostLobby {

    public static void hostLobby(String hostPlayer) {
        String lobbyCode = SessionState.LobbyCode; // Replace with your actual pre-generated logic if needed
        String lobbyAddress = SessionState.LobbyConnection;

        // Step 1: Create the lobby
        String createdLobby = LobbyManager.createLobby(lobbyCode);
        if (createdLobby == null) {
            System.out.println("❌ Failed to create lobby. It may already exist.");
            return;
        }

        // Step 2: Assign host to the lobby
        boolean hostSet = LobbyManager.assignHost(lobbyCode, hostPlayer);
        if (!hostSet) {
            System.out.println("❌ Failed to assign host.");
            return;
        }

        // Step 3: Have the host join the lobby as a player
        String result = JoinLobby.joinLobby(hostPlayer, lobbyAddress, lobbyCode);
        System.out.println(result);
    }
}
