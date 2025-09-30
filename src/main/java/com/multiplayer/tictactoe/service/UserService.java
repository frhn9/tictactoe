package com.multiplayer.tictactoe.service;

import com.multiplayer.tictactoe.entity.jpa.User;

public interface UserService {
    User createUser(String sessionId, String deviceId);
    User findOrCreateUser(String sessionId, String deviceId);
    User findBySessionId(String sessionId);
    User findByDeviceId(String deviceId);
}