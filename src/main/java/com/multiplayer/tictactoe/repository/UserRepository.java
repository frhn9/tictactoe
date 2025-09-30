package com.multiplayer.tictactoe.repository;

import com.multiplayer.tictactoe.entity.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySessionId(String sessionId);
    Optional<User> findByDeviceId(String deviceId);
}