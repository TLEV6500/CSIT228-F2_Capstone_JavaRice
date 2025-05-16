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

        // Check if this player was just forced to draw from a WILD_DRAW4
        List<ThreadLobbyManager.MoveInfo> moves = ThreadLobbyManager.getGameMoves(lobbyCode);
        boolean wasForcedToDraw = false;
        int lastPlayTurn = -1;
        int lastDraw4Turn = -1;
        for (int i = moves.size() - 1; i >= 0; i--) {
            ThreadLobbyManager.MoveInfo move = moves.get(i);
            if (move.action.equals("draw4") && move.playerName.equals(localPlayerName)) {
                // This player was forced to draw, they can't play a card
                System.out.println("[DEBUG] Cannot play card after being forced to draw from WILD_DRAW4");
                wasForcedToDraw = true;
                lastDraw4Turn = move.turnNumber;
                break;
            }
            if (move.action.equals("play") && move.playerName.equals(localPlayerName)) {
                // Found the last play by this player, stop checking
                lastPlayTurn = move.turnNumber;
                break;
            }
        }
        if (wasForcedToDraw) {
            System.out.println("[DEBUG] Last play turn: " + lastPlayTurn + ", Last draw4 turn: " + lastDraw4Turn);
            return false;
        }

        // Get the card to play
        AbstractCard topCard = getTopCard();
        AbstractCard cardToPlay = getPlayers().get(0).getHand().get(cardIndex);

        // Check if trying to play WILD DRAW FOUR
        if (cardToPlay.getType() == Types.DRAW_FOUR) {
            // Check if player has any valid cards they could play instead
            boolean hasValidCard = false;
            for (AbstractCard card : getPlayers().get(0).getHand()) {
                if (card != cardToPlay && card.canPlayOn(topCard)) {
                    hasValidCard = true;
                    System.out.println("[DEBUG] Cannot play WILD_DRAW4 - have valid card: " + card);
                    break;
                }
            }
            if (hasValidCard) {
                return false;
            }
            System.out.println("[DEBUG] Playing WILD_DRAW4 - no valid cards available");
        }

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
            } else if (playedCard.getType() == Types.SKIP) {
                cardInfo = playedCard.getColor() + "_skip";
            } else if (playedCard.getType() == Types.DRAW_TWO) {
                cardInfo = playedCard.getColor() + "_draw2";
                // Mark this DRAW_TWO as handled immediately with the current turn number
                ThreadLobbyManager.markForcedDrawHandled(lobbyCode, turnNumber);
            } else if (playedCard.getType() == Types.WILD) {
                // Update color in DB before recording the move
                ThreadLobbyManager.updateCurrentColor(lobbyCode, getCurrentColor());
                // Wait for DB confirmation
                int retries = 0;
                while (retries < 10) {
                    Colors dbColor = ThreadLobbyManager.getCurrentColor(lobbyCode);
                    if (dbColor == getCurrentColor()) break;
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    retries++;
                }
                cardInfo = "wild";
            } else if (playedCard.getType() == Types.DRAW_FOUR) {
                // Update color in DB before recording the move
                ThreadLobbyManager.updateCurrentColor(lobbyCode, getCurrentColor());
                // Wait for DB confirmation
                int retries = 0;
                while (retries < 10) {
                    Colors dbColor = ThreadLobbyManager.getCurrentColor(lobbyCode);
                    if (dbColor == getCurrentColor()) break;
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    retries++;
                }
                cardInfo = "wild_draw4";
                // Don't mark as handled here - let updateGameState handle it when the next player draws
                System.out.println("[DEBUG] WILD_DRAW4 played, waiting for next player to draw");
            } else {
                cardInfo = playedCard.getColor() + "_" + playedCard.getValue();
            }
            
            ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, getPlayers().get(0).getHand().size());
            ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, cardInfo, "play", ++turnNumber);
            
            // Set current player to the next player using the latest direction from the DB
            List<ThreadLobbyManager.PlayerInfo> dbPlayers = ThreadLobbyManager.getPlayersInLobby(lobbyCode); // Always use DB order
            int currentIndex = -1;
            for (int i = 0; i < dbPlayers.size(); i++) {
                if (dbPlayers.get(i).name.equals(currentPlayerName)) {
                    currentIndex = i;
                    break;
                }
            }
            final int finalCurrentIndex = currentIndex;
            final boolean finalNewDirection = newDirection;
            Runnable advanceTurn = () -> {
                boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
                setCustomOrderClockwise(dbDirection); // Only set from DB
                int nextIndex;
                if (playedCard.getType() == Types.SKIP) {
                    // Skip logic: advance by two (skip next player)
                    if (dbPlayers.size() == 2) {
                        nextIndex = finalCurrentIndex; // In 2-player, skip returns to self
                    } else if (dbDirection) {
                        nextIndex = (finalCurrentIndex + 2) % dbPlayers.size();
                    } else {
                        nextIndex = (finalCurrentIndex - 2 + dbPlayers.size()) % dbPlayers.size();
                    }
                } else if (playedCard.getType() == Types.DRAW_FOUR) {
                    // Wild Draw 4: advance to next player (they will be forced to draw)
                    if (dbDirection) {
                        nextIndex = (finalCurrentIndex + 1) % dbPlayers.size();
                    } else {
                        nextIndex = (finalCurrentIndex - 1 + dbPlayers.size()) % dbPlayers.size();
                    }
                    System.out.println("[DEBUG] WILD_DRAW4: Advancing to next player for forced draw");
                } else {
                    if (dbDirection) {
                        nextIndex = (finalCurrentIndex + 1) % dbPlayers.size();
                    } else {
                        nextIndex = (finalCurrentIndex - 1 + dbPlayers.size()) % dbPlayers.size();
                    }
                }
                String nextPlayer = dbPlayers.get(nextIndex).name;
                System.out.println("[DEBUG] Advancing turn to: " + nextPlayer);
                ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
                currentPlayerName = nextPlayer;
                Platform.runLater(() -> {
                    notifyTurnChanged();
                    updateGameState();
                });
            };
            if (reversePlayed) {
                // Poll the DB until the new direction is confirmed
                new Thread(() -> {
                    int retries = 0;
                    boolean confirmed = false;
                    while (retries < 10) {
                        boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
                        if (dbDirection == finalNewDirection) {
                            confirmed = true;
                            break;
                        }
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                        retries++;
                    }
                    // Now, on the JavaFX thread, continue with the next move (including draw2 logic)
                    Platform.runLater(advanceTurn);
                }).start();
            } else {
                advanceTurn.run();
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
        // --- WILD DRAW FOUR LOGIC (AGGRESSIVE) ---
        // Check for unhandled WILD_DRAW4 move targeting this player FIRST
        List<ThreadLobbyManager.MoveInfo> moves = ThreadLobbyManager.getGameMoves(lobbyCode);
        int wildDraw4ToDraw = 0;
        int wildDraw4TurnToHandle = -1;
        for (int i = moves.size() - 1; i >= 0; i--) {
            ThreadLobbyManager.MoveInfo move = moves.get(i);
            if (move.action.equals("draw4")) {
                // Only the player whose turn is immediately after the draw4 move should draw
                String draw4Player = move.playerName;
                String myName = localPlayerName;
                List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
                int draw4Index = -1;
                int myIndex = -1;
                for (int j = 0; j < players.size(); j++) {
                    if (players.get(j).name.equals(draw4Player)) draw4Index = j;
                    if (players.get(j).name.equals(myName)) myIndex = j;
                }
                boolean isNextPlayer = false;
                if (draw4Index != -1 && myIndex != -1) {
                    if (isCustomOrderClockwise()) {
                        isNextPlayer = ((draw4Index + 1) % players.size()) == myIndex;
                    } else {
                        isNextPlayer = ((draw4Index - 1 + players.size()) % players.size()) == myIndex;
                    }
                }
                // Double-check if the draw is still unhandled right before processing
                if (isNextPlayer && !ThreadLobbyManager.isForcedDrawHandled(lobbyCode, move.turnNumber)) {
                    // Try to mark as handled first to prevent other players from processing it
                    ThreadLobbyManager.markForcedDrawHandled(lobbyCode, move.turnNumber);
                    // Double-check if we were the first to mark it
                if (!ThreadLobbyManager.isForcedDrawHandled(lobbyCode, move.turnNumber)) {
                        // Another player beat us to it, skip processing
                        break;
                    }
                    wildDraw4ToDraw = 4;
                    wildDraw4TurnToHandle = move.turnNumber;
                }
                break; // Only one WILD_DRAW4 can be active at a time (standard UNO)
            } else if (
                (move.action.equals("play") && move.playerName.equals(localPlayerName)) ||
                (move.action.equals("draw") && move.playerName.equals(localPlayerName))
            ) {
                break;
            }
        }
        if (wildDraw4ToDraw > 0 && wildDraw4TurnToHandle != -1) {
            System.out.println("[DEBUG] Forced to draw 4 cards due to WILD_DRAW4 (aggressive)");
            for (int i = 0; i < wildDraw4ToDraw; i++) {
                super.playerDrawCard(0);
            }
            ThreadLobbyManager.markForcedDrawHandled(lobbyCode, wildDraw4TurnToHandle);
            System.out.println("[DEBUG] Marked forced draw handled for WILD_DRAW4 turn: " + wildDraw4TurnToHandle);
            int newHandSize = getPlayers().get(0).getHand().size();
            ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, newHandSize);
            System.out.println("[DEBUG] Updated hand size in DB: " + newHandSize);
            // Record the draw4 action
            ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, "", "draw4", wildDraw4TurnToHandle);
            System.out.println("[DEBUG] Recorded draw4 action for turn: " + wildDraw4TurnToHandle);
            
            // Skip this player's turn (advance to next-next player)
            List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
            int currentIndex = -1;
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).name.equals(localPlayerName)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex != -1) {
                // Get the latest game direction from DB
                boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
                setCustomOrderClockwise(dbDirection);
                
                int nextIndex;
                if (dbDirection) {
                    nextIndex = (currentIndex + 1) % players.size();
                    nextIndex = (nextIndex + 1) % players.size(); // skip
                } else {
                    nextIndex = (currentIndex - 1 + players.size()) % players.size();
                    nextIndex = (nextIndex - 1 + players.size()) % players.size(); // skip
                }
                String nextPlayer = players.get(nextIndex).name;
                System.out.println("[DEBUG] Advancing turn after WILD_DRAW4 from " + localPlayerName + " to " + nextPlayer);
                ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
                currentPlayerName = nextPlayer;
                Platform.runLater(() -> {
                    notifyTurnChanged();
                    updateGameState();
                });
            }
            return; // Do not process anything else until forced draw is handled
        }

        // Update the current player from the database
        String dbCurrentPlayer = ThreadLobbyManager.getCurrentPlayer(lobbyCode);
        if (dbCurrentPlayer != null) {
            currentPlayerName = dbCurrentPlayer;
            System.out.println("[DEBUG] Current player from DB: " + currentPlayerName);
            // Update the current player index
            for (int i = 0; i < getPlayers().size(); i++) {
                if (getPlayers().get(i).getName().equals(currentPlayerName)) {
                    setCurrentPlayer(currentPlayerName);
                    System.out.println("[DEBUG] Set current player index to: " + i);
                    break;
                }
            }
        }

        // Update game direction from database
        boolean dbDirection = ThreadLobbyManager.getGameDirection(lobbyCode);
        if (isCustomOrderClockwise() != dbDirection) {
            setCustomOrderClockwise(dbDirection);
            System.out.println("[DEBUG] Updated game direction from DB: " + (dbDirection ? "clockwise" : "counterclockwise"));
        }

        // Sync the discard pile with the database
        if (!moves.isEmpty()) {
            // Find the most recent play action
            for (int i = moves.size() - 1; i >= 0; i--) {
                ThreadLobbyManager.MoveInfo move = moves.get(i);
                if (move.action.equals("play")) {
                    String[] cardParts = move.cardPlayed.split("_");
                    if (cardParts.length >= 2) {
                        try {
                            Colors cardColor = Colors.valueOf(cardParts[0]);
                            String cardType = cardParts[1];
                            AbstractCard topCard = getTopCard();
                            AbstractCard newCard = null;
                            boolean isAction = false;
                            if (cardType.equals("draw2")) {
                                newCard = new CardAction(cardColor, Types.DRAW_TWO);
                                isAction = true;
                            } else if (cardType.equals("skip")) {
                                newCard = new CardAction(cardColor, Types.SKIP);
                                isAction = true;
                            } else if (cardType.equals("reverse")) {
                                newCard = new CardAction(cardColor, Types.REVERSE);
                                isAction = true;
                            } else if (cardType.equals("wild")) {
                                newCard = new CardAction(Colors.WILD, Types.WILD);
                                isAction = true;
                            } else if (cardType.equals("draw4")) {
                                newCard = new CardAction(Colors.WILD, Types.DRAW_FOUR);
                                isAction = true;
                            } else {
                                try {
                                    int num = Integer.parseInt(cardType);
                                    newCard = new CardNumber(cardColor, num);
                                } catch (NumberFormatException e) {
                                    System.out.println("[ERROR] Unknown card type in database: " + cardType);
                                }
                            }
                            // Only update if there's a mismatch and the newCard is valid
                            if (newCard != null && (topCard == null || !topCard.getColor().equals(newCard.getColor()) || !topCard.getType().equals(newCard.getType()) || (newCard instanceof CardNumber && topCard instanceof CardNumber && ((CardNumber)newCard).getValue() != ((CardNumber)topCard).getValue()))) {
                                System.out.println("[DEBUG] Syncing discard pile with DB: " + move.cardPlayed);
                                getDiscardPile().clear();
                                getDiscardPile().add(newCard);
                                System.out.println("[DEBUG] Added card to discard pile: " + newCard);
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("[ERROR] Invalid card info in database: " + move.cardPlayed);
                        }
                    }
                    break;
                }
            }
        }

        // Only proceed if it's the local player's turn
        if (!currentPlayerName.equals(localPlayerName)) {
            System.out.println("[DEBUG] Not local player's turn, skipping draw check");
            return;
        }

        // Find the most recent play action by the local player
        int lastPlayTurn = -1;
        for (int i = moves.size() - 1; i >= 0; i--) {
            ThreadLobbyManager.MoveInfo move = moves.get(i);
            if (move.playerName.equals(localPlayerName) && move.action.equals("play")) {
                lastPlayTurn = move.turnNumber;
                break;
            }
        }

        // Check if there was a manual draw after the last play
        boolean hasManualDraw = false;
        for (int i = moves.size() - 1; i >= 0; i--) {
            ThreadLobbyManager.MoveInfo move = moves.get(i);
            if (move.playerName.equals(localPlayerName) && move.action.equals("draw")) {
                if (move.turnNumber > lastPlayTurn) {
                    hasManualDraw = true;
                    System.out.println("[DEBUG] Found manual draw after last play");
                    break;
                }
            }
        }
        if (hasManualDraw) {
            System.out.println("[DEBUG] Player has already drawn manually this turn, skipping forced draw");
            return;
        }

        // --- STACKED DRAW_TWO LOGIC ---
        // Only process forced draw if it's this player's turn
        if (!currentPlayerName.equals(localPlayerName)) {
            System.out.println("[DEBUG] Not this player's turn for forced DRAW_TWO, skipping forced draw");
            return;
        }
        // Accumulate all consecutive unhandled DRAW_TWO moves since the last play by this player
        int cardsToDraw = 0;
        List<Integer> draw2TurnsToHandle = new ArrayList<>();
        int lastDraw2Turn = -1;
        boolean alreadyHandledThisStack = false;
        String stackInitiator = null;
        for (int i = moves.size() - 1; i >= 0; i--) {
            ThreadLobbyManager.MoveInfo move = moves.get(i);
            if (move.action.equals("draw2")) {
                // If this move is by the local player, stop stacking
                if (move.playerName.equals(localPlayerName)) {
                    break;
                }
                // Only stack if not handled
                if (!ThreadLobbyManager.isForcedDrawHandled(lobbyCode, move.turnNumber)) {
                    cardsToDraw += 2;
                    draw2TurnsToHandle.add(move.turnNumber);
                    lastDraw2Turn = move.turnNumber;
                    if (stackInitiator == null) stackInitiator = move.playerName;
                } else {
                    alreadyHandledThisStack = true;
                    break; // Stop at the first handled one
                }
            } else if (
                (move.action.equals("play") && move.playerName.equals(localPlayerName)) ||
                (move.action.equals("draw") && move.playerName.equals(localPlayerName))
            ) {
                break; // Stop at the last play or draw by this player
            }
        }

        if (cardsToDraw > 0 && !alreadyHandledThisStack) {
            // Check if the player can stack another DRAW_TWO
            boolean canStack = false;
            for (AbstractCard card : getPlayers().get(0).getHand()) {
                if (card.getType() == Types.DRAW_TWO) {
                    canStack = true;
                    break;
                }
            }
            if (!canStack) {
                System.out.println("[DEBUG] Forced to draw " + cardsToDraw + " cards due to stacked DRAW_TWO");
                for (int i = 0; i < cardsToDraw; i++) {
                    super.playerDrawCard(0);
                }
                // Mark all involved DRAW_TWO as handled
                for (int turn : draw2TurnsToHandle) {
                    ThreadLobbyManager.markForcedDrawHandled(lobbyCode, turn);
                    System.out.println("[DEBUG] Marked forced draw handled for turn: " + turn);
                }
                // Update hand size in database
                int newHandSize = getPlayers().get(0).getHand().size();
                ThreadLobbyManager.updatePlayerHandSize(lobbyCode, localPlayerName, newHandSize);
                System.out.println("[DEBUG] Updated hand size in DB: " + newHandSize);
                // Record a single draw2 action for the total stack, using the last DRAW_TWO's turn number
                ThreadLobbyManager.recordGameMove(lobbyCode, localPlayerName, "", "draw2", lastDraw2Turn);
                System.out.println("[DEBUG] Recorded draw2 action for turn: " + lastDraw2Turn + ", stack initiated by: " + stackInitiator);
                // Move to next player after drawing
                List<ThreadLobbyManager.PlayerInfo> players = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
                int currentIndex = -1;
                for (int i = 0; i < players.size(); i++) {
                    if (players.get(i).name.equals(localPlayerName)) {
                        currentIndex = i;
                        break;
                    }
                }
                if (currentIndex != -1) {
                    int nextIndex;
                    if (isCustomOrderClockwise()) {
                        nextIndex = (currentIndex + 1) % players.size();
                    } else {
                        nextIndex = (currentIndex - 1 + players.size()) % players.size();
                    }
                    String nextPlayer = players.get(nextIndex).name;
                    ThreadLobbyManager.setCurrentPlayer(lobbyCode, nextPlayer);
                    currentPlayerName = nextPlayer;
                    Platform.runLater(() -> {
                        notifyTurnChanged();
                        updateGameState();
                    });
                }
            } else {
                System.out.println("[DEBUG] Player can stack another DRAW_TWO, waiting for play");
            }
        } else if (alreadyHandledThisStack) {
            System.out.println("[DEBUG] This DRAW_TWO stack has already been handled, skipping forced draw");
        }

        // Always sync hand size from DB after any move
        List<ThreadLobbyManager.PlayerInfo> playerInfos = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        for (int i = 0; i < getPlayers().size(); i++) {
            AbstractPlayer player = getPlayers().get(i);
            String playerName = player.getName();
            if (playerName.equals(localPlayerName)) {
                // Do NOT overwrite the local player's hand with placeholders
                continue;
            }
            // For opponents, update hand size with placeholders
            int dbHandSize = -1;
            for (ThreadLobbyManager.PlayerInfo info : playerInfos) {
                if (info.name.equals(playerName)) {
                    dbHandSize = info.handSize;
                    break;
                }
            }
            if (dbHandSize >= 0) {
                player.getHand().clear();
                for (int j = 0; j < dbHandSize; j++) {
                    player.getHand().add(new CardNumber(Colors.RED, 0)); // Placeholder
                }
                System.out.println("[DEBUG] Synced opponent hand size from DB: " + playerName + " = " + dbHandSize);
            } else {
                System.out.println("[DEBUG] Could not find player in DB to sync hand size: " + playerName);
            }
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