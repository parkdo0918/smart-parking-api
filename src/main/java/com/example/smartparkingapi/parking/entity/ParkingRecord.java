package com.example.smartparkingapi.parking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;  // 차량 번호판

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;  // 입차 시간

    @Column(name = "exit_time")
    private LocalDateTime exitTime;  // 출차 시간

    @Column(name = "image_url", length = 500)
    private String imageUrl;  // 저장된 이미지 URL

    @Column(name = "parking_space")
    private Integer parkingSpace;  // 주차 위치

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingStatus status;  // 주차 상태

    public enum ParkingStatus {
        PARKED,     // 주차 중
        EXITED      // 출차 완료
    }
}
