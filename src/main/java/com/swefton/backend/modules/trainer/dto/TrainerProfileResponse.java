package com.swefton.backend.modules.trainer.dto;

import java.util.List;

import com.swefton.backend.modules.image.dto.ImagePojo;

public record TrainerProfileResponse(
        Long id, String displayName, String bio, Integer experience, String city, String country,
        ImagePojo avatar, List<TrainerPackageResponse> packages) {
}
