package com.multiplayer.tictactoe.enums;

/**
 * <p>Defines the possible states for an active game stored in Redis.</p>
 * <br>
 *
 * @implNote
 * <p>WAITING -> Game is already created and waiting for a second player to join.</p>
 * <p>IN_PROGRESS -> Both players have joined and game currently is in progress</p>
 * <p>X_WON -> Game has been completed, Player X has won.</p>
 * <p>O_WON -> Game has been completed, Player O has won.</p>
 * <p>DRAW -> Game has been completed and resulted in draw.</p>
 * <p>CANCELLED -> Game has been cancelled.</p>
 */
public enum GameStatus {
    WAITING,
    IN_PROGRESS,
    X_WON,
    O_WON,
    DRAW,
    CANCELLED
}