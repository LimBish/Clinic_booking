package com.clinic.repository;

import com.clinic.model.Appointment;
import com.clinic.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDateOrderByAppointmentTime(Long doctorId, LocalDate date);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long doctorId, LocalDate date, LocalTime time, AppointmentStatus status);

    long countByStatus(AppointmentStatus status);

    List<Appointment> findByPatientIdAndStatusIn(Long patientId, List<AppointmentStatus> statuses);

    // For doctor weekly view: appointments in a date range
    List<Appointment> findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
            Long doctorId, LocalDate from, LocalDate to);

    // Admin: all appointments in date range
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
            LocalDate from, LocalDate to);

    // Count completed consultations per doctor
    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
}
