package com.hydroneo.aquaCore.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hydroneo.aquaCore.controllers.UserController;
import com.hydroneo.aquaCore.models.Pond;
import com.hydroneo.aquaCore.repository.PondRepository;
import com.hydroneo.aquaCore.repository.UserRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PondService {
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final PondRepository pondRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;

    public PondService(PondRepository pondRepository, UserRepository userRepository,
            WebClient.Builder webClientBuilder) {
        this.pondRepository = pondRepository;
        this.userRepository = userRepository;
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Pond> findPondByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(existingUser -> {
                    return pondRepository.findByUserId(existingUser.getId());
                });
    }

    public Flux<Pond> findPondsByUsername(String username) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMapMany(existingUser -> {
                    return pondRepository.findAllByUserId(existingUser.getId());
                });
    }

    public Mono<Pond> createPond(String username, Pond pond) {
        if (pond.getAddress() == null || pond.getAddress().isBlank()) {
            return Mono.error(new IllegalArgumentException("Address is required"));
        }

        String serialNumber = pond.getSerialNumber();
        if (serialNumber == null || serialNumber.isBlank()) {
            return Mono.error(new IllegalArgumentException("Serial number is required"));
        }

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(user -> {
                    pond.setUserId(user.getId());

                    return webClientBuilder.build()
                            .put()
                            .uri("http://device-registry-alb-backend-5-918131464.ap-southeast-1.elb.amazonaws.com/api/v1/devices/{serialNumber}/status?active=true", serialNumber)
                            .retrieve()
                            .onStatus(
                                    status -> status.value() == 409,
                                    response -> Mono.error(new IllegalArgumentException("❌ Device is already active")))
                            .onStatus(
                                    status -> status.is4xxClientError() || status.is5xxServerError(),
                                    response -> response.bodyToMono(String.class)
                                            .flatMap(body -> {
                                                log.error("❌ Failed to activate device: {}", body);
                                                return Mono.error(new IllegalArgumentException(
                                                        "❌ Device not found or could not be activated"));
                                            }))
                            .toBodilessEntity()
                            .then(
                                    pondRepository.findBySerialNumber(serialNumber).hasElement())
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new IllegalArgumentException("❌ Sensor ID already in use"));
                                }
                                return pondRepository.save(pond)
                                        .doOnSuccess(savedPond -> log.info("✅ Pond created: {}", savedPond));
                            });
                });
    }

    public Mono<Pond> updatePond(String username, Long pondId, Pond pondUpdate) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(user -> pondRepository.findById(pondId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Pond not found")))
                        .flatMap(existingPond -> {
                            if (!existingPond.getUserId().equals(user.getId())) {
                                return Mono.error(new IllegalAccessException("You do not own this pond"));
                            }

                            Mono<Void> checkSerialConflict = Mono.empty();
                            if (pondUpdate.getSerialNumber() != null) {
                                checkSerialConflict = pondRepository.findBySerialNumber(pondUpdate.getSerialNumber())
                                        .filter(p -> !p.getId().equals(pondId))
                                        .hasElement()
                                        .flatMap(exists -> {
                                            if (exists) {
                                                return Mono.error(
                                                        new IllegalArgumentException("Sensor ID already in use"));
                                            }
                                            return Mono.empty();
                                        });
                            }

                            return checkSerialConflict.then(Mono.defer(() -> {
                                // Apply updates
                                if (pondUpdate.getAddress() != null) {
                                    existingPond.setAddress(pondUpdate.getAddress());
                                }
                                if (pondUpdate.getSerialNumber() != null) {
                                    existingPond.setSerialNumber(pondUpdate.getSerialNumber());
                                }

                                return pondRepository.save(existingPond)
                                        .doOnSuccess(updated -> log.info("✅ Pond updated: {}", updated));
                            }));
                        }));
    }

    public Mono<Pond> deletePond(String username, Long pondId) {
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(user -> pondRepository.findById(pondId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Pond not found")))
                        .flatMap(existingPond -> {
                            if (!existingPond.getUserId().equals(user.getId())) {
                                return Mono.error(new IllegalAccessException("You do not own this pond"));
                            }

                        // 🔁 Set device active=false before deleting
                        return webClientBuilder.build()
                                .put()
                                .uri("http://device-registry-alb-backend-5-918131464.ap-southeast-1.elb.amazonaws.com/api/v1/devices/{serialNumber}/status?active=false", existingPond.getSerialNumber())
                                .retrieve()
                                .onStatus(
                                        status -> status.is4xxClientError() || status.is5xxServerError(),
                                        response -> response.bodyToMono(String.class)
                                                .flatMap(body -> {
                                                    log.error("❌ Failed to deactivate device: {}", body);
                                                    return Mono.error(new IllegalArgumentException("❌ Device could not be deactivated"));
                                                })
                                )
                                .toBodilessEntity()
                                .then(pondRepository.delete(existingPond)
                                        .thenReturn(existingPond)
                                        .doOnSuccess(deleted -> log.info("🗑️ Pond deleted: {}", deleted)));
                    }));
    }

}
