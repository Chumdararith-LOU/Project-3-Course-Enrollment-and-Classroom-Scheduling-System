package com.cource.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {
    private Long id;
    private String roomNumber;
    private String building;
    private Integer capacity;
    private String roomType;
    private Boolean isActive;

}
