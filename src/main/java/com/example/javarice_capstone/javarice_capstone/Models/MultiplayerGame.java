package com.example.javarice_capstone.javarice_capstone.Models;

import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractPlayer;
import com.example.javarice_capstone.javarice_capstone.Factory.PlayerFactory;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;
import com.example.javarice_capstone.javarice_capstone.enums.Types;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager;
import com.example.javarice_capstone.javarice_capstone.Models.CardNumber;
import com.example.javarice_capstone.javarice_capstone.Models.CardAction;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

public class MultiplayerGame extends Game {
    private String lobbyCode;
    private boolean isHost;
    private String localPlayerName;
    private int turnNumber = 0;
    private String currentPlayerName;
    private String hostPlayerName;
    private boolean hasHandledForcedDraw = false;  // New flag to track forced draws

    public MultiplayerGame(int numPlayers, String lobbyCode, boolean isHost, String localPlayerName) {
        super(1); // Only create one player (the local player)
        this.lobbyCode = lobbyCode;
        this.isHost = isHost;
        this.localPlayerName = localPlayerName;
        
        // Set the local player's name
        getPlayers().get(0).setName(localPlayerName);
        
        // For multiplayer, we don't want to initialize the discard pile here
        // It will be set by the host and synchronized to other players
        if (!isHost) {
            // Clear the initial discard pile for joined players
            getDiscardPile().clear();
        }
        // Get the host player name from the lobbies table
        hostPlayerName = com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.getHostPlayerName(lobbyCode);
        // Always set current player to host at game start
        com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager.setCurrentPlayer(lobbyCode, hostPlayerName);
        currentPlayerName = hostPlayerName;
    }

    @Override
    public boolean isPlayersTurn(int playerIndex) {
        // Get the current player from the database
        String dbCurrentPlayer = ThreadLobbyManager.getCurrentPlayer(lobbyCode);
        if (dbCurrentPlayer != null) {
            currentPlayerName = dbCurrentPlayer;
            System.out.println("[DEBUG] Current player from DB: " + currentPlayerName);
        }
        boolean isTurn = currentPlayerName != null && currentPlayerName.equals(getPlayers().get(playerIndex).getName());
        System.out.println("[DEBUG] Is player's turn: " + isTurn + " for player: " + getPlayers().get(playerIndex).getName());
        return isTurn;
    }

    @Override
    public boolean playCard(int cardIndex) {
        // Check if it's the local player's turn
        if (!isPlayersTurn(0)) {
            System.out.println("[DEBUG] Not your turn! Current player: " + currentPlayerName);
            return false;
        }

        // Get the card to play
        AbstractCard topCard = getTopCard();
        AbstractCard cardToPlay = getPlayers().get(0).getHand().get(cardIndex);

        // Check if we can stack a DRAW_TWO
        if (topCard != null && topCard.getType() == Types.DRAW_TWO && 
            cardToPlay.getType() == Types.DRAW_TWO) {
            // Allow stacking DRAW_TWO cards
            System.out.println("[DEBUG] Stacking DRAW_TWO card");
        } else if (!cardToPlay.canPlayOn(topCard) && cardToPlay.getColor() != getCurrentColor()) {
            System.out.println("[DEBUG] Cannot play this card!");
            return false;
        }

        boolean result = super.playCard(cardIndex);
        if (result) {
            AbstractCard playedCard = getLastPlayedCard();
            String cardInfo;
            boolean reversePlayed = false;
            boolean newDirection = isCustomOrderClockwise();
            
            if (playedCard.getType() == Types.REVERSE) {
                cardInfo = playedCard.getColor() + "_reverse";
                // Update game direction in database
                newDirection = !isCustomOrderClockwise();
                ThreadLobbyManager.updateGameDirection(lobbyCode, newDirection);
                reversePlayed = true;
                
                // Get players list for 2-player check
                List<ThreadLobbyManager.PlayerInfo> dbPlayers = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
                
                // For 2-player game, reverse acts like a skip
                if (dbPlayers.size() == 2) {
                    // Keep the same player's turn
                    ThreadLobbyManager.setCurrentPlayer(lobbyCode, currentPlayerName);
                    // Don't call updateGameState here to avoid recursion
                    Platform.runLater(() -> notifyTurnChanged());
                    return result;
                }
            } else if (playedCard.getType() == Types.SKIP) {
                cardInfo = playedCard.getColor() + "_skip";
            } else if (playedCard.getType() == Types.DRAW_TWO) {
                cardInfo = playedCard.getColor() + "_draw2";
                
                // First increment turnNumber for consistent tracking
                turnNumber++;
                
                // Mark the current turn as unhandled so the next player is forced to draw
                ThreadLobbyManager.markForcedDrawHandled(lobbyCode, turnNumber);
                
                // Record the move with the current turn number
                ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, cardInfo, "play", turnNumber);
                ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, getPlayers().get(0).getHand().size());
                
                // Skip to next-next player (since next player will draw 2 and lose turn)
                List<ThreadLobbyManager.PlayerInfo> dbPlayers = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
                int currentIndex = -1;
                for (int i = 0; i < dbPlayers.size(); i++) {
                    if (dbPlayers.get(i).name.equals(currentPlayerName)) {
                        currentIndex = i;
                        break;
                    }
                }
                
                if (currentIndex != -1) {
                    boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
                    setCustomOrderClockwise(dbDirection);
                    
                    int nextIndex;
                    if (dbDirection) {
                        nextIndex = (currentIndex + 2) % dbPlayers.size();
                    } else {
                        nextIndex = (currentIndex - 2 + dbPlayers.size()) % dbPlayers.size();
                    }
                    
                    String nextPlayer = dbPlayers.get(nextIndex).name;
                    System.out.println("[DEBUG] Skipping to next-next player after DRAW_TWO: " + nextPlayer);
                    ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
                    currentPlayerName = nextPlayer;
                    
                    // Don't call updateGameState here to avoid recursion
                    Platform.runLater(() -> notifyTurnChanged());
                }
                return result;
            } else if (playedCard.getType() == Types.WILD) {
                cardInfo = playedCard.getColor() + "_wild";
                // Update color in DB
                ThreadLobbyManager.updateCurrentColor(lobbyCode, getCurrentColor());
            } else if (playedCard.getType() == Types.DRAW_FOUR) {
                cardInfo = playedCard.getColor() + "_wild_four";
                
                // First increment turnNumber for consistent tracking 
                turnNumber++;
                
                // Mark the current turn as unhandled so the next player is forced to draw
                ThreadLobbyManager.markForcedDrawHandled(lobbyCode, turnNumber);
                
                // Update color in DB
                ThreadLobbyManager.updateCurrentColor(lobbyCode, getCurrentColor());
                
                // Record the move with the current turn number
                ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, cardInfo, "play", turnNumber);
                ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, getPlayers().get(0).getHand().size());
                
                // Skip to next-next player (since next player will draw 4 and lose turn)
                List<ThreadLobbyManager.PlayerInfo> dbPlayers = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
                int currentIndex = -1;
                for (int i = 0; i < dbPlayers.size(); i++) {
                    if (dbPlayers.get(i).name.equals(currentPlayerName)) {
                        currentIndex = i;
                        break;
                    }
                }
                
                if (currentIndex != -1) {
                    boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
                    setCustomOrderClockwise(dbDirection);
                    
                    int nextIndex;
                    if (dbDirection) {
                        nextIndex = (currentIndex + 2) % dbPlayers.size();
                    } else {
                        nextIndex = (currentIndex - 2 + dbPlayers.size()) % dbPlayers.size();
                    }
                    
                    String nextPlayer = dbPlayers.get(nextIndex).name;
                    System.out.println("[DEBUG] Skipping to next-next player after WILD_DRAW_FOUR: " + nextPlayer);
                    ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
                    currentPlayerName = nextPlayer;
                    
                    Platform.runLater(() -> notifyTurnChanged());
                }
                return result;
            } else {
                cardInfo = playedCard.getColor() + "_" + playedCard.getValue();
            }
            
            ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, getPlayers().get(0).getHand().size());
            ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, cardInfo, "play", ++turnNumber);
            
            // Set current player to the next player using the latest direction from the DB
            List<ThreadLobbyManager.PlayerInfo> dbPlayers = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
            int currentIndex = -1;
            for (int i = 0; i < dbPlayers.size(); i++) {
                if (dbPlayers.get(i).name.equals(currentPlayerName)) {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex != -1) {
                boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
                setCustomOrderClockwise(dbDirection);
                
                int nextIndex;
                if (playedCard.getType() == Types.SKIP) {
                    // Skip logic: advance by two (skip next player)
                    if (dbDirection) {
                        nextIndex = (currentIndex + 2) % dbPlayers.size();
                    } else {
                        nextIndex = (currentIndex - 2 + dbPlayers.size()) % dbPlayers.size();
                    }
                    System.out.println("[DEBUG] Skip card played, advancing to player at index: " + nextIndex);
                } else if (playedCard.getType() == Types.REVERSE && dbPlayers.size() > 2) {
                    // For reverse in 3+ players, first change direction, then calculate next player
                    // The next player should be the same player that was before the current player
                    if (dbDirection) {
                        nextIndex = (currentIndex - 1 + dbPlayers.size()) % dbPlayers.size();
                    } else {
                        nextIndex = (currentIndex + 1) % dbPlayers.size();
                    }
                } else {
                    if (dbDirection) {
                        nextIndex = (currentIndex + 1) % dbPlayers.size();
                    } else {
                        nextIndex = (currentIndex - 1 + dbPlayers.size()) % dbPlayers.size();
                    }
                }
                
                String nextPlayer = dbPlayers.get(nextIndex).name;
                System.out.println("[DEBUG] Advancing turn to: " + nextPlayer);
                ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
                currentPlayerName = nextPlayer;
                
                // Don't call updateGameState here to avoid recursion
                Platform.runLater(() -> notifyTurnChanged());
            }
        }
        return result;
    }

    private void notifyTurnChanged() {
        // This method will be called when the turn changes
        // The GameController will listen for these changes and update the UI accordingly
        System.out.println("Turn changed notification sent");
    }

    public void updateGameState() {
        // First, update current player from database
        String dbCurrentPlayer = ThreadLobbyManager.getCurrentPlayer(lobbyCode);
        if (dbCurrentPlayer != null) {
            currentPlayerName = dbCurrentPlayer;
            System.out.println("[DEBUG] Current player from DB: " + currentPlayerName + ", local player: " + localPlayerName);
        }

        // Get all game moves
        List<ThreadLobbyManager.MoveInfo> moves = ThreadLobbyManager.getGameMoves(lobbyCode);
        
        // Find the most recent play action
        ThreadLobbyManager.MoveInfo lastPlay = null;
        for (int i = moves.size() - 1; i >= 0; i--) {
            if (moves.get(i).action.equals("play")) {
                lastPlay = moves.get(i);
                System.out.println("[DEBUG] Last play: " + lastPlay.playerName + " played " + lastPlay.cardPlayed + " on turn " + lastPlay.turnNumber);
                break;
            }
        }

        // Update game direction from database
        boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
        if (isCustomOrderClockwise() != dbDirection) {
            setCustomOrderClockwise(dbDirection);
        }

        // Sync the discard pile with the database
        if (lastPlay != null) {
            syncDiscardPile(lastPlay);
        }

        // Check for forced draws (special cards) - always check, not just when it's our turn
        if (lastPlay != null) {
            String cardPlayed = lastPlay.cardPlayed;
            System.out.println("[DEBUG] Checking for special card effects: " + cardPlayed);
            
            // Handle DRAW_TWO
            if (cardPlayed.endsWith("_draw2")) {
                handleDrawTwo(lastPlay);
            }
            // Handle WILD_DRAW_FOUR
            else if (cardPlayed.contains("wild_four")) {
                handleDrawFour(lastPlay);
            }
            // Handle SKIP - no changes needed
            else if (cardPlayed.endsWith("_skip")) {
                handleSkip(lastPlay);
            }
        }

        // Sync hand sizes
        syncHandSizes();
    }

    private void handleDrawTwo(ThreadLobbyManager.MoveInfo lastPlay) {
        // Check if already handled
        if (ThreadLobbyManager.isForcedDrawHandled(lobbyCode, lastPlay.turnNumber)) {
            System.out.println("[DEBUG] DRAW_TWO already handled for turn: " + lastPlay.turnNumber);
            return;
        }

        // Check if this player is the target of the draw
        // If the previous player played the DRAW_TWO, then this player should draw
        List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        int targetIndex = -1;
        int sourceIndex = -1;
        
        // Find the player who played the DRAW_TWO
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(lastPlay.playerName)) {
                sourceIndex = i;
                break;
            }
        }
        
        // Find the player who should draw (the player after the one who played)
        if (sourceIndex != -1) {
            boolean direction = ThreadLobbyManager.getGameDirection(lobbyCode);
            if (direction) {
                targetIndex = (sourceIndex + 1) % players.size();
            } else {
                targetIndex = (sourceIndex - 1 + players.size()) % players.size();
            }
            
            // If this player is not the target, do nothing
            if (!players.get(targetIndex).name.equals(localPlayerName)) {
                System.out.println("[DEBUG] Not the target of DRAW_TWO, skipping draw");
                return;
            }
        }

        System.out.println("[DEBUG] Forced to draw 2 cards due to DRAW_TWO");
        // Draw 2 cards
        for (int i = 0; i < 2; i++) {
            super.playerDrawCard(0);
        }
        
        // Mark as handled
        ThreadLobbyManager.markForcedDrawHandled(lobbyCode, lastPlay.turnNumber);
        
        // Update hand size
        ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, getPlayers().get(0).getHand().size());
        
        // Record the draw
        ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, "", "draw2", lastPlay.turnNumber);
    }

    private void handleSkip(ThreadLobbyManager.MoveInfo lastPlay) {
        // Check if already handled
        if (ThreadLobbyManager.isForcedDrawHandled(lobbyCode, lastPlay.turnNumber)) {
            System.out.println("[DEBUG] SKIP already handled for turn: " + lastPlay.turnNumber);
            return;
        }

        System.out.println("[DEBUG] Skipping turn due to SKIP card");
        // Mark as handled
        ThreadLobbyManager.markForcedDrawHandled(lobbyCode, lastPlay.turnNumber);
        
        // Get current player index
        List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(currentPlayerName)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex != -1) {
            boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
            setCustomOrderClockwise(dbDirection);
            
            // Skip to next-next player
            int nextIndex;
            if (dbDirection) {
                nextIndex = (currentIndex + 2) % players.size();
            } else {
                nextIndex = (currentIndex - 2 + players.size()) % players.size();
            }
            
            String nextPlayer = players.get(nextIndex).name;
            System.out.println("[DEBUG] Skipping to next-next player: " + nextPlayer);
            ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
            currentPlayerName = nextPlayer;
            
            // Don't call updateGameState here to avoid recursion
            Platform.runLater(() -> notifyTurnChanged());
        }
    }

    private void handleDrawFour(ThreadLobbyManager.MoveInfo lastPlay) {
        // Check if already handled
        if (ThreadLobbyManager.isForcedDrawHandled(lobbyCode, lastPlay.turnNumber)) {
            System.out.println("[DEBUG] WILD_DRAW_FOUR already handled for turn: " + lastPlay.turnNumber);
            return;
        }
        
        // Check if this player is the target of the draw
        // If the previous player played the WILD_DRAW_FOUR, then this player should draw
        List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        int targetIndex = -1;
        int sourceIndex = -1;
        
        // Find the player who played the WILD_DRAW_FOUR
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(lastPlay.playerName)) {
                sourceIndex = i;
                break;
            }
        }
        
        // Find the player who should draw (the player after the one who played)
        if (sourceIndex != -1) {
            boolean direction = ThreadLobbyManager.getGameDirection(lobbyCode);
            if (direction) {
                targetIndex = (sourceIndex + 1) % players.size();
            } else {
                targetIndex = (sourceIndex - 1 + players.size()) % players.size();
            }
            
            // If this player is not the target, do nothing
            if (!players.get(targetIndex).name.equals(localPlayerName)) {
                System.out.println("[DEBUG] Not the target of WILD_DRAW_FOUR, skipping draw");
                return;
            }
        }

        System.out.println("[DEBUG] Forced to draw 4 cards due to WILD_DRAW_FOUR");
        // Draw 4 cards
        for (int i = 0; i < 4; i++) {
            super.playerDrawCard(0);
        }
        
        // Mark as handled
        ThreadLobbyManager.markForcedDrawHandled(lobbyCode, lastPlay.turnNumber);
        
        // Update hand size
        ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, getPlayers().get(0).getHand().size());
        
        // Record the draw
        ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, "", "draw4", lastPlay.turnNumber);
    }

    private void syncDiscardPile(ThreadLobbyManager.MoveInfo lastPlay) {
        try {
            System.out.println("[DEBUG] Syncing discard pile with card: " + lastPlay.cardPlayed);
            
            String[] cardParts = lastPlay.cardPlayed.split("_");
            if (cardParts.length >= 2) {
                Colors cardColor = Colors.valueOf(cardParts[0].toUpperCase());
                String cardType;
                
                // Special handling for wild_four which may have multiple parts
                if (lastPlay.cardPlayed.contains("wild_four")) {
                    cardType = "wild_four";
                    System.out.println("[DEBUG] Detected wild_four card, setting type and color");
                } else {
                    cardType = cardParts[1].toLowerCase();
                }
                
                AbstractCard newCard = null;

                if (cardType.equals("draw2")) {
                    newCard = new CardAction(cardColor, Types.DRAW_TWO);
                } else if (cardType.equals("skip")) {
                    newCard = new CardAction(cardColor, Types.SKIP);
                } else if (cardType.equals("reverse")) {
                    newCard = new CardAction(cardColor, Types.REVERSE);
                } else if (cardType.equals("wild")) {
                    newCard = new CardAction(Colors.WILD, Types.WILD);
                    newCard.setColor(cardColor);
                } else if (cardType.equals("wild_four")) {
                    newCard = new CardAction(Colors.WILD, Types.DRAW_FOUR);
                    newCard.setColor(cardColor);
                    System.out.println("[DEBUG] Created WILD_DRAW_FOUR card with color: " + cardColor);
                } else {
                    try {
                        int num = Integer.parseInt(cardType);
                        newCard = new CardNumber(cardColor, num);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERROR] Unknown card type: " + cardType);
                    }
                }

                if (newCard != null) {
                    System.out.println("[DEBUG] Adding card to discard pile: " + newCard.getType() + " " + newCard.getColor());
                    getDiscardPile().clear();
                    getDiscardPile().add(newCard);
                    Platform.runLater(() -> notifyTurnChanged());
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] Invalid card info: " + lastPlay.cardPlayed);
            e.printStackTrace();
        }
    }

    private void syncHandSizes() {
        List<ThreadLobbyManager.PlayerInfo> playerInfos = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        for (int i = 0; i < getPlayers().size(); i++) {
            AbstractPlayer player = getPlayers().get(i);
            String playerName = player.getName();
            if (playerName.equals(localPlayerName)) {
                continue; // Don't overwrite local player's hand
            }
            
            // Update opponent hand sizes
            for (ThreadLobbyManager.PlayerInfo info : playerInfos) {
                if (info.name.equals(playerName)) {
                    player.getHand().clear();
                    for (int j = 0; j < info.handSize; j++) {
                        player.getHand().add(new CardNumber(Colors.RED, 0)); // Placeholder
                    }
                    System.out.println("[DEBUG] Synced opponent hand size: " + playerName + " = " + info.handSize);
                    break;
                }
            }
        }
    }

    private void advanceToNextPlayer() {
        List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(currentPlayerName)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex != -1) {
            boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
            setCustomOrderClockwise(dbDirection);
            
            int nextIndex;
            if (dbDirection) {
                nextIndex = (currentIndex + 1) % players.size();
            } else {
                nextIndex = (currentIndex - 1 + players.size()) % players.size();
            }
            
            String nextPlayer = players.get(nextIndex).name;
            System.out.println("[DEBUG] Advancing turn to: " + nextPlayer);
            ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
            currentPlayerName = nextPlayer;
            
            // Don't call updateGameState here to avoid recursion
            Platform.runLater(() -> notifyTurnChanged());
        }
    }

    private void skipToNextNextPlayer() {
        List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(currentPlayerName)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex != -1) {
            boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
            setCustomOrderClockwise(dbDirection);
            
            int nextIndex;
            if (dbDirection) {
                nextIndex = (currentIndex + 1) % players.size();
                nextIndex = (nextIndex + 1) % players.size(); // skip one more
            } else {
                nextIndex = (currentIndex - 1 + players.size()) % players.size();
                nextIndex = (nextIndex - 1 + players.size()) % players.size(); // skip one more
            }
            
            String nextPlayer = players.get(nextIndex).name;
            System.out.println("[DEBUG] Skipping to next-next player: " + nextPlayer);
            ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
            currentPlayerName = nextPlayer;
            
            // Don't call updateGameState here to avoid recursion
            Platform.runLater(() -> notifyTurnChanged());
        }
    }

    @Override
    public void playerDrawCard(int playerIndex) {
        // Check if it's the local player's turn
        if (!isPlayersTurn(0)) {
            System.out.println("Not your turn!");
            return;
        }

        // Draw a single card
        super.playerDrawCard(playerIndex);
        
        AbstractPlayer currentPlayer = getPlayers().get(playerIndex);
        
        // Update player's hand size in database
        ThreadLobbyManager.updatePlayerHandSize(lobbyCode, currentPlayer.getName(), currentPlayer.getHand().size());
        
        // Record the draw action in game_moves
        ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, "", "draw", ++turnNumber);
        
        // Force a UI update to reflect the hand size change
        Platform.runLater(() -> {
            notifyTurnChanged();
        });
    }

    protected void handleSpecialCard(AbstractCard card) {
        // Do nothing. All special card logic is handled in playCard for multiplayer.
    }

    public String getLobbyCode() {
        return lobbyCode;
    }

    public boolean isHost() {
        return isHost;
    }

    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    public int getTurnNumber() {
        return turnNumber;
    }
} 