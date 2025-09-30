package com.multiplayer.tictactoe.service;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.request.MakeMoveReq;

public interface GameService {

    void createOrJoinGame(CreateOrJoinGameReq request, String userId);
    
    void makeMove(MakeMoveReq request);

}
