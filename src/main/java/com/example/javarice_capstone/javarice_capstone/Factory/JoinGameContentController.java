package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.JoinLobby;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.SessionState;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.util.List;

public class JoinGameContentController {
    @FXML private TextField lobbyAddressTextField;
    @FXML private TextField usernameTextField;
    @FXML private TextField lobbyCodeTextField;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private GameSetupDialogController parent;

    public void init(GameSetupDialogController parent) {
        this.parent = parent;
    }

    @FXML
    private void joinButtonClicked() {
        String username = usernameTextField.getText() != null ? usernameTextField.getText().trim() : "";
        String lobbyAddress = lobbyAddressTextField.getText() != null ? lobbyAddressTextField.getText().trim() : "";
        String lobbyCode = lobbyCodeTextField.getText() != null ? lobbyCodeTextField.getText().trim() : "";

        if (username.isEmpty() || lobbyAddress.isEmpty() || lobbyCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in both username and host code.");
            return;
        }

        // Validate host address format
        if (!lobbyAddress.contains(":")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Host Address", 
                "Please enter the host address in the format: IP_ADDRESS:PORT\n" +
                "Example: localhost:3306 or 192.168.1.100:3306");
            return;
        }

        SessionState.LobbyCode = lobbyCode;
        SessionState.LobbyConnection = lobbyAddress;

        String joinResult = JoinLobby.joinLobby(username, lobbyCode);
        System.out.println(joinResult);

        if (joinResult.startsWith("Player")) {
            // Successfully joined, fetch current players
            List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
            if (players != null && !players.isEmpty()) {
                // Successfully joined and got player list, continue to parent
                parent.onJoinGameOk(username, lobbyAddress, lobbyCode, players);
            } else {
                showAlert(Alert.AlertType.ERROR, "Join Failed", "Could not fetch player list from lobby.");
            }
        } else {
            // Failed to join, show error
            showAlert(Alert.AlertType.ERROR, "Join Failed", joinResult);
        }
    }

    @FXML
    private void cancelClicked() {
        parent.onJoinGameCancel();
        SessionState.LobbyConnection = null;
        SessionState.LobbyCode = null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
