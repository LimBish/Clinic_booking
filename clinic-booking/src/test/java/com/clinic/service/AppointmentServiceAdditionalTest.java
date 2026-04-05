package com.clinic.service;

import com.clinic.dto.Dtos.ConsultationRequest;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorSchedule;
import com.clinic.model.User;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DoctorLeaveRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.DoctorScheduleRepository;
import com.clinic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceAdditionalTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private DoctorRepository doctorRepo;
    @Mock private DoctorScheduleRepository scheduleRepo;
    @Mock private DoctorLeaveRepository leaveRepo;
    @Mock private UserRepository userRepo;
    @Mock private NotificationService notificationService;

    @InjectMocks private AppointmentService appointmentService;

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@clinic.com");
        patient.setFullName("Patient");

        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@clinic.com");
        doctorUser.setFullName("Doctor");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

        appointment = new Appointment();
        appointment.setId(99L);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setAppointmentTime(LocalTime.of(10, 0));
        appointment.setStatus(AppointmentStatus.CONFIRMED);
    }

    @Test
    void getAvailableSlots_whenPastDate_returnsEmpty() {
        assertTrue(appointmentService.getAvailableSlots(10L, LocalDate.now().minusDays(1)).isEmpty());
    }

    @Test
    void getAvailableSlots_whenDoctorOnLeave_returnsEmpty() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(leaveRepo.existsByDoctorIdAndLeaveDate(10L, tomorrow)).thenReturn(true);

        assertTrue(appointmentService.getAvailableSlots(10L, tomorrow).isEmpty());
    }

    @Test
    void getAvailableSlots_whenSchedulesPresent_excludesTakenSlots() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(10, 0));
        schedule.setSlotDurationMinutes(30);

        when(leaveRepo.existsByDoctorIdAndLeaveDate(10L, tomorrow)).thenReturn(false);
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(10L, tomorrow.getDayOfWeek())).thenReturn(List.of(schedule));
        when(appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                10L, tomorrow, LocalTime.of(9, 0), AppointmentStatus.CANCELLED)).thenReturn(true);
        when(appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                10L, tomorrow, LocalTime.of(9, 30), AppointmentStatus.CANCELLED)).thenReturn(false);

        List<String> slots = appointmentService.getAvailableSlots(10L, tomorrow);

        assertEquals(List.of("09:30"), slots);
    }

    @Test
    void complete_whenDoctorMismatch_throwsAppException() {
        when(appointmentRepo.findById(99L)).thenReturn(Optional.of(appointment));

        ConsultationRequest req = new ConsultationRequest();
        req.setConsultationNotes("Done");

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.complete(99L, "another@clinic.com", req));

        assertEquals("You can only update your own appointments.", ex.getMessage());
    }

    @Test
    void complete_whenCancelled_throwsAppException() {
        appointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepo.findById(99L)).thenReturn(Optional.of(appointment));

        ConsultationRequest req = new ConsultationRequest();
        req.setConsultationNotes("Done");

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.complete(99L, "doctor@clinic.com", req));

        assertEquals("Cannot mark a cancelled appointment as consulted.", ex.getMessage());
    }

    @Test
    void complete_whenValid_updatesAndNotifies() {
        when(appointmentRepo.findById(99L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        ConsultationRequest req = new ConsultationRequest();
        req.setConsultationNotes("Recovered");

        Appointment saved = appointmentService.complete(99L, "doctor@clinic.com", req);

        assertEquals(AppointmentStatus.COMPLETED, saved.getStatus());
        assertEquals("Recovered", saved.getConsultationNotes());
        assertNotNull(saved.getConsultedAt());
        assertTrue(saved.getConsultedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(notificationService).sendConsultationComplete(saved);
    }

    @Test
    void cancel_whenWrongPatient_throwsAppException() {
        when(appointmentRepo.findById(99L)).thenReturn(Optional.of(appointment));

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.cancel(99L, "wrong@clinic.com"));

        assertEquals("You do not have permission to modify this appointment.", ex.getMessage());
    }

    @Test
    void cancel_whenCompleted_throwsAppException() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepo.findById(99L)).thenReturn(Optional.of(appointment));

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.cancel(99L, "patient@clinic.com"));

        assertEquals("Completed appointments cannot be cancelled.", ex.getMessage());
    }

    @Test
    void cancel_whenValid_setsCancelledAndNotifies() {
        when(appointmentRepo.findById(99L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment saved = appointmentService.cancel(99L, "patient@clinic.com");

        assertEquals(AppointmentStatus.CANCELLED, saved.getStatus());
        verify(notificationService).sendCancellation(saved);
    }

    @Test
    void getPatientHistory_whenUserMissing_throwsNotFound() {
        when(userRepo.findByEmail("missing@clinic.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> appointmentService.getPatientHistory("missing@clinic.com"));

        assertEquals("User not found with id: missing@clinic.com", ex.getMessage());
    }

    @Test
    void doctorAppointmentQueries_resolveDoctorByEmailAndDelegate() {
        User doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("doctor@clinic.com");

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(doctorUser);

        when(userRepo.findByEmail("doctor@clinic.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepo.findByUserId(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepo.findByDoctorIdAndAppointmentDateOrderByAppointmentTime(10L, LocalDate.now()))
                .thenReturn(List.of(appointment));
        when(appointmentRepo.findByDoctorIdAndAppointmentDateOrderByAppointmentTime(10L, LocalDate.now().plusDays(1)))
                .thenReturn(List.of(appointment));
        when(appointmentRepo.findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
                10L, LocalDate.now(), LocalDate.now().plusDays(6))).thenReturn(List.of(appointment));

        assertEquals(1, appointmentService.getDoctorTodayAppointments("doctor@clinic.com").size());
        assertEquals(1, appointmentService.getDoctorAppointmentsForDate("doctor@clinic.com", LocalDate.now().plusDays(1)).size());
        assertEquals(1, appointmentService.getDoctorWeekAppointments("doctor@clinic.com", LocalDate.now()).size());
    }
}