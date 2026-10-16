package com.swefton.backend.modules.trainer.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.pricing.dto.PriceOfferRequest;
import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;
import com.swefton.backend.modules.pricing.service.PriceOfferService;
import com.swefton.backend.modules.trainer.dto.TrainerPackageRequest;
import com.swefton.backend.modules.trainer.dto.TrainerPackageResponse;
import com.swefton.backend.modules.trainer.entity.TrainerPackage;
import com.swefton.backend.modules.trainer.repository.TrainerPackageRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainerPackageService {
    private final TrainerPackageRepository repository;
    private final UserRepository userRepository;
    private final PriceOfferService priceOfferService;
    private final ISessionUser sessionUser;

    @Transactional(readOnly = true)
    public List<TrainerPackageResponse> mine() {
        return repository.findAllByTrainerIdOrderByCreatedAtDesc(sessionUser.getUserId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public TrainerPackageResponse create(TrainerPackageRequest request) {
        User trainer = currentTrainer();
        TrainerPackage item = new TrainerPackage();
        item.setTrainer(trainer);
        apply(item, request);
        return toResponse(repository.saveAndFlush(item));
    }

    @Transactional
    public TrainerPackageResponse update(Long packageId, TrainerPackageRequest request) {
        TrainerPackage item = owned(packageId);
        apply(item, request);
        return toResponse(repository.saveAndFlush(item));
    }

    @Transactional
    public PriceOfferResponse createOffer(Long packageId, PriceOfferRequest request) {
        TrainerPackage item = owned(packageId);
        return priceOfferService.create(PriceOfferService.TRAINER_PACKAGE, packageId, item.getPrice(), request);
    }

    @Transactional(readOnly = true)
    public List<PriceOfferResponse> offerHistory(Long packageId) {
        owned(packageId);
        return priceOfferService.history(PriceOfferService.TRAINER_PACKAGE, packageId);
    }

    @Transactional
    public void cancelOffer(Long packageId, Long offerId) {
        owned(packageId);
        priceOfferService.cancel(PriceOfferService.TRAINER_PACKAGE, packageId, offerId);
    }

    public TrainerPackageResponse toResponse(TrainerPackage item) {
        PriceOfferResponse offer = priceOfferService.activeResponse(PriceOfferService.TRAINER_PACKAGE, item.getId());
        return new TrainerPackageResponse(item, offer == null ? item.getPrice() : offer.salePrice(), offer);
    }

    private void apply(TrainerPackage item, TrainerPackageRequest request) {
        item.setName(request.name().trim());
        item.setDescription(request.description() == null ? null : request.description().trim());
        item.setSessionsPerWeek(request.sessionsPerWeek());
        item.setDurationDays(request.durationDays());
        item.setPrice(request.price());
        item.setActive(request.active());
    }

    private TrainerPackage owned(Long packageId) {
        return repository.findByIdAndTrainerId(packageId, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer package not found"));
    }

    private User currentTrainer() {
        User user = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));
        if (!RoleCode.TRAINER.equals(user.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A trainer account is required");
        }
        return user;
    }
}
