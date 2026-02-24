package com.clinic.repository;

import com.clinic.model.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {
    List<DoctorLeave> findByDoctorIdAndLeaveDateBetweenOrderByLeaveDate(Long doctorId, LocalDate from, LocalDate to);
    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate date);
    Optional<DoctorLeave> findByDoctorIdAndLeaveDate(Long doctorId, LocalDate date);
    List<DoctorLeave> findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDate(Long doctorId, LocalDate from);
}
