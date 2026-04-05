package com.clinic.model;

import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.model.enums.Specialization;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ModelCoverageTest {

    @Test
    void user_constructorAndPrePersist_workAsExpected() {
        User user = new User("Alice", "alice@clinic.com", "pw", "123", Role.PATIENT);
        assertEquals("Alice", user.getFullName());
        assertNull(user.getCreatedAt());

        user.prePersist();

        assertNotNull(user.getCreatedAt());
    }

    @Test
    void doctor_and_schedule_fields_areSettable() {
        User doctorUser = new User();
        doctorUser.setFullName("Dr. Strange");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setUser(doctorUser);
        doctor.setSpecialization(Specialization.NEUROLOGY);
        doctor.setBio("Senior consultant");
        doctor.setConsultationFee(1500);
        doctor.setActive(true);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setSlotDurationMinutes(30);

        assertEquals("Dr. Strange", schedule.getDoctor().getUser().getFullName());
        assertEquals(30, schedule.getSlotDurationMinutes());
    }

    @Test
    void appointment_and_leave_objects_holdDomainState() {
        User patient = new User();
        patient.setFullName("Patient A");

        Doctor doctor = new Doctor();
        doctor.setUser(new User());
        doctor.getUser().setFullName("Doctor B");

        Appointment appt = new Appointment();
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setAppointmentDate(LocalDate.now().plusDays(1));
        appt.setAppointmentTime(LocalTime.of(10, 0));
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appt.setConsultationNotes("All good");
        appt.setConsultedAt(LocalDateTime.now());

        DoctorLeave leave = new DoctorLeave(doctor, LocalDate.now().plusDays(2), "Conference");

        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());
        assertEquals("Conference", leave.getReason());
        assertEquals("Doctor B", leave.getDoctor().getUser().getFullName());
    }
}