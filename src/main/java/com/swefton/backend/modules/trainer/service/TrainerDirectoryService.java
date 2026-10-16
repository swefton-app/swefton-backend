package com.swefton.backend.modules.trainer.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.trainer.dto.TrainerProfileResponse;
import com.swefton.backend.modules.trainer.repository.TrainerPackageRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserAddress;
import com.swefton.backend.modules.user.entity.UserProfile;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserAddressRepository;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainerDirectoryService {
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserAddressRepository addressRepository;
    private final ImageRepository imageRepository;
    private final TrainerPackageRepository packageRepository;
    private final TrainerPackageService packageService;

    @Transactional(readOnly = true)
    public List<TrainerProfileResponse> search(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAllByRoleCodeAndEnabledTrueAndOnboardingCompletedTrueAndDeletedAtIsNullOrderByCreatedAtDesc(RoleCode.TRAINER)
                .stream().map(this::toResponse)
                .filter(item -> normalized.isBlank() || contains(item.displayName(), normalized)
                        || contains(item.bio(), normalized) || contains(item.city(), normalized)
                        || contains(item.country(), normalized))
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse profile(Long trainerId) {
        User trainer = userRepository.findById(trainerId)
                .filter(user -> user.getDeletedAt() == null && user.isEnabled()
                        && RoleCode.TRAINER.equals(user.getRole().getCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));
        return toResponse(trainer);
    }

    private TrainerProfileResponse toResponse(User trainer) {
        UserProfile profile = profileRepository.findByUserId(trainer.getId()).orElse(null);
        UserAddress address = addressRepository.findByUserId(trainer.getId()).stream().findFirst().orElse(null);
        ImagePojo avatar = imageRepository.findAllByUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(trainer.getId()).stream()
                .filter(image -> ImageType.PROFILE.equals(image.getType())).findFirst().map(ImagePojo::new).orElse(null);
        return new TrainerProfileResponse(
                trainer.getId(), profile == null ? trainer.getEmail() : name(profile),
                profile == null ? null : profile.getBio(), profile == null ? null : profile.getExperience(),
                address == null ? null : address.getCity(), address == null ? null : address.getCountry(), avatar,
                packageRepository.findAllByTrainerIdAndActiveTrueOrderByPriceAsc(trainer.getId()).stream()
                        .map(packageService::toResponse).toList());
    }

    private String name(UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) return profile.getDisplayName();
        return (profile.getFirstName() + " " + profile.getLastName()).trim();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
