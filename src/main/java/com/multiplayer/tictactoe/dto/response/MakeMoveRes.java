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
    private String winnerId; // ID of the winning player, null if no winner yet
    private String nextPlayerId; // ID of the player whose turn is next
    private Map<Character, Set<String>> boardState;
}