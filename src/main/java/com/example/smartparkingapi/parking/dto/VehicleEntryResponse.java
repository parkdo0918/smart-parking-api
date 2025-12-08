package com.example.smartparkingapi.parking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleEntryResponse {

    private boolean success;
    private String message;
    private String licensePlate;
    private String imageUrl;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Long recordId;
}