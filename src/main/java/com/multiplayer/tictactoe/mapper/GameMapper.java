package com.multiplayer.tictactoe.mapper;

import com.multiplayer.tictactoe.dto.response.GameStartedRes;
import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameMapper {

    GameStartedRes toGameStartedRes(ActiveGame activeGame);

}
