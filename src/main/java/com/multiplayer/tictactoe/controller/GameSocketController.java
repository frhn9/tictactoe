package com.multiplayer.tictactoe.controller;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;

    /**
     * Single endpoint for both creating and joining games
     */
    @MessageMapping("/game.createOrJoin")
    public void createOrJoinGame(CreateOrJoinGameReq request) {
        gameService.createOrJoinGame(request);
    }
}
