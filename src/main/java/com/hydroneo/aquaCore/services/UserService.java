package com.hydroneo.aquaCore.services;

import com.hydroneo.aquaCore.models.User;
import com.hydroneo.aquaCore.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> createOrUpdate(User user) {
        return userRepository.findByGoogleId(user.getGoogleId())
                .flatMap(existingUser -> {
                    existingUser.setUsername(user.getUsername());
                    existingUser.setEmail(user.getEmail());
                    return userRepository.save(existingUser);
                })
                .switchIfEmpty(Mono.defer(() -> userRepository.save(user)));
    }

    public Mono<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

}
