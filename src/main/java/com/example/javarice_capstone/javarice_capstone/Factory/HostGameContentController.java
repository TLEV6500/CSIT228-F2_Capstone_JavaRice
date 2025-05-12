package com.example.javarice_capstone.javarice_capstone.Factory;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.GenerateLobbyCode;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.JoinLobby;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.LobbyManager;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.SessionState;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import static com.example.javarice_capstone.javarice_capstone.Multiplayer.XAMPP_Initializer.addShutdownHook;
import static com.example.javarice_capstone.javarice_capstone.Multiplayer.XAMPP_Initializer.stopXAMPP;

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
            // Validate MySQL connection first
            try {
                String testUrl = "jdbc:mysql://" + lobbyAddress + "/?useSSL=false&connectTimeout=5000";
                java.sql.Connection testConn = java.sql.DriverManager.getConnection(testUrl, "root", "");
                testConn.close();
            } catch (java.sql.SQLException e) {
                System.err.println("❌ Failed to connect to MySQL server at: " + lobbyAddress);
                System.err.println("Error: " + e.getMessage());
                // Show error to user
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText("Cannot connect to MySQL server");
                alert.setContentText("Please check that:\n" +
                    "1. The MySQL server is running\n" +
                    "2. The address is correct (e.g., localhost:3306)\n" +
                    "3. The server is accepting connections");
                alert.showAndWait();
                return;
            }

            parent.onHostGameOk(username);

            // Step 1: Generate lobby code
            String lobbyCode = GenerateLobbyCode.GenerateLobbyCode();
            SessionState.LobbyCode = lobbyCode;
            SessionState.LobbyConnection = lobbyAddress;

            // Step 2: Create the lobby
            String creationResult = LobbyManager.createLobby(lobbyCode);
            System.out.println("Create Lobby Result: " + creationResult);

            // Optional: check if creation failed
            if (creationResult == null || creationResult.contains("already exists") || creationResult.contains("error")) {
                System.out.println("⚠️ Lobby creation failed.");
                return;
            }

            // Step 3: Assign host
            boolean hostAssigned = LobbyManager.assignHost(lobbyCode, username);
            System.out.println("Host assigned: " + hostAssigned);
            addShutdownHook();
        }
    }


    @FXML
    private void cancelClicked() {
        parent.onHostGameCancel();
    }
}
