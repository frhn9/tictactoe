package com.multiplayer.tictactoe.service;

import com.multiplayer.tictactoe.entity.jpa.GameHistory;
import com.multiplayer.tictactoe.enums.GameStatus;

public interface GameHistoryService {
    GameHistory saveGameHistory(String gameId, String userIdX, String userIdO, GameStatus status, 
                               int boardVerticalSize, int boardHorizontalSize, String boardState, 
                               String winnerId);
}