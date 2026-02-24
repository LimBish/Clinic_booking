package com.clinic.repository;

import com.clinic.model.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorId(Long doctorId);
    List<DoctorSchedule> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}
