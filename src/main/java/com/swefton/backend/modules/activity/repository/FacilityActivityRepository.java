package com.swefton.backend.modules.activity.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swefton.backend.modules.activity.entity.FacilityActivity;
public interface FacilityActivityRepository extends JpaRepository<FacilityActivity,Long>{
 List<FacilityActivity> findAllByFacilityIdAndDeletedAtIsNullOrderByNameAsc(Long facilityId);
 Optional<FacilityActivity> findByIdAndFacilityIdAndDeletedAtIsNull(Long id,Long facilityId);
 List<FacilityActivity> findAllByResponsibleStaffIdAndDeletedAtIsNull(Long staffId);
}
