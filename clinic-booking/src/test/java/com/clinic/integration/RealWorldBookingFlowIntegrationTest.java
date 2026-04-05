package com.clinic.integration;

import com.clinic.dto.Dtos.AppointmentRequest;
import com.clinic.dto.Dtos.LeaveRequest;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorSchedule;
import com.clinic.model.User;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.model.enums.Specialization;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DoctorLeaveRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.DoctorScheduleRepository;
import com.clinic.repository.UserRepository;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RealWorldBookingFlowIntegrationTest {

    @Autowired private AppointmentService appointmentService;
    @Autowired private DoctorService doctorService;

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private DoctorScheduleRepository doctorScheduleRepository;
    @Autowired private DoctorLeaveRepository doctorLeaveRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User patient;
    private Doctor doctor;
    private LocalDate nextMonday;

    @BeforeEach
    void setup() {
        appointmentRepository.deleteAll();
        doctorLeaveRepository.deleteAll();
        doctorScheduleRepository.deleteAll();
        doctorRepository.deleteAll();
        userRepository.deleteAll();

        patient = userRepository.save(new User(
                "Patient One", "patient.flow@clinic.com", passwordEncoder.encode("patient123"), "999", Role.PATIENT));

        User doctorUser = userRepository.save(new User(
                "Dr. Integration", "doctor.flow@clinic.com", passwordEncoder.encode("doctor123"), "888", Role.DOCTOR));

        doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization(Specialization.CARDIOLOGY);
        doctor.setConsultationFee(1000);
        doctor = doctorRepository.save(doctor);

        nextMonday = nextDayOfWeek(DayOfWeek.MONDAY);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(11, 0));
        schedule.setSlotDurationMinutes(30);
        doctorScheduleRepository.save(schedule);
    }

    @Test
    void patientBooksThenCancelsAppointment_realWorldFlow() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctor.getId());
        req.setAppointmentDate(nextMonday);
        req.setAppointmentTime("09:30");
        req.setReason("Chest discomfort");

        Appointment booked = appointmentService.book(patient.getEmail(), req);

        assertNotNull(booked.getId());
        assertEquals(AppointmentStatus.CONFIRMED, booked.getStatus());

        List<String> slotsAfterBooking = appointmentService.getAvailableSlots(doctor.getId(), nextMonday);
        assertFalse(slotsAfterBooking.contains("09:30"));

        Appointment cancelled = appointmentService.cancel(booked.getId(), patient.getEmail());
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());

        List<String> slotsAfterCancel = appointmentService.getAvailableSlots(doctor.getId(), nextMonday);
        assertTrue(slotsAfterCancel.contains("09:30"));
    }

    @Test
    void doctorMarksLeave_autoCancelsConfirmedAppointments() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctor.getId());
        req.setAppointmentDate(nextMonday);
        req.setAppointmentTime("10:00");
        req.setReason("Routine follow-up");

        Appointment booked = appointmentService.book(patient.getEmail(), req);
        assertEquals(AppointmentStatus.CONFIRMED, booked.getStatus());

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setLeaveDate(nextMonday);
        leaveRequest.setReason("Medical conference");

        doctorService.addLeave(doctor.getId(), leaveRequest);

        Appointment refreshed = appointmentRepository.findById(booked.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, refreshed.getStatus());

        assertTrue(appointmentService.getAvailableSlots(doctor.getId(), nextMonday).isEmpty());
    }

    private LocalDate nextDayOfWeek(DayOfWeek day) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        return date;
    }
}