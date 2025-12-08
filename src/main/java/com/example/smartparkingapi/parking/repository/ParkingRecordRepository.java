package com.example.smartparkingapi.parking.repository;

import com.example.smartparkingapi.parking.entity.ParkingRecord;
import com.example.smartparkingapi.parking.entity.ParkingRecord.ParkingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingRecordRepository extends JpaRepository<ParkingRecord, Long> {

    // 현재 주차 중인 차량 조회
    List<ParkingRecord> findByStatus(ParkingStatus status);

    // 번호판으로 주차 중인 차량 찾기
    Optional<ParkingRecord> findByLicensePlateAndStatus(String licensePlate, ParkingStatus status);

    // 현재 주차 중인 차량 수
    long countByStatus(ParkingStatus status);

    // 특정 번호판의 모든 기록 조회
    List<ParkingRecord> findByLicensePlateOrderByEntryTimeDesc(String licensePlate);
}