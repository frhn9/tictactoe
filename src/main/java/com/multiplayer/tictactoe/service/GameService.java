package com.multiplayer.tictactoe.service;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;

public interface GameService {

    void createOrJoinGame(CreateOrJoinGameReq request);

}
