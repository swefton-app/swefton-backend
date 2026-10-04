package com.swefton.backend.modules.machine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.machine.dto.CreateFacilityMachineRequest;
import com.swefton.backend.modules.machine.dto.FacilityMachineResponse;
import com.swefton.backend.modules.machine.dto.UpdateFacilityMachineRequest;
import com.swefton.backend.modules.machine.entity.FacilityMachine;
import com.swefton.backend.modules.machine.entity.Machine;
import com.swefton.backend.modules.machine.entity.MachineMovement;
import com.swefton.backend.modules.machine.enums.FacilityMachineStatus;
import com.swefton.backend.modules.machine.enums.MachineMuscleGroup;
import com.swefton.backend.modules.video.entity.Video;
import com.swefton.backend.modules.machine.repository.FacilityMachineRepository;
import com.swefton.backend.modules.machine.repository.MachineRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class FacilityMachineServiceTests {

    @Mock
    private FacilityMachineRepository facilityMachineRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ISessionUser sessionUser;

    private FacilityMachineService service;
    private Facility facility;
    private Machine machine;

    @BeforeEach
    void setUp() {
        service = new FacilityMachineService(
                facilityMachineRepository,
                facilityRepository,
                machineRepository,
                sessionUser);

        User owner = new User();
        owner.setId(7L);
        facility = new Facility();
        facility.setId(3L);
        facility.setOwner(owner);
        facility.setCategory("GYM");
        machine = machine(5L);
    }

    @Test
    void createConnectsMachineAndFacilityWithInventoryData() {
        CreateFacilityMachineRequest request = new CreateFacilityMachineRequest(
                5L,
                4,
                FacilityMachineStatus.ACTIVE,
                " Main training floor ",
                null);
        stubManagedFacility();
        when(machineRepository.findById(5L)).thenReturn(Optional.of(machine));
        when(facilityMachineRepository.saveAndFlush(any(FacilityMachine.class)))
                .thenAnswer(invocation -> {
                    FacilityMachine saved = invocation.getArgument(0);
                    saved.setId(11L);
                    saved.prePersist();
                    return saved;
                });

        FacilityMachineResponse response = service.create(3L, request);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.facilityId()).isEqualTo(3L);
        assertThat(response.machineId()).isEqualTo(5L);
        assertThat(response.quantity()).isEqualTo(4);
        assertThat(response.status()).isEqualTo("Active");
        assertThat(response.notes()).isEqualTo("Main training floor");
    }

    @Test
    void comingSoonRequiresExpectedArrivalDate() {
        CreateFacilityMachineRequest request = new CreateFacilityMachineRequest(
                5L,
                2,
                FacilityMachineStatus.COMING_SOON,
                null,
                null);
        stubManagedFacility();
        when(machineRepository.findById(5L)).thenReturn(Optional.of(machine));

        assertThatThrownBy(() -> service.create(3L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Expected arrival date is required");
    }

    @Test
    void createRejectsMachineForAnotherFacilityCategory() {
        facility.setCategory("BOXING");
        CreateFacilityMachineRequest request = new CreateFacilityMachineRequest(
                5L,
                2,
                FacilityMachineStatus.ACTIVE,
                null,
                null);
        stubManagedFacility();
        when(machineRepository.findById(5L)).thenReturn(Optional.of(machine));

        assertThatThrownBy(() -> service.create(3L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not available for this facility category");
    }

    @Test
    void updateChangesQuantityAndRepairStatus() {
        FacilityMachine relation = relation(11L);
        UpdateFacilityMachineRequest request = new UpdateFacilityMachineRequest(
                3,
                FacilityMachineStatus.NEEDS_REPAIR,
                "Broken cable",
                LocalDate.now().plusDays(2));
        stubManagedFacility();
        when(facilityMachineRepository.findByIdAndFacilityId(11L, 3L))
                .thenReturn(Optional.of(relation));
        when(facilityMachineRepository.saveAndFlush(relation)).thenReturn(relation);

        FacilityMachineResponse response = service.update(3L, 11L, request);

        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("Needs Repair");
        assertThat(response.expectedArrivalDate()).isNull();
    }

    @Test
    void findAllReturnsOnlyTheRequestedFacilityInventory() {
        FacilityMachine relation = relation(11L);
        stubManagedFacility();
        when(facilityMachineRepository.findAllByFacilityIdOrderByMachineNameAsc(3L))
                .thenReturn(List.of(relation));

        List<FacilityMachineResponse> response = service.findAll(3L);

        assertThat(response).singleElement().extracting(FacilityMachineResponse::machineId)
                .isEqualTo(5L);
    }

    @Test
    void deleteOnlyRemovesTheFacilityRelationship() {
        FacilityMachine relation = relation(11L);
        stubManagedFacility();
        when(facilityMachineRepository.findByIdAndFacilityId(11L, 3L))
                .thenReturn(Optional.of(relation));

        service.delete(3L, 11L);

        verify(facilityMachineRepository).delete(relation);
        verify(facilityMachineRepository).flush();
    }

    private void stubManagedFacility() {
        when(facilityRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(facility));
        when(sessionUser.getUserId()).thenReturn(7L);
    }

    private FacilityMachine relation(Long id) {
        FacilityMachine relation = new FacilityMachine();
        relation.setId(id);
        relation.setFacility(facility);
        relation.setMachine(machine);
        relation.setQuantity(2);
        relation.setStatus(FacilityMachineStatus.ACTIVE);
        return relation;
    }

    private Machine machine(Long id) {
        Machine result = new Machine();
        result.setId(id);
        result.setName("Chest Press");
        result.setCode("PRESS-01");
        result.setFacilityCategory("GYM");
        MachineMovement movement = new MachineMovement();
        movement.setId(20L);
        movement.setMachine(result);
        movement.setName("Chest Press");
        movement.setMuscleGroup(MachineMuscleGroup.CHEST);
        Video video = new Video();
        video.setId(30L);
        video.setOriginalName("chest-press.mp4");
        movement.assignVideo(video);
        movement.setPosition(0);
        result.getMovements().add(movement);
        return result;
    }
}
