package com.example.smartparkingapi.parking.controller;

import com.example.smartparkingapi.parking.dto.ParkingStatusResponse;
import com.example.smartparkingapi.parking.dto.VehicleEntryResponse;
import com.example.smartparkingapi.parking.entity.ParkingRecord;
import com.example.smartparkingapi.parking.service.ParkingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ParkingController {

    private final ParkingService parkingService;

    /**
     * 서버 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Smart Parking API");
        return ResponseEntity.ok(response);
    }

    /**
     * 차량 입차 처리
     * CCTV 이미지 업로드 → AI 분석 → DB 저장
     */
    @PostMapping(value = "/entry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "차량 입차", description = "CCTV 이미지 업로드 → AI 분석 → DB 저장")

    public ResponseEntity<VehicleEntryResponse> vehicleEntry(
            @RequestParam("image") MultipartFile image) {

        log.info("입차 요청 - 파일명: {}, 크기: {} bytes",
                image.getOriginalFilename(), image.getSize());

        try {
            VehicleEntryResponse response = parkingService.processVehicleEntry(image);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (IOException e) {
            log.error("입차 처리 실패", e);
            return ResponseEntity.internalServerError()
                    .body(VehicleEntryResponse.builder()
                            .success(false)
                            .message("서버 오류: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 차량 출차 처리
     */
    @Operation(summary = "차량 출차", description = "번호판으로 출차 처리")
    @PostMapping("/exit")
    public ResponseEntity<VehicleEntryResponse> vehicleExit(
            @RequestParam("licensePlate") String licensePlate) {

        log.info("출차 요청 - 번호판: {}", licensePlate);

        VehicleEntryResponse response = parkingService.processVehicleExit(licensePlate);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 실시간 주차 현황 조회
     */
    @Operation(summary = "주차 현황 조회", description = "실시간 주차 가능 공간 수 조회 (Redis 캐시)")
    @GetMapping("/parking/status")
    public ResponseEntity<ParkingStatusResponse> getParkingStatus() {
        log.info("주차 현황 조회 요청");
        ParkingStatusResponse response = parkingService.getParkingStatus();
        return ResponseEntity.ok(response);
    }

    /**
     * 차량 출입 기록 조회
     */
    @Operation(summary = "출입 기록 조회", description = "차량 출입 기록 조회 (번호판 필터 가능)")
    @GetMapping("/parking/history")
    public ResponseEntity<List<ParkingRecord>> getParkingHistory(
            @RequestParam(value = "licensePlate", required = false) String licensePlate) {

        log.info("출입 기록 조회 요청 - 번호판: {}", licensePlate);
        List<ParkingRecord> records = parkingService.getParkingHistory(licensePlate);
        return ResponseEntity.ok(records);
    }

    /**
     * 현재 주차 중인 차량 목록
     */
    @Operation(summary = "현재 주차 중인 차량", description = "현재 주차장에 있는 차량 목록")
    @GetMapping("/parking/current")
    public ResponseEntity<List<ParkingRecord>> getCurrentlyParkedVehicles() {
        log.info("현재 주차 중인 차량 조회 요청");
        List<ParkingRecord> records = parkingService.getCurrentlyParkedVehicles();
        return ResponseEntity.ok(records);
    }
}