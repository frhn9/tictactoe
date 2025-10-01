package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.request.MakeMoveReq;
import com.multiplayer.tictactoe.dto.response.GameCreatedRes;
import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.dto.response.MakeMoveRes;
import com.multiplayer.tictactoe.entity.jpa.User;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import com.multiplayer.tictactoe.enums.GameStatus;
import com.multiplayer.tictactoe.repository.ActiveGameRepository;
import com.multiplayer.tictactoe.service.GameHistoryService;
import com.multiplayer.tictactoe.service.GameService;
import com.multiplayer.tictactoe.service.UserService;
import com.multiplayer.tictactoe.utils.GameEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ActiveGameRepository activeGameRepository;
    private final GameEngine gameEngine;
    private final UserService userService;
    private final GameHistoryService gameHistoryService;

    public void createOrJoinGame(CreateOrJoinGameReq request, String userId) {
        if (request.getGameId() == null || request.getGameId().trim().isEmpty()) {
            ActiveGame game = createGame(request, request.getSessionId());
            
            // Send the game started message to the creator so they can see the game board
            // even while waiting for an opponent
            GameStartedRes gameStartedRes = new GameStartedRes(
                    game.getGameId(),
                    game.getSessionIdX(),
                    game.getSessionIdO(),
                    game.getCurrentTurnSessionId(),
                    game.getStatus(), // WAITING
                    game.getBoardVerticalSize(),
                    game.getBoardHorizontalSize()
            );
            
            simpMessagingTemplate.convertAndSendToUser(
                request.getSessionId(), 
                "/queue/game",
                gameStartedRes
            );
        } else {
            ActiveGame game = joinGame(request, request.getSessionId());

            // Send websocket notification that game has started
            if (game.getStatus() == GameStatus.IN_PROGRESS) {
                GameStartedRes gameStartedRes = new GameStartedRes(
                        game.getGameId(),
                        game.getSessionIdX(),
                        game.getSessionIdO(),
                        game.getCurrentTurnSessionId(),
                        game.getStatus(),
                        game.getBoardVerticalSize(),
                        game.getBoardHorizontalSize()
                );
                
                // Send to both players
                simpMessagingTemplate.convertAndSendToUser(
                    game.getSessionIdX(), 
                    "/queue/game",
                    gameStartedRes
                );
                
                simpMessagingTemplate.convertAndSendToUser(
                    game.getSessionIdO(), 
                    "/queue/game",
                    gameStartedRes
                );
            }
        }
    }

    @Override
    public void makeMove(MakeMoveReq request) {
        // Get user based on sessionId or deviceId to extract the userId for logic
        User user = userService.findBySessionId(request.getSessionId());
        if (user == null) {
            user = userService.findByDeviceId(request.getDeviceId());
        }
        
        if (user == null) {
            throw new RuntimeException("User not found for session: " + request.getSessionId() + " or device: " + request.getDeviceId());
        }
        
        String userId = user.getId().toString();
        String userSessionId = request.getSessionId();

        // Get the game from repository
        ActiveGame game = activeGameRepository.findById(request.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found: " + request.getGameId()));

        // Validate the move using GameEngine
        if (gameEngine.isIllegalMove(game, userSessionId, request.getRow(), request.getCol())) {
            // Send error response to the player
            throw new RuntimeException("Illegal move");
        }

        // Make the move
        gameEngine.makeMove(game, userSessionId, request.getRow(), request.getCol());

        // Check win condition
        Character winner = gameEngine.checkWinCondition(game, request.getRow(), request.getCol());
        if (winner != null) {
            // Update game status based on winner
            if (winner == 'X') {
                game.setStatus(GameStatus.X_WON);
            } else {
                game.setStatus(GameStatus.O_WON);
            }

            // Save game to update status
            game = activeGameRepository.save(game);

            // Send game over message to both players
            String winnerSessionId = winner == 'X' ? game.getSessionIdX() : game.getSessionIdO();
            MakeMoveRes moveRes = new MakeMoveRes(
                    game.getGameId(),
                    userSessionId,
                    request.getRow(),
                    request.getCol(),
                    game.getStatus(),
                    winnerSessionId,
                    null,
                    game.getBoardState()
            );

            simpMessagingTemplate.convertAndSendToUser(
                    game.getSessionIdX(),
                    "/queue/move-made",
                    moveRes
            );
            
            simpMessagingTemplate.convertAndSendToUser(
                    game.getSessionIdO(),
                    "/queue/move-made",
                    moveRes
            );
            
            // Save game to history
            gameHistoryService.saveGameHistory(
                    game.getGameId(),
                    game.getSessionIdX(),
                    game.getSessionIdO(),
                    game.getStatus(),
                    game.getBoardVerticalSize(),
                    game.getBoardHorizontalSize(),
                    game.getBoardState() != null ? game.getBoardState().toString() : null,
                    winnerSessionId,
                    null // durationMs could be calculated if needed
            );
            
            // Optionally delete the active game from Redis since it's completed
            activeGameRepository.deleteById(game.getGameId());
            
            return; // Exit early since game is over
        }

        // Check draw condition
        if (gameEngine.checkDrawCondition(game)) {
            game.setStatus(GameStatus.DRAW);
            
            // Save game to update status
            game = activeGameRepository.save(game);

            // Send game over (draw) message to both players
            MakeMoveRes moveRes = new MakeMoveRes(
                    game.getGameId(),
                    userSessionId,
                    request.getRow(),
                    request.getCol(),
                    game.getStatus(),
                    null, // No winner in a draw
                    null,
                    game.getBoardState()
            );

            simpMessagingTemplate.convertAndSendToUser(
                    game.getSessionIdX(),
                    "/queue/move-made",
                    moveRes
            );
            
            simpMessagingTemplate.convertAndSendToUser(
                    game.getSessionIdO(),
                    "/queue/move-made",
                    moveRes
            );
            
            // Save game to history (draw)
            gameHistoryService.saveGameHistory(
                    game.getGameId(),
                    game.getSessionIdX(),
                    game.getSessionIdO(),
                    game.getStatus(),
                    game.getBoardVerticalSize(),
                    game.getBoardHorizontalSize(),
                    game.getBoardState() != null ? game.getBoardState().toString() : null,
                    null, // No winner in a draw
                    null // durationMs could be calculated if needed
            );
            
            // Optionally delete the active game from Redis since it's completed
            activeGameRepository.deleteById(game.getGameId());
            
            return; // Exit early since game is over
        }

        // Save game to update board state and turn
        game = activeGameRepository.save(game);

        // Send move update to both players
        MakeMoveRes moveRes = new MakeMoveRes(
                game.getGameId(),
                userSessionId,
                request.getRow(),
                request.getCol(),
                game.getStatus(),
                null, // No winner yet
                game.getCurrentTurnSessionId(),
                game.getBoardState()
        );

        simpMessagingTemplate.convertAndSendToUser(
                game.getSessionIdX(),
                "/queue/move-made",
                moveRes
        );
        
        simpMessagingTemplate.convertAndSendToUser(
                game.getSessionIdO(),
                "/queue/move-made",
                moveRes
        );
    }

    private ActiveGame createGame(CreateOrJoinGameReq request, String sessionId) {
        ActiveGame game = new ActiveGame();
        game.setBoardVerticalSize(request.getBoardVerticalSize());
        game.setBoardHorizontalSize(request.getBoardHorizontalSize());

        Random random = new Random();
        int randomNumber = random.nextInt(0, 1);
        if (randomNumber == 0) {
            game.setSessionIdX(sessionId);
        } else {
            game.setSessionIdO(sessionId);
        }

        game.setStatus(GameStatus.WAITING);
        game.setCurrentTurnSessionId(game.getSessionIdO());

        return activeGameRepository.save(game);
    }

    private ActiveGame joinGame(CreateOrJoinGameReq request, String sessionId) {
        ActiveGame game = activeGameRepository.findById(request.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found: " + request.getGameId()));

        // Validate game state
        if (game.getStatus() != GameStatus.WAITING) {
            throw new RuntimeException("Game is not waiting for players. Status: " + game.getStatus());
        }

        // Check if game is already full
        if (game.getSessionIdX() != null && game.getSessionIdO() != null) {
            throw new RuntimeException("Game is already full");
        }

        // Check if player is already in the game
        if (sessionId.equals(game.getSessionIdX())) {
            throw new RuntimeException("You are already in this game as Player X");
        }

        if (sessionId.equals(game.getSessionIdO())) {
            throw new RuntimeException("You are already in this game as Player O");
        }

        Random random = new Random();
        int randomNumber = random.nextInt(0, 1);
        if (randomNumber == 0 && game.getSessionIdX() == null && game.getSessionIdO() != null) {
            game.setSessionIdX(sessionId);
        } else {
            game.setSessionIdO(sessionId);
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurnSessionId(game.getSessionIdX());

        return activeGameRepository.save(game);
    }

}
