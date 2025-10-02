package com.multiplayer.tictactoe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrJoinGameReq {

    private String gameId;

    private int boardVerticalSize;

    private int boardHorizontalSize;

    private String sessionId;

}
