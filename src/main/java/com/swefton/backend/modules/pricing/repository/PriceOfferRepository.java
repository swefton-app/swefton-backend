package com.swefton.backend.modules.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.pricing.entity.PriceOffer;

public interface PriceOfferRepository extends JpaRepository<PriceOffer, Long> {
    List<PriceOffer> findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
    List<PriceOffer> findAllByTargetTypeAndTargetIdAndCancelledAtIsNullAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByCreatedAtDesc(
            String targetType, Long targetId, LocalDateTime startsAt, LocalDateTime endsAt);
}
