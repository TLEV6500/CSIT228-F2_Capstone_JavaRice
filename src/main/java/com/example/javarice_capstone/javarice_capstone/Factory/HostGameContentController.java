package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.GenerateLobbyCode;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.JoinLobby;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.LobbyManager;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.SessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class HostGameContentController {
    @FXML private TextField hostAddressTextField;
    @FXML private TextField usernameTextField;
    @FXML private Button Host;
    @FXML private Button cancelButton;

    private GameSetupDialogController parent;

    public void init(GameSetupDialogController parent) {
        this.parent = parent;
    }

    @FXML
    private void hostButtonClicked() {
        String username = usernameTextField.getText() != null ? usernameTextField.getText().trim() : "";
        String lobbyAddress = hostAddressTextField.getText() != null ? hostAddressTextField.getText().trim() : "";

        if (!username.isEmpty() && !lobbyAddress.isEmpty()) {
            parent.onHostGameOk(username);

            // Step 1: Generate lobby code
            String lobbyCode = GenerateLobbyCode.GenerateLobbyCode();
            SessionState.LobbyCode = lobbyCode;
            SessionState.LobbyConnection = lobbyAddress;

            // Step 2: Create the lobby
            String creationResult = LobbyManager.createLobby(lobbyCode);
            System.out.println("Create Lobby Result: " + creationResult);

            // Optional: check if creation failed
            if (creationResult.contains("already exists") || creationResult.contains("error")) {
                System.out.println("⚠️ Lobby creation failed.");
                return;
            }

            // Step 3: Assign host
            boolean hostAssigned = LobbyManager.assignHost(lobbyCode, username);
            System.out.println("Host assigned: " + hostAssigned);

            // Step 4: Join lobby
            String joinResult = JoinLobby.joinLobby(username, lobbyAddress, lobbyCode);
            System.out.println("Join Lobby Result: " + joinResult);
        }
    }


    @FXML
    private void cancelClicked() {
        parent.onHostGameCancel();
        SessionState.LobbyConnection = null;
        SessionState.LobbyCode = null;
    }
}
