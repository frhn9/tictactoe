package com.multiplayer.tictactoe.controller;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.request.MakeMoveReq;
import com.multiplayer.tictactoe.entity.jpa.User;
import com.multiplayer.tictactoe.service.GameService;
import com.multiplayer.tictactoe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;
    private final UserService userService;

    /**
     * Single endpoint for both creating and joining games
     */
    @MessageMapping("/game.createOrJoin")
    public void createOrJoinGame(CreateOrJoinGameReq request) {
        User user = userService.findOrCreateUser(request.getSessionId(), request.getDeviceId());
        gameService.createOrJoinGame(request, user.getId().toString());
    }

    @MessageMapping("/game.makeMove")
    public void makeMove(MakeMoveReq request) {
        gameService.makeMove(request);
    }
}
