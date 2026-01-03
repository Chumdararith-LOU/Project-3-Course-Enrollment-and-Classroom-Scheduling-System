package com.cource.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRequestDTO {

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotBlank(message = "Building is required")
    private String building;

    @NotNull(message = "Capacity is required")
    private Integer capacity;

    private String roomType;

    private Boolean isActive;
}
