package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.dto.request.CreateOrJoinGameReq;
import com.multiplayer.tictactoe.dto.request.MakeMoveReq;
import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.dto.response.MakeMoveRes;
import com.multiplayer.tictactoe.entity.jpa.GameHistory;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import com.multiplayer.tictactoe.enums.GameStatus;
import com.multiplayer.tictactoe.exception.GameErrorType;
import com.multiplayer.tictactoe.exception.GameException;
import com.multiplayer.tictactoe.mapper.GameMapper;
import com.multiplayer.tictactoe.repository.ActiveGameRepository;
import com.multiplayer.tictactoe.repository.GameHistoryRepository;
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

    private final GameHistoryRepository gameHistoryRepository;
    private final ActiveGameRepository activeGameRepository;

    private final GameEngine gameEngine;

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
        ActiveGame game = activeGameRepository.findById(request.getGameId())
                .orElseThrow(() -> new GameException(GameErrorType.GAME_NOT_FOUND, request.getSessionId()));

        if (gameEngine.isIllegalMove(game, request.getSessionId(), request.getRow(), request.getCol())) {
            throw new GameException(GameErrorType.ILLEGAL_MOVE, request.getSessionId());
        }

        gameEngine.makeMove(game, request.getSessionId(), request.getRow(), request.getCol());

        Character winner = gameEngine.checkWinCondition(game, request.getRow(), request.getCol());
        if (winner != null) {
            handleWinCondition(request, winner, game);
            return;
        }

        if (gameEngine.checkDrawCondition(game)) {
            handleDrawCondition(request, game);
            return;
        }

        game = activeGameRepository.save(game);
        MakeMoveRes moveRes = gameMapper.toMakeMoveResWithNextPlayer(game, request, null);
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
                .orElseThrow(() -> new GameException(GameErrorType.GAME_NOT_FOUND, sessionId));

        // Validate game state
        if (game.getStatus() != GameStatus.WAITING) {
            throw new GameException(GameErrorType.GAME_NOT_WAITING, sessionId);
        }

        // Check if game is already full
        if (game.getSessionIdX() != null && game.getSessionIdO() != null) {
            throw new GameException(GameErrorType.GAME_FULL, sessionId);
        }

        // Check if player is already in the game
        if (sessionId.equals(game.getSessionIdX())) {
            throw new GameException(GameErrorType.ALREADY_IN_GAME_AS_X, sessionId);
        }

        if (sessionId.equals(game.getSessionIdO())) {
            throw new GameException(GameErrorType.ALREADY_IN_GAME_AS_O, sessionId);
        }

        // Assign the joining player to the available slot
        if (game.getSessionIdX() == null) {
            game.setSessionIdX(sessionId);
        } else {
            game.setSessionIdO(sessionId);
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurnSessionId(game.getSessionIdX());

        return activeGameRepository.save(game);
    }

    private void handleWinCondition(MakeMoveReq request, Character winner, ActiveGame game) {
        if (winner == 'X') {
            game.setStatus(GameStatus.X_WON);
        } else {
            game.setStatus(GameStatus.O_WON);
        }

        game = activeGameRepository.save(game);

        String winnerSessionId = winner == 'X' ? game.getSessionIdX() : game.getSessionIdO();
        MakeMoveRes moveRes = gameMapper.toMakeMoveResNoNextPlayer(game, request, winnerSessionId);

        sendWebSocketMessage(game.getSessionIdX(), moveRes);
        sendWebSocketMessage(game.getSessionIdO(), moveRes);

        if (Boolean.FALSE.equals(game.isHistorySaved())) {
            game.setHistorySaved(true);
            GameHistory gameHistory = gameMapper.toGameHistoryEntity(game, winnerSessionId);
            gameHistoryRepository.save(gameHistory);
        }

        activeGameRepository.deleteById(game.getGameId());
    }

    private void handleDrawCondition(MakeMoveReq request, ActiveGame game) {
        game.setStatus(GameStatus.DRAW);
        game = activeGameRepository.save(game);

        MakeMoveRes moveRes = gameMapper.toMakeMoveResNoNextPlayer(game, request, null);

        sendWebSocketMessage(game.getSessionIdX(), moveRes);
        sendWebSocketMessage(game.getSessionIdO(), moveRes);

        if (Boolean.FALSE.equals(game.isHistorySaved())) {
            game.setHistorySaved(true);
            GameHistory gameHistory = gameMapper.toGameHistoryEntity(game, null);
            gameHistoryRepository.save(gameHistory);
        }

        activeGameRepository.deleteById(game.getGameId());
    }

    private void sendWebSocketMessage(String sessionId, Object payload) {
        simpMessagingTemplate.convertAndSend("/topic/session/" + sessionId, payload);
    }

}
