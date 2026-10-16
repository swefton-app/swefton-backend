package com.swefton.backend.modules.trainer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.trainer.dto.TrainerProfileResponse;
import com.swefton.backend.modules.trainer.service.TrainerDirectoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/trainers")
public class PublicTrainerController {
    private final TrainerDirectoryService service;

    @GetMapping
    public ResponseEntity<List<TrainerProfileResponse>> search(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(service.search(query));
    }

    @GetMapping("/{trainerId}")
    public ResponseEntity<TrainerProfileResponse> profile(@PathVariable Long trainerId) {
        return ResponseEntity.ok(service.profile(trainerId));
    }
}
