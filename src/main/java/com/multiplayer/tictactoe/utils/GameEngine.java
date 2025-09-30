package com.multiplayer.tictactoe.utils;

import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import com.multiplayer.tictactoe.enums.GameStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GameEngine {

    private static final Integer CONSECUTIVE_COUNT = 3;

    /**
     * Checks if a move is illegal
     */
    public boolean isIllegalMove(ActiveGame game, String playerName, int row, int col) {
        // 1. Check if game is active
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return true;
        }

        // 2. Check if it's player's turn
        if (!isPlayersTurn(game, playerName)) {
            return true;
        }

        // 3. Check if position is valid
        if (!isValidPosition(game, row, col)) {
            return true;
        }

        // 4. Check if position is already occupied
        if (game.isPositionOccupied(row, col)) {
            return true;
        }

        // 5. Check if player is valid for this game
        return !isValidPlayer(game, playerName);
    }

    /**
     * Makes a move on the board
     */
    public void makeMove(ActiveGame game, String playerName, int row, int col) {
        Character playerSymbol = getPlayerSymbol(game, playerName);
        game.setCell(row, col, playerSymbol);

        // Switch turn to the other player
        switchTurn(game);
    }

    /**
     * Checks win condition after a move
     * Returns the winning player symbol or null if no winner
     */
    public Character checkWinCondition(ActiveGame game, int lastMoveRow, int lastMoveCol) {
        Character lastMovePlayer = game.getCell(lastMoveRow, lastMoveCol);
        if (lastMovePlayer == null) return null;

        // Check all possible win conditions
        if (checkRowWin(game, lastMoveRow, lastMovePlayer) ||
                checkColumnWin(game, lastMoveCol, lastMovePlayer) ||
                checkDiagonalWin(game, lastMovePlayer)) {
            return lastMovePlayer;
        }

        return null;
    }

    /**
     * Checks if the game is a draw
     */
    public boolean checkDrawCondition(ActiveGame game) {
        int totalCells = game.getBoardVerticalSize() * game.getBoardHorizontalSize();
        int occupiedCells = game.getAllOccupiedPositions().size();

        return occupiedCells == totalCells;
    }

    private boolean isPlayersTurn(ActiveGame game, String playerName) {
        return playerName.equals(game.getCurrentTurnUserId());
    }

    private boolean isValidPosition(ActiveGame game, int row, int col) {
        return row >= 0 && row < game.getBoardVerticalSize() &&
                col >= 0 && col < game.getBoardHorizontalSize();
    }

    private boolean isValidPlayer(ActiveGame game, String playerName) {
        return playerName.equals(game.getUserIdX()) || playerName.equals(game.getUserIdO());
    }

    private Character getPlayerSymbol(ActiveGame game, String playerName) {
        if (playerName.equals(game.getUserIdX())) return 'X';
        if (playerName.equals(game.getUserIdO())) return 'O';

        throw new IllegalArgumentException("Player not in this game: " + playerName);
    }

    private void switchTurn(ActiveGame game) {
        String currentTurn = game.getCurrentTurnUserId();
        if (currentTurn.equals(game.getUserIdX())) {
            game.setCurrentTurnUserId(game.getUserIdO());
        } else {
            game.setCurrentTurnUserId(game.getUserIdX());
        }
    }

    private boolean checkRowWin(ActiveGame game, int row, Character player) {
        Set<String> playerPositions = game.getPlayerPositions(player);
        int consecutiveCount = 0;

        for (int col = 0; col < game.getBoardHorizontalSize(); col++) {
            if (playerPositions.contains(row + "," + col)) {
                consecutiveCount++;
                if (consecutiveCount == CONSECUTIVE_COUNT) return true;
            } else {
                consecutiveCount = 0;
            }
        }
        return false;
    }

    private boolean checkColumnWin(ActiveGame game, int col, Character player) {
        Set<String> playerPositions = game.getPlayerPositions(player);
        int consecutiveCount = 0;

        for (int row = 0; row < game.getBoardVerticalSize(); row++) {
            if (playerPositions.contains(row + "," + col)) {
                consecutiveCount++;
                if (consecutiveCount == CONSECUTIVE_COUNT) return true;
            } else {
                consecutiveCount = 0;
            }
        }
        return false;
    }

    private boolean checkDiagonalWin(ActiveGame game, Character player) {
        return checkMainDiagonalWin(game, player) || checkAntiDiagonalWin(game, player);
    }

    private boolean checkMainDiagonalWin(ActiveGame game, Character player) {
        Set<String> playerPositions = game.getPlayerPositions(player);
        int consecutiveCount = 0;
        int size = Math.min(game.getBoardVerticalSize(), game.getBoardHorizontalSize());

        for (int i = 0; i < size; i++) {
            if (playerPositions.contains(i + "," + i)) {
                consecutiveCount++;
                if (consecutiveCount == CONSECUTIVE_COUNT) return true;
            } else {
                consecutiveCount = 0;
            }
        }
        return false;
    }

    private boolean checkAntiDiagonalWin(ActiveGame game, Character player) {
        Set<String> playerPositions = game.getPlayerPositions(player);
        int consecutiveCount = 0;
        int rows = game.getBoardVerticalSize();
        int cols = game.getBoardHorizontalSize();
        int size = Math.min(rows, cols);

        for (int i = 0; i < size; i++) {
            if (playerPositions.contains(i + "," + (cols - 1 - i))) {
                consecutiveCount++;
                if (consecutiveCount == CONSECUTIVE_COUNT) return true;
            } else {
                consecutiveCount = 0;
            }
        }

        return false;
    }
}
