package com.example.smartparkingapi.parking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingStatusResponse {

    private int totalSpaces;       // 총 주차 공간
    private int occupiedSpaces;    // 사용 중인 공간
    private int availableSpaces;   // 이용 가능한 공간
    private double occupancyRate;  // 점유율 (%)
}