package com.multiplayer.tictactoe.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeMoveReq {

    private String gameId;

    private String sessionId;

    private int row;

    private int col;

}