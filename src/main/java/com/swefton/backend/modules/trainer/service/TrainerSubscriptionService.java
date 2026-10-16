package com.swefton.backend.modules.trainer.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.payment.dto.PaymentResponse;
import com.swefton.backend.modules.payment.entity.Payment;
import com.swefton.backend.modules.payment.service.DevelopmentPaymentService;
import com.swefton.backend.modules.pricing.service.PriceOfferService;
import com.swefton.backend.modules.trainer.dto.TrainerCheckoutRequest;
import com.swefton.backend.modules.trainer.dto.TrainerSubscriptionResponse;
import com.swefton.backend.modules.trainer.entity.TrainerPackage;
import com.swefton.backend.modules.trainer.entity.TrainerSubscription;
import com.swefton.backend.modules.trainer.repository.TrainerPackageRepository;
import com.swefton.backend.modules.trainer.repository.TrainerSubscriptionRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserProfile;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainerSubscriptionService {
    private final TrainerPackageRepository packageRepository;
    private final TrainerSubscriptionRepository repository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final DevelopmentPaymentService paymentService;
    private final PriceOfferService priceOfferService;
    private final TrainerPackageService packageService;
    private final ISessionUser sessionUser;

    @Transactional
    public TrainerSubscriptionResponse checkout(TrainerCheckoutRequest request) {
        TrainerPackage item = packageRepository.findById(request.packageId()).filter(TrainerPackage::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer package not found"));
        User client = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (repository.existsByClientIdAndTrainerIdAndStatusIn(client.getId(), item.getTrainer().getId(), List.of("PENDING", "ACTIVE"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active or pending request with this trainer");
        }
        Payment payment = paymentService.charge(client, PriceOfferService.TRAINER_PACKAGE, item.getId(), item.getPrice(),
                priceOfferService.effectivePrice(PriceOfferService.TRAINER_PACKAGE, item.getId(), item.getPrice()),
                item.getCurrency(), request.payment());
        TrainerSubscription subscription = new TrainerSubscription();
        subscription.setClient(client);
        subscription.setTrainer(item.getTrainer());
        subscription.setTrainerPackage(item);
        subscription.setPayment(payment);
        return response(repository.saveAndFlush(subscription));
    }

    @Transactional(readOnly = true)
    public List<TrainerSubscriptionResponse> mine() {
        return repository.findAllByClientIdOrderByRequestedAtDesc(sessionUser.getUserId()).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<TrainerSubscriptionResponse> incoming() {
        return repository.findAllByTrainerIdOrderByRequestedAtDesc(sessionUser.getUserId()).stream().map(this::response).toList();
    }

    @Transactional
    public TrainerSubscriptionResponse decide(Long id, boolean approve) {
        TrainerSubscription item = repository.findById(id)
                .filter(value -> value.getTrainer().getId().equals(sessionUser.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client request not found"));
        if (!"PENDING".equals(item.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending requests can be updated");
        }
        if (approve) {
            LocalDate today = LocalDate.now();
            item.setStatus("ACTIVE");
            item.setApprovedAt(LocalDateTime.now());
            item.setStartsOn(today);
            item.setEndsOn(today.plusDays(item.getTrainerPackage().getDurationDays() - 1L));
        } else {
            item.setStatus("REJECTED");
        }
        return response(repository.saveAndFlush(item));
    }

    private TrainerSubscriptionResponse response(TrainerSubscription item) {
        return new TrainerSubscriptionResponse(item.getId(), item.getTrainer().getId(), name(item.getTrainer()),
                name(item.getClient()), item.getClient().getEmail(), item.getStatus(), packageService.toResponse(item.getTrainerPackage()),
                new PaymentResponse(item.getPayment()), item.getRequestedAt(), item.getStartsOn(), item.getEndsOn());
    }

    private String name(User user) {
        return profileRepository.findByUserId(user.getId()).map(this::profileName).orElse(user.getEmail());
    }

    private String profileName(UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) return profile.getDisplayName();
        return (profile.getFirstName() + " " + profile.getLastName()).trim();
    }
}
