package com.hydroneo.aquaCore.repository;

import com.hydroneo.aquaCore.models.Pond;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PondRepository extends ReactiveCrudRepository<Pond, Long> {
    Mono<Pond> findByUserId(Long userId); // for one pond
    // or
    Flux<Pond> findAllByUserId(Long userId); // if user can have multiple ponds

    Mono<Pond> findBySerialNumber(String serialNumber);
}
