package com.multiplayer.tictactoe.dto.response;

import com.multiplayer.tictactoe.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStartedRes {

    private String gameId;

    private String sessionIdX;

    private String sessionIdO;

    private String currentTurnSessionId;

    private GameStatus status;

    private int boardVerticalSize;

    private int boardHorizontalSize;
}