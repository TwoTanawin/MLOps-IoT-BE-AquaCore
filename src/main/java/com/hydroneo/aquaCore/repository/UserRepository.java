package com.hydroneo.aquaCore.repository;

import com.hydroneo.aquaCore.models.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByGoogleId(String githubId);
    Mono<User> findByUsername(String username);
}
