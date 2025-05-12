package com.hydroneo.aquaCore.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hydroneo.aquaCore.models.Pond;
import com.hydroneo.aquaCore.services.PondService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/ponds")
public class PondController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final PondService pondService;

    public PondController(PondService pondService) {
        this.pondService = pondService;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<?>> createPond(
            @RequestHeader("Authorization") String token,
            @RequestParam("username") String username,
            @Valid @RequestBody Pond pond) {
        return pondService.createPond(username, pond)
                .map(savedPond -> {
                    if (savedPond == null) {
                        log.error("❌ createPond() returned null");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("message", "Pond creation failed internally"));
                    }
                    return ResponseEntity.ok(savedPond);
                })
                .onErrorResume(error -> {
                    log.error("❌ Failed to create pond: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(Map.of("message", error.getMessage())));
                });
    }

    @GetMapping("/getPond")
    public Mono<Pond> getPond(
            @RequestHeader("Authorization") String token,
            @RequestParam("username") String username) {
        return pondService.findPondByUsername(username);
    }

    @GetMapping("/getPonds")
    public Flux<Pond> getPonds(
            @RequestHeader("Authorization") String token,
            @RequestParam("username") String username) {
        return pondService.findPondsByUsername(username);
    }

    @PutMapping("/update")
    public Mono<Pond> updatePond(
            @RequestHeader("Authorization") String token,
            @RequestParam("username") String username,
            @RequestParam("pondId") Long pondId,
            @RequestBody Pond pondUpdate) {
        return pondService.updatePond(username, pondId, pondUpdate);
    }

    @DeleteMapping("/delete")
    public Mono<Pond> deletePond(
            @RequestHeader("Authorization") String token,
            @RequestParam("username") String username,
            @RequestParam("pondId") Long pondId) {
        return pondService.deletePond(username, pondId);
    }
}
