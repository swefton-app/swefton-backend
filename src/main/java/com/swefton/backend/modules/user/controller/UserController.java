package com.swefton.backend.modules.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.user.api.UserApi;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(UserApi.BASE_PATH)
public class UserController {

    private final UserService userService;

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingUserPojo> completeOnboarding(
            @Valid @RequestBody OnboardingUserPojo request) {
        return ResponseEntity.ok(userService.completeOnboarding(request));
    }

    @GetMapping("/onboarding")
    public ResponseEntity<OnboardingUserPojo> getOnboarding() {
        return ResponseEntity.ok(userService.getOnboarding());
    }
}
