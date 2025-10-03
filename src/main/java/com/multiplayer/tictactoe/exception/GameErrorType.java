package com.multiplayer.tictactoe.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GameErrorType {

    GAME_NOT_FOUND("Game not found"),
    GAME_FULL("Game is already full"),
    GAME_NOT_WAITING("Game is not waiting for players"),
    ALREADY_IN_GAME_AS_X("You are already in this game as Player X"),
    ALREADY_IN_GAME_AS_O("You are already in this game as Player O"),
    ILLEGAL_MOVE("Illegal move");

    private final String message;

}