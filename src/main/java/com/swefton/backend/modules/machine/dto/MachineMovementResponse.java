package com.swefton.backend.modules.machine.dto;

import com.swefton.backend.modules.machine.entity.MachineMovement;
import com.swefton.backend.modules.video.api.VideoApi;

public record MachineMovementResponse(
        Long id,
        String name,
        String muscleGroup,
        Long videoId,
        String videoUrl,
        String videoOriginalName,
        String instructions,
        Integer position) {

    public MachineMovementResponse(MachineMovement entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getMuscleGroup().getLabel(),
                entity.getVideo() == null ? null : entity.getVideo().getId(),
                entity.getVideo() == null
                        ? entity.getLegacyVideoUrl()
                        : VideoApi.BASE_PATH + "/" + entity.getVideo().getId(),
                entity.getVideo() == null ? null : entity.getVideo().getOriginalName(),
                entity.getInstructions(),
                entity.getPosition());
    }
}
