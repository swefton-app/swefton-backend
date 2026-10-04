package com.swefton.backend.modules.machine.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.machine.dto.CreateFacilityMachineRequest;
import com.swefton.backend.modules.machine.dto.FacilityMachineResponse;
import com.swefton.backend.modules.machine.dto.UpdateFacilityMachineRequest;
import com.swefton.backend.modules.machine.entity.FacilityMachine;
import com.swefton.backend.modules.machine.entity.Machine;
import com.swefton.backend.modules.machine.enums.FacilityMachineStatus;
import com.swefton.backend.modules.machine.repository.FacilityMachineRepository;
import com.swefton.backend.modules.machine.repository.MachineRepository;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityMachineService {

    private final FacilityMachineRepository facilityMachineRepository;
    private final FacilityRepository facilityRepository;
    private final MachineRepository machineRepository;
    private final ISessionUser sessionUser;

    @Transactional(readOnly = true)
    public List<FacilityMachineResponse> findAll(Long facilityId) {
        managedFacility(facilityId);
        return facilityMachineRepository.findAllByFacilityIdOrderByMachineNameAsc(facilityId)
                .stream()
                .map(FacilityMachineResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacilityMachineResponse findById(Long facilityId, Long facilityMachineId) {
        managedFacility(facilityId);
        return new FacilityMachineResponse(findEntity(facilityId, facilityMachineId));
    }

    @Transactional
    public FacilityMachineResponse create(Long facilityId, CreateFacilityMachineRequest request) {
        Facility facility = managedFacility(facilityId);
        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found"));
        if (facility.getCategory() != null
                && !facility.getCategory().isBlank()
                && !facility.getCategory().equalsIgnoreCase(machine.getFacilityCategory())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Machine is not available for this facility category");
        }
        if (facilityMachineRepository.existsByFacilityIdAndMachineId(facilityId, machine.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This machine is already registered for the facility");
        }

        FacilityMachine facilityMachine = new FacilityMachine();
        facilityMachine.setFacility(facility);
        facilityMachine.setMachine(machine);
        applyValues(
                facilityMachine,
                request.quantity(),
                request.status(),
                request.notes(),
                request.expectedArrivalDate());
        return new FacilityMachineResponse(facilityMachineRepository.saveAndFlush(facilityMachine));
    }

    @Transactional
    public FacilityMachineResponse update(
            Long facilityId,
            Long facilityMachineId,
            UpdateFacilityMachineRequest request) {
        managedFacility(facilityId);
        FacilityMachine facilityMachine = findEntity(facilityId, facilityMachineId);
        applyValues(
                facilityMachine,
                request.quantity(),
                request.status(),
                request.notes(),
                request.expectedArrivalDate());
        return new FacilityMachineResponse(facilityMachineRepository.saveAndFlush(facilityMachine));
    }

    @Transactional
    public void delete(Long facilityId, Long facilityMachineId) {
        managedFacility(facilityId);
        FacilityMachine facilityMachine = findEntity(facilityId, facilityMachineId);
        facilityMachineRepository.delete(facilityMachine);
        facilityMachineRepository.flush();
    }

    private Facility managedFacility(Long facilityId) {
        Facility facility = facilityRepository.findByIdAndDeletedAtIsNull(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
        boolean owner = facility.getOwner().getId().equals(sessionUser.getUserId());
        boolean admin = sessionUser.hasRole(RoleCode.ADMIN);
        if (!owner && !admin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot manage this facility's machines");
        }
        return facility;
    }

    private FacilityMachine findEntity(Long facilityId, Long facilityMachineId) {
        return facilityMachineRepository.findByIdAndFacilityId(facilityMachineId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Facility machine not found"));
    }

    private void applyValues(
            FacilityMachine facilityMachine,
            Integer quantity,
            FacilityMachineStatus status,
            String notes,
            LocalDate expectedArrivalDate) {
        if (status == FacilityMachineStatus.COMING_SOON && expectedArrivalDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expected arrival date is required when a machine is coming soon");
        }
        facilityMachine.setQuantity(quantity);
        facilityMachine.setStatus(status);
        facilityMachine.setNotes(trimToNull(notes));
        facilityMachine.setExpectedArrivalDate(
                status == FacilityMachineStatus.COMING_SOON ? expectedArrivalDate : null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
