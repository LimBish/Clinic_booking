package com.clinic.repository;

import com.clinic.model.Doctor;
import com.clinic.model.enums.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findByActiveTrue();
    List<Doctor> findBySpecializationAndActiveTrue(Specialization specialization);
}
