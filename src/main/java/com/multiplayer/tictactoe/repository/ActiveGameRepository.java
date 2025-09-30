package com.multiplayer.tictactoe.repository;

import com.multiplayer.tictactoe.entity.redis.ActiveGame;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveGameRepository extends CrudRepository<ActiveGame, String> {
}
