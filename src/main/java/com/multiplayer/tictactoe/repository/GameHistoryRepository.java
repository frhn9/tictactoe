package com.multiplayer.tictactoe.repository;

import com.multiplayer.tictactoe.entity.jpa.GameHistory;
import com.multiplayer.tictactoe.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    List<GameHistory> findByUserIdXOrUserIdO(String userIdX, String userIdO);
    List<GameHistory> findByStatus(GameStatus status);
    List<GameHistory> findByUserIdXAndUserIdO(String userIdX, String userIdO);
}