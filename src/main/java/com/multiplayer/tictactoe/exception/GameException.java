package com.multiplayer.tictactoe.exception;

import lombok.Getter;

@Getter
public class GameException extends RuntimeException {

    private final GameErrorType errorType;
    private final String sessionId;
    
    public GameException(GameErrorType errorType, String sessionId) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.sessionId = sessionId;
    }

}