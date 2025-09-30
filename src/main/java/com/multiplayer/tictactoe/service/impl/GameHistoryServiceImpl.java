package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.entity.jpa.GameHistory;
import com.multiplayer.tictactoe.enums.GameStatus;
import com.multiplayer.tictactoe.repository.GameHistoryRepository;
import com.multiplayer.tictactoe.service.GameHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameHistoryServiceImpl implements GameHistoryService {

    private final GameHistoryRepository gameHistoryRepository;

    @Override
    public GameHistory saveGameHistory(String gameId, String userIdX, String userIdO, GameStatus status,
                                       int boardVerticalSize, int boardHorizontalSize, String boardState,
                                       String winnerId, Long durationMs) {
        GameHistory gameHistory = new GameHistory();
        gameHistory.setGameId(gameId);
        gameHistory.setUserIdX(userIdX);
        gameHistory.setUserIdO(userIdO);
        gameHistory.setStatus(status);
        gameHistory.setBoardVerticalSize(boardVerticalSize);
        gameHistory.setBoardHorizontalSize(boardHorizontalSize);
        gameHistory.setBoardState(boardState);
        gameHistory.setWinnerId(winnerId);
        gameHistory.setCompletedAt(LocalDateTime.now());
        gameHistory.setDurationMs(durationMs);

        return gameHistoryRepository.save(gameHistory);
    }

    @Override
    public List<GameHistory> getGameHistoryByUserId(String userId) {
        return gameHistoryRepository.findByUserIdXOrUserIdO(userId, userId);
    }

    @Override
    public List<GameHistory> getGameHistoryByStatus(GameStatus status) {
        return gameHistoryRepository.findByStatus(status);
    }

    @Override
    public List<GameHistory> getGameHistoryByUsers(String userIdX, String userIdO) {
        return gameHistoryRepository.findByUserIdXAndUserIdO(userIdX, userIdO);
    }

    @Override
    public GameHistory getGameHistoryById(Long id) {
        return gameHistoryRepository.findById(id).orElse(null);
    }
}