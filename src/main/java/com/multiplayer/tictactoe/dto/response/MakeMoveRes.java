package com.multiplayer.tictactoe.dto.response;

import com.multiplayer.tictactoe.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeMoveRes {

    private String gameId;

    private String playerId;

    private int row;

    private int col;

    private GameStatus status;

    private String winnerId;

    private String nextPlayerId;

    private Map<Character, Set<String>> boardState;

}