package com.multiplayer.tictactoe.dto.response;

import com.multiplayer.tictactoe.exception.GameErrorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String errorCode;
    private String message;

    public ErrorResponse(GameErrorType errorType, String message) {
        this.errorCode = errorType.name();
        this.message = message;
    }

}