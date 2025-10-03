package com.multiplayer.tictactoe.mapper;

import com.multiplayer.tictactoe.dto.request.MakeMoveReq;
import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.dto.response.MakeMoveRes;
import com.multiplayer.tictactoe.entity.jpa.GameHistory;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface GameMapper {

    GameStartedRes toGameStartedRes(ActiveGame activeGame);

    @Mapping(target = "gameId", source = "activeGame.gameId")
    @Mapping(target = "playerId", source = "request.sessionId")
    @Mapping(target = "row", source = "request.row")
    @Mapping(target = "col", source = "request.col") 
    @Mapping(target = "status", source = "activeGame.status")
    @Mapping(target = "winnerId", source = "winnerId")
    @Mapping(target = "nextPlayerId", ignore = true)
    @Mapping(target = "boardState", source = "activeGame.boardState")
    MakeMoveRes toMakeMoveResNoNextPlayer(ActiveGame activeGame, MakeMoveReq request, String winnerId);
    
    @Mapping(target = "gameId", source = "activeGame.gameId")
    @Mapping(target = "playerId", source = "request.sessionId")
    @Mapping(target = "row", source = "request.row")
    @Mapping(target = "col", source = "request.col")
    @Mapping(target = "status", source = "activeGame.status")
    @Mapping(target = "winnerId", source = "winnerId")
    @Mapping(target = "nextPlayerId", source = "activeGame.currentTurnSessionId")
    @Mapping(target = "boardState", source = "activeGame.boardState")
    MakeMoveRes toMakeMoveResWithNextPlayer(ActiveGame activeGame, MakeMoveReq request, String winnerId);

    @Mapping(target = "gameId", source = "activeGame.gameId")
    @Mapping(target = "userIdX", source = "activeGame.sessionIdX")
    @Mapping(target = "userIdO", source = "activeGame.sessionIdO")
    @Mapping(target = "status", source = "activeGame.status")
    @Mapping(target = "boardVerticalSize", source = "activeGame.boardVerticalSize")
    @Mapping(target = "boardHorizontalSize", source = "activeGame.boardHorizontalSize")
    @Mapping(target = "boardState", source = "activeGame.boardState", qualifiedByName = "boardStateToString")
    @Mapping(target = "winnerId", source = "winnerSessionId")
    @Mapping(target = "completedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GameHistory toGameHistoryEntity(ActiveGame activeGame, String winnerSessionId);

    @Named("boardStateToString")
    default String boardStateToString(Map<Character, Set<String>> boardState) {
        return boardState != null ? boardState.toString() : null;
    }

}
