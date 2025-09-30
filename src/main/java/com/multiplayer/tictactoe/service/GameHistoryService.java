package com.multiplayer.tictactoe.service;

import com.multiplayer.tictactoe.entity.jpa.GameHistory;
import com.multiplayer.tictactoe.enums.GameStatus;

import java.util.List;

public interface GameHistoryService {
    GameHistory saveGameHistory(String gameId, String userIdX, String userIdO, GameStatus status, 
                               int boardVerticalSize, int boardHorizontalSize, String boardState, 
                               String winnerId, Long durationMs);
    List<GameHistory> getGameHistoryByUserId(String userId);
    List<GameHistory> getGameHistoryByStatus(GameStatus status);
    List<GameHistory> getGameHistoryByUsers(String userIdX, String userIdO);
    GameHistory getGameHistoryById(Long id);
}