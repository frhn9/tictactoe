package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.response.GameCreatedRes;
import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import com.multiplayer.tictactoe.enums.GameStatus;
import com.multiplayer.tictactoe.repository.ActiveGameRepository;
import com.multiplayer.tictactoe.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ActiveGameRepository activeGameRepository;

    public void createOrJoinGame(CreateOrJoinGameReq request) {
        if (request.getGameId() == null || request.getGameId().trim().isEmpty()) {
            ActiveGame game = createGame(request);

            // Send websocket notification that game room has been created
            GameCreatedRes gameCreatedRes = new GameCreatedRes(
                    game.getGameId(),
                    request.getPlayerId(),
                    request.getPlayerId().equals(game.getUserIdX()) ? "X" : "O",
                    game.getStatus(),
                    game.getBoardVerticalSize(),
                    game.getBoardHorizontalSize()
            );
            
            // Send to the player who created the game
            simpMessagingTemplate.convertAndSendToUser(
                request.getPlayerId(), 
                "/queue/game-created", 
                gameCreatedRes
            );
        } else {
            ActiveGame game = joinGame(request);

            // Send websocket notification that game has started
            if (game.getStatus() == GameStatus.IN_PROGRESS) {
                GameStartedRes gameStartedRes = new GameStartedRes(
                        game.getGameId(),
                        game.getUserIdX(),
                        game.getUserIdO(),
                        game.getCurrentTurnUserId(),
                        game.getStatus(),
                        game.getBoardVerticalSize(),
                        game.getBoardHorizontalSize()
                );
                
                // Send to both players
                simpMessagingTemplate.convertAndSendToUser(
                    game.getUserIdX(), 
                    "/queue/game-started", 
                    gameStartedRes
                );
                
                simpMessagingTemplate.convertAndSendToUser(
                    game.getUserIdO(), 
                    "/queue/game-started", 
                    gameStartedRes
                );
            }
        }
    }

    private ActiveGame createGame(CreateOrJoinGameReq request) {
        ActiveGame game = new ActiveGame();
        game.setBoardVerticalSize(request.getBoardVerticalSize());
        game.setBoardHorizontalSize(request.getBoardHorizontalSize());

        Random random = new Random();
        int randomNumber = random.nextInt(0, 1);
        if (randomNumber == 0) {
            game.setUserIdX(request.getPlayerId());
        } else {
            game.setUserIdO(request.getPlayerId());
        }

        game.setStatus(GameStatus.WAITING);
        game.setCurrentTurnUserId(game.getUserIdO());

        return activeGameRepository.save(game);
    }

    private ActiveGame joinGame(CreateOrJoinGameReq request) {
        ActiveGame game = activeGameRepository.findById(request.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found: " + request.getGameId()));

        // Validate game state
        if (game.getStatus() != GameStatus.WAITING) {
            throw new RuntimeException("Game is not waiting for players. Status: " + game.getStatus());
        }

        // Check if game is already full
        if (game.getUserIdX() != null && game.getUserIdO() != null) {
            throw new RuntimeException("Game is already full");
        }

        // Check if player is already in the game
        if (request.getPlayerId().equals(game.getUserIdX())) {
            throw new RuntimeException("You are already in this game as Player X");
        }

        if (request.getPlayerId().equals(game.getUserIdO())) {
            throw new RuntimeException("You are already in this game as Player O");
        }

        Random random = new Random();
        int randomNumber = random.nextInt(0, 1);
        if (randomNumber == 0 && game.getUserIdX() == null && game.getUserIdO() != null) {
            game.setUserIdX(request.getPlayerId());
        } else {
            game.setUserIdO(request.getPlayerId());
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurnUserId(game.getUserIdX());

        return activeGameRepository.save(game);
    }

}
