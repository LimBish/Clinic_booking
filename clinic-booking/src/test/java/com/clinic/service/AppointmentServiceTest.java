package com.clinic.service;

import com.clinic.dto.Dtos.AppointmentRequest;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorSchedule;
import com.clinic.model.User;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepo;
    @Mock
    private DoctorRepository doctorRepo;
    @Mock
    private DoctorScheduleRepository scheduleRepo;
    @Mock
    private DoctorLeaveRepository leaveRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppointmentService appointmentService;

    private AppointmentRequest request;

    @BeforeEach
    void setUp() {
        request = new AppointmentRequest();
        request.setDoctorId(10L);
        request.setAppointmentDate(LocalDate.now().plusDays(1));
        request.setAppointmentTime("10:00");
        request.setReason("General checkup");
    }

    @Test
    void book_whenPatientNotFound_throwsNotFoundException() {
        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("User not found with id: patient@example.com", ex.getMessage());
        verify(doctorRepo, never()).findById(any());
    }

    @Test
    void book_whenDoctorNotFound_throwsNotFoundException() {
        User patient = buildPatient("patient@example.com");
        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(10L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("Doctor not found with id: 10", ex.getMessage());
    }

    @Test
    void book_whenAppointmentDateInPast_throwsAppException() {
        User patient = buildPatient("patient@example.com");
        Doctor doctor = buildActiveDoctor();
        request.setAppointmentDate(LocalDate.now().minusDays(1));

        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(10L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("Appointment date cannot be in the past.", ex.getMessage());
    }

    @Test
    void book_whenDoctorHasNoSchedule_showsReadableDayNameInError() {
        LocalDate nextMonday = LocalDate.now().plusDays((8 - LocalDate.now().getDayOfWeek().getValue()) % 7);
        if (nextMonday.equals(LocalDate.now())) {
            nextMonday = nextMonday.plusWeeks(1);
        }

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(100L);
        request.setAppointmentDate(nextMonday);
        request.setAppointmentTime("10:00");

        User patient = new User();
        patient.setEmail("patient@example.com");

        User doctorUser = new User();
        doctorUser.setFullName("Dr. Adams");

        Doctor doctor = new Doctor();
        doctor.setId(100L);
        doctor.setUser(doctorUser);
        doctor.setActive(true);

        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(100L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(doctor.getId(), nextMonday.getDayOfWeek())).thenReturn(List.of());

        AppException exception = assertThrows(AppException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("Dr. Adams is not available on Mondays.", exception.getMessage());
    }

    @Test
    void book_whenSlotAlreadyTaken_throwsAppException() {
        User patient = buildPatient("patient@example.com");
        Doctor doctor = buildActiveDoctor();
        DayOfWeek day = request.getAppointmentDate().getDayOfWeek();

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(30);

        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(10L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(doctor.getId(), day)).thenReturn(List.of(schedule));
        when(leaveRepo.existsByDoctorIdAndLeaveDate(doctor.getId(), request.getAppointmentDate())).thenReturn(false);
        when(appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                doctor.getId(), request.getAppointmentDate(), LocalTime.of(10, 0), com.clinic.model.enums.AppointmentStatus.CANCELLED
        )).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("This time slot is already booked. Please choose another.", ex.getMessage());
    }

    private User buildPatient(String email) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        return user;
    }

    private Doctor buildActiveDoctor() {
        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setActive(true);
        return doctor;
    }

    @Test
    void book_whenValidRequest_savesAppointmentAndSendsNotification() {
        User patient = buildPatient("patient@example.com");
        Doctor doctor = buildActiveDoctor();
        DayOfWeek day = request.getAppointmentDate().getDayOfWeek();

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(30);

        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(10L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(doctor.getId(), day)).thenReturn(List.of(schedule));
        when(leaveRepo.existsByDoctorIdAndLeaveDate(doctor.getId(), request.getAppointmentDate())).thenReturn(false);
        when(appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                doctor.getId(),
                request.getAppointmentDate(),
                LocalTime.of(10, 0),
                com.clinic.model.enums.AppointmentStatus.CANCELLED
        )).thenReturn(false);

        when(appointmentRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = appointmentService.book("patient@example.com", request);

        assertEquals(LocalTime.of(10, 0), result.getAppointmentTime());
        assertEquals(request.getReason(), result.getReason());

        verify(appointmentRepo).save(any());
        verify(notificationService).sendConfirmation(any());
    }


    @Test
    void book_whenDoctorOnLeave_throwsAppException() {
        User patient = buildPatient("patient@example.com");

        // Doctor needs a user fullName for message
        User doctorUser = new User();
        doctorUser.setFullName("Dr. Smith");

        Doctor doctor = buildActiveDoctor();
        doctor.setUser(doctorUser);

        DayOfWeek day = request.getAppointmentDate().getDayOfWeek();

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(30);

        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(10L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(doctor.getId(), day)).thenReturn(List.of(schedule));
        when(leaveRepo.existsByDoctorIdAndLeaveDate(doctor.getId(), request.getAppointmentDate())).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("Dr. Dr. Smith is on leave on " + request.getAppointmentDate()
                + ". Please choose another date.", ex.getMessage());

        verify(appointmentRepo, never()).save(any());
        verify(notificationService, never()).sendConfirmation(any());
    }

    @Test
    void book_whenTimeDoesNotMatchSlotIntervals_throwsAppException() {
        User patient = buildPatient("patient@example.com");
        Doctor doctor = buildActiveDoctor();

        request.setAppointmentTime("10:05"); // invalid for 30-min slots
        DayOfWeek day = request.getAppointmentDate().getDayOfWeek();

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(30);

        when(userRepo.findByEmail("patient@example.com")).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(10L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(doctor.getId(), day)).thenReturn(List.of(schedule));
        when(leaveRepo.existsByDoctorIdAndLeaveDate(doctor.getId(), request.getAppointmentDate())).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.book("patient@example.com", request));

        assertEquals("Selected time is outside of the doctor's working hours or does not match slot intervals.",
                ex.getMessage());

        verify(appointmentRepo, never()).save(any());
        verify(notificationService, never()).sendConfirmation(any());
    }



    @Test
    void getAvailableSlots_whenDoctorOnLeave_returnsEmptyAndSkipsScheduleQuery() {
        LocalDate date = LocalDate.now().plusDays(1);

        when(leaveRepo.existsByDoctorIdAndLeaveDate(10L, date)).thenReturn(true);

        List<String> slots = appointmentService.getAvailableSlots(10L, date);

        assertEquals(List.of(), slots);
        verify(scheduleRepo, never()).findByDoctorIdAndDayOfWeek(any(), any());
    }

    @Test
    void complete_whenCancelled_throwsAppException() {
        var req = new com.clinic.dto.Dtos.ConsultationRequest();
        req.setConsultationNotes("Notes");

        User docUser = new User();
        docUser.setEmail("doc@test.com");

        Doctor doctor = new Doctor();
        doctor.setUser(docUser);

        var appt = new com.clinic.model.Appointment();
        appt.setDoctor(doctor);
        appt.setStatus(com.clinic.model.enums.AppointmentStatus.CANCELLED);

        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appt));

        AppException ex = assertThrows(AppException.class,
                () -> appointmentService.complete(1L, "doc@test.com", req));

        assertEquals("Cannot mark a cancelled appointment as consulted.", ex.getMessage());
        verify(appointmentRepo, never()).save(any());
        verify(notificationService, never()).sendConsultationComplete(any());
    }

    @Test
    void cancel_whenValidRequest_marksCancelledAndNotifies() {
        User patient = buildPatient("patient@example.com");

        Doctor doctor = buildActiveDoctor();

        var appt = new com.clinic.model.Appointment();
        appt.setId(5L);
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setStatus(com.clinic.model.enums.AppointmentStatus.CONFIRMED);

        when(appointmentRepo.findById(5L)).thenReturn(Optional.of(appt));
        when(appointmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = appointmentService.cancel(5L, "patient@example.com");

        assertEquals(com.clinic.model.enums.AppointmentStatus.CANCELLED, saved.getStatus());
        verify(appointmentRepo).save(appt);
        verify(notificationService).sendCancellation(appt);
    }


}