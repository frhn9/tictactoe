package com.multiplayer.tictactoe.dto.response;

import com.multiplayer.tictactoe.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCreatedRes {
    private String gameId;
    private String playerId;
    private String playerRole; // 'X' or 'O'
    private GameStatus status;
    private int boardVerticalSize;
    private int boardHorizontalSize;
}