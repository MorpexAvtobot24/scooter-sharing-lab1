package com.course.app.scooter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScooterRepository extends JpaRepository<Scooter, Long> {

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);

    List<Scooter> findByStatusInAndBatteryLevelLessThan(List<ScooterStatus> statuses, Integer batteryLevel);

    List<Scooter> findByStatusIn(List<ScooterStatus> statuses, Pageable pageable);
}
