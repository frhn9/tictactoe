package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.request.MakeMoveReq;
import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.dto.response.MakeMoveRes;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import com.multiplayer.tictactoe.enums.GameStatus;
import com.multiplayer.tictactoe.repository.ActiveGameRepository;
import com.multiplayer.tictactoe.service.GameHistoryService;
import com.multiplayer.tictactoe.service.GameService;
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
            
            // Use session-specific topic for reliable delivery
            simpMessagingTemplate.convertAndSend("/topic/session/" + request.getSessionId(), gameStartedRes);
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
                // Use session-specific topics for reliable delivery to both players
                simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdX(), gameStartedRes);
                simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdO(), gameStartedRes);
            }
        }
    }

    @Override
    public void makeMove(MakeMoveReq request) {
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

            // Use session-specific topics for reliable delivery to both players
            simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdX(), moveRes);
            simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdO(), moveRes);
            
            // Only save game to history if not already saved to prevent duplicate entries
            if (!game.isHistorySaved()) {
                game.setHistorySaved(true);
                
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
            }
            
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

            // Use session-specific topics for reliable delivery to both players
            simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdX(), moveRes);
            simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdO(), moveRes);
            
            // Only save game to history if not already saved to prevent duplicate entries
            if (!game.isHistorySaved()) {
                game.setHistorySaved(true);
                
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
            }
            
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

        // Use session-specific topics for reliable delivery to both players
        simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdX(), moveRes);
        simpMessagingTemplate.convertAndSend("/topic/session/" + game.getSessionIdO(), moveRes);
    }

    private ActiveGame createGame(CreateOrJoinGameReq request, String sessionId) {
        ActiveGame game = new ActiveGame();
        game.setBoardVerticalSize(request.getBoardVerticalSize());
        game.setBoardHorizontalSize(request.getBoardHorizontalSize());

        // Randomly assign the creating player as X or O
        Random random = new Random();
        int randomNumber = random.nextInt(2); // Returns 0 or 1
        if (randomNumber == 0) {
            game.setSessionIdX(sessionId);
            game.setCurrentTurnSessionId(sessionId); // X goes first when opponent joins
        } else {
            game.setSessionIdO(sessionId);
            game.setCurrentTurnSessionId(sessionId); // O goes first when opponent joins
        }

        game.setStatus(GameStatus.WAITING);

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

        // Assign the joining player to the available slot
        if (game.getSessionIdX() == null) {
            game.setSessionIdX(sessionId);
        } else if (game.getSessionIdO() == null) {
            game.setSessionIdO(sessionId);
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        // In Tic Tac Toe, X always goes first, regardless of who created the game
        game.setCurrentTurnSessionId(game.getSessionIdX());

        return activeGameRepository.save(game);
    }

}
