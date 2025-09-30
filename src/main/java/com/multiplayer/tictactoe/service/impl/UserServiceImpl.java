package com.multiplayer.tictactoe.service.impl;

import com.multiplayer.tictactoe.entity.jpa.User;
import com.multiplayer.tictactoe.repository.UserRepository;
import com.multiplayer.tictactoe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User createUser(String sessionId, String deviceId) {
        User user = new User();
        user.setSessionId(sessionId);
        user.setDeviceId(deviceId);
        return userRepository.save(user);
    }

    @Override
    public User findOrCreateUser(String sessionId, String deviceId) {
        return userRepository.findBySessionId(sessionId)
                .orElseGet(() -> createUser(sessionId, deviceId));
    }

    @Override
    public User findBySessionId(String sessionId) {
        return userRepository.findBySessionId(sessionId).orElse(null);
    }

    @Override
    public User findByDeviceId(String deviceId) {
        return userRepository.findByDeviceId(deviceId).orElse(null);
    }
}