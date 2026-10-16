package com.swefton.backend.modules.pricing.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.pricing.dto.PriceOfferRequest;
import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;
import com.swefton.backend.modules.pricing.entity.PriceOffer;
import com.swefton.backend.modules.pricing.repository.PriceOfferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceOfferService {

    public static final String MEMBERSHIP_PLAN = "MEMBERSHIP_PLAN";
    public static final String TRAINER_PACKAGE = "TRAINER_PACKAGE";

    private final PriceOfferRepository repository;

    @Transactional(readOnly = true)
    public Optional<PriceOffer> active(String targetType, Long targetId) {
        LocalDateTime now = LocalDateTime.now();
        return repository
                .findAllByTargetTypeAndTargetIdAndCancelledAtIsNullAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByCreatedAtDesc(
                        targetType, targetId, now, now)
                .stream().findFirst();
    }

    @Transactional(readOnly = true)
    public BigDecimal effectivePrice(String targetType, Long targetId, BigDecimal basePrice) {
        return active(targetType, targetId).map(PriceOffer::getSalePrice).orElse(basePrice);
    }

    @Transactional(readOnly = true)
    public PriceOfferResponse activeResponse(String targetType, Long targetId) {
        return active(targetType, targetId).map(offer -> new PriceOfferResponse(offer, true)).orElse(null);
    }

    @Transactional(readOnly = true)
    public PriceOfferResponse activeOrScheduledResponse(String targetType, Long targetId) {
        LocalDateTime now = LocalDateTime.now();
        return repository.findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId).stream()
                .filter(offer -> offer.getCancelledAt() == null && !offer.getEndsAt().isBefore(now))
                .sorted(Comparator.comparing(PriceOffer::getStartsAt))
                .findFirst()
                .map(offer -> new PriceOfferResponse(offer,
                        !offer.getStartsAt().isAfter(now) && !offer.getEndsAt().isBefore(now)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PriceOfferResponse> history(String targetType, Long targetId) {
        LocalDateTime now = LocalDateTime.now();
        return repository.findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId).stream()
                .map(offer -> new PriceOfferResponse(offer, offer.getCancelledAt() == null
                        && !offer.getStartsAt().isAfter(now) && !offer.getEndsAt().isBefore(now)))
                .toList();
    }

    @Transactional
    public PriceOfferResponse create(String targetType, Long targetId, BigDecimal basePrice, PriceOfferRequest request) {
        validate(targetType, targetId, null, basePrice, request);
        PriceOffer offer = new PriceOffer();
        offer.setTargetType(targetType);
        offer.setTargetId(targetId);
        offer.setOriginalPrice(basePrice);
        offer.setSalePrice(request.salePrice());
        offer.setStartsAt(request.startsAt());
        offer.setEndsAt(request.endsAt());
        PriceOffer saved = repository.saveAndFlush(offer);
        LocalDateTime now = LocalDateTime.now();
        return new PriceOfferResponse(saved, !saved.getStartsAt().isAfter(now) && !saved.getEndsAt().isBefore(now));
    }

    @Transactional
    public PriceOfferResponse update(String targetType, Long targetId, Long offerId, BigDecimal basePrice,
            PriceOfferRequest request) {
        PriceOffer offer = offer(targetType, targetId, offerId);
        if (offer.getCancelledAt() != null || offer.getEndsAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed offers cannot be modified");
        }
        validate(targetType, targetId, offerId, basePrice, request);
        offer.setOriginalPrice(basePrice);
        offer.setSalePrice(request.salePrice());
        offer.setStartsAt(request.startsAt());
        offer.setEndsAt(request.endsAt());
        PriceOffer saved = repository.saveAndFlush(offer);
        LocalDateTime now = LocalDateTime.now();
        return new PriceOfferResponse(saved, !saved.getStartsAt().isAfter(now) && !saved.getEndsAt().isBefore(now));
    }

    @Transactional
    public void cancel(String targetType, Long targetId, Long offerId) {
        PriceOffer offer = offer(targetType, targetId, offerId);
        if (offer.getCancelledAt() == null) offer.setCancelledAt(LocalDateTime.now());
    }

    private void validate(String targetType, Long targetId, Long ignoredOfferId, BigDecimal basePrice,
            PriceOfferRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer end date must be after its start date");
        }
        if (request.salePrice().compareTo(basePrice) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sale price must be lower than the regular price");
        }
        boolean overlaps = repository.findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId).stream()
                .anyMatch(item -> !item.getId().equals(ignoredOfferId)
                        && item.getCancelledAt() == null
                        && request.startsAt().isBefore(item.getEndsAt())
                        && request.endsAt().isAfter(item.getStartsAt()));
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This plan already has an offer in that period");
        }
    }

    private PriceOffer offer(String targetType, Long targetId, Long offerId) {
        return repository.findById(offerId)
                .filter(item -> targetType.equals(item.getTargetType()) && targetId.equals(item.getTargetId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Price offer not found"));
    }
}
