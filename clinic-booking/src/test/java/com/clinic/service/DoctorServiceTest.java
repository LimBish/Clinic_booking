package com.clinic.service;

import com.clinic.dto.Dtos.DoctorRequest;
import com.clinic.dto.Dtos.LeaveRequest;
import com.clinic.dto.Dtos.ScheduleRequest;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorLeave;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepo;
    @Mock private UserRepository userRepo;
    @Mock private DoctorScheduleRepository scheduleRepo;
    @Mock private DoctorLeaveRepository leaveRepo;
    @Mock private AppointmentRepository appointmentRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private NotificationService notificationService;

    @InjectMocks private DoctorService doctorService;

    private Doctor doctor;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(11L);
        user.setFullName("Dr. House");
        user.setEmail("house@clinic.com");

        doctor = new Doctor();
        doctor.setId(7L);
        doctor.setUser(user);
        doctor.setActive(true);
    }

    @Test
    void createDoctor_whenEmailAlreadyExists_throwsAppException() {
        DoctorRequest req = new DoctorRequest();
        req.setEmail("house@clinic.com");

        when(userRepo.existsByEmail("house@clinic.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> doctorService.createDoctor(req));
        assertEquals("An account with this email already exists.", ex.getMessage());
    }

    @Test
    void createDoctor_whenValid_createsUserAndDoctor() {
        DoctorRequest req = new DoctorRequest();
        req.setFullName("Dr. Who");
        req.setEmail("who@clinic.com");
        req.setPassword("secret");
        req.setPhone("111");
        req.setSpecialization(Specialization.CARDIOLOGY);
        req.setBio("Bio");
        req.setConsultationFee(1000);

        when(userRepo.existsByEmail(req.getEmail())).thenReturn(false);
        when(encoder.encode("secret")).thenReturn("enc-secret");
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(doctorRepo.save(any(Doctor.class))).thenAnswer(i -> i.getArgument(0));

        Doctor saved = doctorService.createDoctor(req);

        assertEquals(Specialization.CARDIOLOGY, saved.getSpecialization());
        assertEquals("enc-secret", saved.getUser().getPassword());
        assertEquals(Role.DOCTOR, saved.getUser().getRole());
    }

    @Test
    void setSchedule_whenInvalidDay_throwsAppException() {
        ScheduleRequest req = new ScheduleRequest();
        req.setDayOfWeek("FUNDAY");
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        req.setSlotDurationMinutes(30);

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> doctorService.setSchedule(7L, req));
        assertEquals("Invalid day: FUNDAY", ex.getMessage());
    }

    @Test
    void setSchedule_whenEndBeforeStart_throwsAppException() {
        ScheduleRequest req = new ScheduleRequest();
        req.setDayOfWeek("MONDAY");
        req.setStartTime("10:00");
        req.setEndTime("09:00");
        req.setSlotDurationMinutes(30);

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> doctorService.setSchedule(7L, req));
        assertEquals("End time must be after start time.", ex.getMessage());
    }

    @Test
    void setSchedule_whenOverlapping_throwsAppException() {
        ScheduleRequest req = new ScheduleRequest();
        req.setDayOfWeek("MONDAY");
        req.setStartTime("09:30");
        req.setEndTime("10:30");
        req.setSlotDurationMinutes(30);

        DoctorSchedule existing = new DoctorSchedule();
        existing.setStartTime(LocalTime.of(9, 0));
        existing.setEndTime(LocalTime.of(10, 0));

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of(existing));

        AppException ex = assertThrows(AppException.class, () -> doctorService.setSchedule(7L, req));
        assertEquals("This time range overlaps with an existing schedule on MONDAY.", ex.getMessage());
    }

    @Test
    void setSchedule_whenValid_savesAndNotifies() {
        ScheduleRequest req = new ScheduleRequest();
        req.setDayOfWeek("monday");
        req.setStartTime("09:00");
        req.setEndTime("12:00");
        req.setSlotDurationMinutes(30);

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));
        when(scheduleRepo.findByDoctorIdAndDayOfWeek(7L, DayOfWeek.MONDAY)).thenReturn(List.of());

        doctorService.setSchedule(7L, req);

        verify(scheduleRepo).save(any(DoctorSchedule.class));
        verify(notificationService).sendScheduleUpdated(doctor, "MONDAY");
    }

    @Test
    void addLeave_whenDateInPast_throwsAppException() {
        LeaveRequest req = new LeaveRequest();
        req.setLeaveDate(LocalDate.now().minusDays(1));
        req.setReason("Vacation");

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> doctorService.addLeave(7L, req));
        assertEquals("Cannot mark past dates as leave.", ex.getMessage());
    }

    @Test
    void addLeave_whenAlreadyMarked_throwsAppException() {
        LeaveRequest req = new LeaveRequest();
        req.setLeaveDate(LocalDate.now().plusDays(1));
        req.setReason("Vacation");

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));
        when(leaveRepo.existsByDoctorIdAndLeaveDate(7L, req.getLeaveDate())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> doctorService.addLeave(7L, req));
        assertEquals("You have already marked " + req.getLeaveDate() + " as leave.", ex.getMessage());
    }

    @Test
    void addLeave_whenAppointmentsExist_cancelsConfirmedOnly() {
        LeaveRequest req = new LeaveRequest();
        req.setLeaveDate(LocalDate.now().plusDays(2));
        req.setReason("Conference");

        Appointment confirmed = new Appointment();
        confirmed.setStatus(AppointmentStatus.CONFIRMED);
        Appointment completed = new Appointment();
        completed.setStatus(AppointmentStatus.COMPLETED);

        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));
        when(leaveRepo.existsByDoctorIdAndLeaveDate(7L, req.getLeaveDate())).thenReturn(false);
        when(appointmentRepo.findByDoctorIdAndAppointmentDateOrderByAppointmentTime(7L, req.getLeaveDate()))
                .thenReturn(List.of(confirmed, completed));
        when(leaveRepo.save(any(DoctorLeave.class))).thenAnswer(i -> i.getArgument(0));

        DoctorLeave leave = doctorService.addLeave(7L, req);

        assertEquals(req.getLeaveDate(), leave.getLeaveDate());
        assertEquals(AppointmentStatus.CANCELLED, confirmed.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());

        verify(appointmentRepo).save(confirmed);
        verify(appointmentRepo, never()).save(completed);
        verify(notificationService).sendLeaveAffectedPatient(confirmed, "Conference");
        verify(notificationService).sendLeaveNotification(doctor, req.getLeaveDate(), "Conference");
    }

    @Test
    void removeLeave_whenBelongsToAnotherDoctor_throwsAppException() {
        Doctor anotherDoctor = new Doctor();
        anotherDoctor.setId(88L);

        DoctorLeave leave = new DoctorLeave();
        leave.setDoctor(anotherDoctor);

        when(leaveRepo.findById(3L)).thenReturn(Optional.of(leave));

        AppException ex = assertThrows(AppException.class, () -> doctorService.removeLeave(3L, 7L));
        assertEquals("You can only remove your own leave entries.", ex.getMessage());
    }

    @Test
    void getDoctorByUserId_whenMissing_throwsNotFound() {
        when(doctorRepo.findByUserId(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> doctorService.getDoctorByUserId(99L));
        assertEquals("Doctor profile not found", ex.getMessage());
    }

    @Test
    void toggleActive_flipsAndSavesDoctor() {
        when(doctorRepo.findById(7L)).thenReturn(Optional.of(doctor));

        doctorService.toggleActive(7L);

        assertEquals(false, doctor.isActive());
        verify(doctorRepo).save(doctor);
    }
}