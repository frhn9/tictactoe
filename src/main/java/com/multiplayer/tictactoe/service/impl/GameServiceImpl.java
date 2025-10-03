package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.request.MakeMoveReq;
import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.dto.response.MakeMoveRes;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import com.multiplayer.tictactoe.enums.GameStatus;
import com.multiplayer.tictactoe.mapper.GameMapper;
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

    private final GameMapper gameMapper;

    public void createOrJoinGame(CreateOrJoinGameReq request, String userId) {
        if (request.getGameId() == null || request.getGameId().trim().isEmpty()) {
            ActiveGame game = createGame(request, request.getSessionId());
            GameStartedRes gameStartedRes = gameMapper.toGameStartedRes(game);

            sendWebSocketMessage(request.getSessionId(), gameStartedRes);
        } else {
            ActiveGame game = joinGame(request, request.getSessionId());

            if (game.getStatus() == GameStatus.IN_PROGRESS) {
                GameStartedRes gameStartedRes = gameMapper.toGameStartedRes(game);

                sendWebSocketMessage(game.getSessionIdX(), gameStartedRes);
                sendWebSocketMessage(game.getSessionIdO(), gameStartedRes);
            }
        }
    }

    @Override
    public void makeMove(MakeMoveReq request) {
        String userSessionId = request.getSessionId();

        ActiveGame game = activeGameRepository.findById(request.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found: " + request.getGameId()));

        if (gameEngine.isIllegalMove(game, userSessionId, request.getRow(), request.getCol())) {
            throw new RuntimeException("Illegal move");
        }

        gameEngine.makeMove(game, userSessionId, request.getRow(), request.getCol());

        Character winner = gameEngine.checkWinCondition(game, request.getRow(), request.getCol());
        if (winner != null) {
            if (winner == 'X') {
                game.setStatus(GameStatus.X_WON);
            } else {
                game.setStatus(GameStatus.O_WON);
            }

            game = activeGameRepository.save(game);

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

            sendWebSocketMessage(game.getSessionIdX(), moveRes);
            sendWebSocketMessage(game.getSessionIdO(), moveRes);

            if (Boolean.FALSE.equals(game.isHistorySaved())) {
                game.setHistorySaved(true);

                gameHistoryService.saveGameHistory(
                        game.getGameId(),
                        game.getSessionIdX(),
                        game.getSessionIdO(),
                        game.getStatus(),
                        game.getBoardVerticalSize(),
                        game.getBoardHorizontalSize(),
                        game.getBoardState() != null ? game.getBoardState().toString() : null,
                        winnerSessionId
                );
            }

            activeGameRepository.deleteById(game.getGameId());
            
            return;
        }

        if (gameEngine.checkDrawCondition(game)) {
            game.setStatus(GameStatus.DRAW);

            game = activeGameRepository.save(game);

            MakeMoveRes moveRes = new MakeMoveRes(
                    game.getGameId(),
                    userSessionId,
                    request.getRow(),
                    request.getCol(),
                    game.getStatus(),
                    null,
                    null,
                    game.getBoardState()
            );

            sendWebSocketMessage(game.getSessionIdX(), moveRes);
            sendWebSocketMessage(game.getSessionIdO(), moveRes);

            if (Boolean.FALSE.equals(game.isHistorySaved())) {
                game.setHistorySaved(true);

                gameHistoryService.saveGameHistory(
                        game.getGameId(),
                        game.getSessionIdX(),
                        game.getSessionIdO(),
                        game.getStatus(),
                        game.getBoardVerticalSize(),
                        game.getBoardHorizontalSize(),
                        game.getBoardState() != null ? game.getBoardState().toString() : null,
                        null
                );
            }

            activeGameRepository.deleteById(game.getGameId());
            return;
        }

        game = activeGameRepository.save(game);

        MakeMoveRes moveRes = new MakeMoveRes(
                game.getGameId(),
                userSessionId,
                request.getRow(),
                request.getCol(),
                game.getStatus(),
                null,
                game.getCurrentTurnSessionId(),
                game.getBoardState()
        );

        sendWebSocketMessage(game.getSessionIdX(), moveRes);
        sendWebSocketMessage(game.getSessionIdO(), moveRes);
    }

    private ActiveGame createGame(CreateOrJoinGameReq request, String sessionId) {
        ActiveGame game = new ActiveGame();
        game.setBoardVerticalSize(request.getBoardVerticalSize());
        game.setBoardHorizontalSize(request.getBoardHorizontalSize());

        Random random = new Random();
        int randomNumber = random.nextInt(2);
        if (randomNumber == 0) {
            game.setSessionIdX(sessionId);
            game.setCurrentTurnSessionId(sessionId);
        } else {
            game.setSessionIdO(sessionId);
            game.setCurrentTurnSessionId(sessionId);
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
        game.setCurrentTurnSessionId(game.getSessionIdX());

        return activeGameRepository.save(game);
    }

    private void sendWebSocketMessage(String sessionId, Object payload) {
        simpMessagingTemplate.convertAndSend("/topic/session/" + sessionId, payload);
    }

}
