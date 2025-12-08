package com.example.smartparkingapi.parking.service;

import com.example.smartparkingapi.parking.dto.ParkingStatusResponse;
import com.example.smartparkingapi.parking.dto.VehicleEntryResponse;
import com.example.smartparkingapi.parking.entity.ParkingRecord;
import com.example.smartparkingapi.parking.repository.ParkingRecordRepository;
import com.example.smartparkingapi.parking.entity.ParkingRecord.ParkingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParkingService {

    private final BlobStorageService blobStorageService;
    private final ComputerVisionService computerVisionService;
    private final RedisService redisService;
    private final ParkingRecordRepository parkingRecordRepository;

    /**
     * 차량 입차 처리
     * 1. 이미지를 Blob Storage에 저장
     * 2. Computer Vision으로 번호판 인식
     * 3. DB에 입차 기록 저장
     * 4. Redis 캐시 업데이트
     */
    @Transactional
    public VehicleEntryResponse processVehicleEntry(MultipartFile image) throws IOException {
        // 1. Blob Storage에 이미지 업로드
        String imageUrl = blobStorageService.uploadImage(image);
        log.info("이미지 업로드 완료: {}", imageUrl);

        // 2. Computer Vision으로 번호판 인식
        String licensePlate = computerVisionService.recognizeLicensePlate(image);

        if (licensePlate == null) {
            log.warn("번호판 인식 실패 - 이미지 URL: {}", imageUrl);
            return VehicleEntryResponse.builder()
                    .success(false)
                    .message("번호판을 인식할 수 없습니다.")
                    .imageUrl(imageUrl)
                    .build();
        }

        // 3. 이미 주차 중인 차량인지 확인
        Optional<ParkingRecord> existingRecord = parkingRecordRepository
                .findByLicensePlateAndStatus(licensePlate, ParkingStatus.PARKED);

        if (existingRecord.isPresent()) {
            log.warn("이미 주차 중인 차량: {}", licensePlate);
            return VehicleEntryResponse.builder()
                    .success(false)
                    .message("이미 주차 중인 차량입니다: " + licensePlate)
                    .licensePlate(licensePlate)
                    .imageUrl(imageUrl)
                    .build();
        }

        // 4. DB에 입차 기록 저장
        ParkingRecord record = ParkingRecord.builder()
                .licensePlate(licensePlate)
                .entryTime(LocalDateTime.now())
                .imageUrl(imageUrl)
                .status(ParkingStatus.PARKED)
                .build();

        parkingRecordRepository.save(record);
        log.info("입차 기록 저장 완료 - 번호판: {}, ID: {}", licensePlate, record.getId());

        // 5. Redis 캐시 업데이트
        updateParkingCache();

        return VehicleEntryResponse.builder()
                .success(true)
                .message("입차 처리 완료")
                .licensePlate(licensePlate)
                .imageUrl(imageUrl)
                .entryTime(record.getEntryTime())
                .recordId(record.getId())
                .build();
    }

    /**
     * 차량 출차 처리
     */
    @Transactional
    public VehicleEntryResponse processVehicleExit(String licensePlate) {
        Optional<ParkingRecord> recordOpt = parkingRecordRepository
                .findByLicensePlateAndStatus(licensePlate, ParkingStatus.PARKED);

        if (recordOpt.isEmpty()) {
            return VehicleEntryResponse.builder()
                    .success(false)
                    .message("주차 중인 차량을 찾을 수 없습니다: " + licensePlate)
                    .build();
        }

        ParkingRecord record = recordOpt.get();
        record.setExitTime(LocalDateTime.now());
        record.setStatus(ParkingStatus.EXITED);
        parkingRecordRepository.save(record);

        // Redis 캐시 업데이트
        updateParkingCache();

        log.info("출차 처리 완료 - 번호판: {}", licensePlate);

        return VehicleEntryResponse.builder()
                .success(true)
                .message("출차 처리 완료")
                .licensePlate(licensePlate)
                .entryTime(record.getEntryTime())
                .exitTime(record.getExitTime())
                .recordId(record.getId())
                .build();
    }

    /**
     * 실시간 주차 현황 조회 (Redis 캐시 우선)
     */
    public ParkingStatusResponse getParkingStatus() {
        Long occupiedCount = redisService.getOccupiedCount();
        Long availableCount = redisService.getAvailableCount();

        // 캐시 미스 시 DB에서 조회 후 캐시 업데이트
        if (occupiedCount == null || availableCount == null) {
            log.info("캐시 미스 - DB에서 조회");
            occupiedCount = parkingRecordRepository.countByStatus(ParkingStatus.PARKED);
            availableCount = (long) redisService.getTotalSpaces() - occupiedCount;
            redisService.updateParkingStatus(occupiedCount);
        }

        return ParkingStatusResponse.builder()
                .totalSpaces(redisService.getTotalSpaces())
                .occupiedSpaces(occupiedCount.intValue())
                .availableSpaces(availableCount.intValue())
                .occupancyRate((double) occupiedCount / redisService.getTotalSpaces() * 100)
                .build();
    }

    /**
     * 차량 출입 기록 조회
     */
    public List<ParkingRecord> getParkingHistory(String licensePlate) {
        if (licensePlate != null && !licensePlate.isEmpty()) {
            return parkingRecordRepository.findByLicensePlateOrderByEntryTimeDesc(licensePlate);
        }
        return parkingRecordRepository.findAll();
    }

    /**
     * 현재 주차 중인 차량 목록
     */
    public List<ParkingRecord> getCurrentlyParkedVehicles() {
        return parkingRecordRepository.findByStatus(ParkingStatus.PARKED);
    }

    /**
     * Redis 캐시 업데이트
     */
    private void updateParkingCache() {
        long occupiedCount = parkingRecordRepository.countByStatus(ParkingStatus.PARKED);
        redisService.updateParkingStatus(occupiedCount);
    }
}