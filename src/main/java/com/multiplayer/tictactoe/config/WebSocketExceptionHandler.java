package com.multiplayer.tictactoe.config;

import com.multiplayer.tictactoe.dto.response.ErrorResponse;
import com.multiplayer.tictactoe.exception.GameException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(GameException.class)
    public void handleGameException(GameException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorType(), ex.getMessage());
        messagingTemplate.convertAndSend("/topic/session/" + ex.getSessionId(), errorResponse);
    }
}
