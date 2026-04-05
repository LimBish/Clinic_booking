package com.clinic.controller;

import com.clinic.dto.Dtos.AppointmentRequest;
import com.clinic.exception.AppException;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorLeave;
import com.clinic.model.DoctorSchedule;
import com.clinic.model.User;
import com.clinic.model.enums.Specialization;
import com.clinic.repository.DoctorLeaveRepository;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock private AppointmentService appointmentService;
    @Mock private DoctorService doctorService;
    @Mock private DoctorLeaveRepository leaveRepo;
    @Mock private UserService userService;

    @Mock private Model model;
    @Mock private RedirectAttributes flash;

    private PatientController controller;


    @BeforeEach
    void init() {
        controller = new PatientController(appointmentService, doctorService, leaveRepo, userService, new ObjectMapper());
    }

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User.withUsername("patient@clinic.com")
                .password("x").roles("PATIENT").build();
    }

    @Test
    void dashboard_populatesUpcomingAndUser() {
        when(appointmentService.getPatientUpcoming("patient@clinic.com")).thenReturn(List.of());
        when(userService.findByEmail("patient@clinic.com")).thenReturn(new User());

        String view = controller.dashboard(model, principal());

        assertEquals("patient/dashboard", view);
        verify(model).addAttribute(eq("upcoming"), any());
        verify(model).addAttribute(eq("user"), any());
    }

    @Test
    void bookPage_withSpecialization_filtersDoctors() throws Exception {
        Doctor d1 = doctor(1L, true, Specialization.CARDIOLOGY, "Dr A");
        Doctor d2 = doctor(2L, false, Specialization.CARDIOLOGY, "Dr B");
        when(doctorService.getAll()).thenReturn(List.of(d1, d2));
        when(doctorService.getSchedules(1L)).thenReturn(List.of(schedule(DayOfWeek.MONDAY)));

        String view = controller.bookPage(model, "CARDIOLOGY", principal());

        assertEquals("patient/book", view);
        verify(model).addAttribute(eq("specializations"), any());
        verify(model).addAttribute(eq("doctors"), any());
        verify(model).addAttribute(eq("doctorsJson"), any());
    }

    @Test
    void book_whenAppException_returnsBookViewWithError() throws Exception {
        AppointmentRequest req = new AppointmentRequest();
        when(appointmentService.book(any(), any())).thenThrow(new AppException("slot taken"));
        when(doctorService.getAllActive()).thenReturn(List.of(doctor(1L, true, Specialization.CARDIOLOGY, "Dr A")));
        when(doctorService.getSchedules(1L)).thenReturn(List.of(schedule(DayOfWeek.MONDAY)));

        String view = controller.book(req, principal(), model);

        assertEquals("patient/book", view);
        verify(model).addAttribute("error", "slot taken");
    }

    @Test
    void cancel_onError_setsFlashError() {
        when(appointmentService.cancel(10L, "patient@clinic.com")).thenThrow(new AppException("cannot cancel"));

        String redirect = controller.cancel(10L, principal(), flash);

        assertEquals("redirect:/patient/appointments", redirect);
        verify(flash).addFlashAttribute("error", "cannot cancel");
    }

    @Test
    void getSlots_returnsServiceResponse() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(appointmentService.getAvailableSlots(5L, date)).thenReturn(List.of("10:00"));

        var res = controller.getSlots(5L, date);

        assertEquals(200, res.getStatusCode().value());
        assertEquals(List.of("10:00"), res.getBody());
    }

    @Test
    void getDoctorAvailability_returnsWorkingDaysAndLeaveDates() {
        DoctorSchedule schedule = schedule(DayOfWeek.MONDAY);
        DoctorLeave leave = new DoctorLeave();
        leave.setLeaveDate(LocalDate.now().plusDays(5));

        when(doctorService.getSchedules(7L)).thenReturn(List.of(schedule));
        when(leaveRepo.findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDate(7L, LocalDate.now()))
                .thenReturn(List.of(leave));

        var res = controller.getDoctorAvailability(7L);

        assertEquals(200, res.getStatusCode().value());
    }

    private Doctor doctor(Long id, boolean active, Specialization specialization, String name) {
        User user = new User();
        user.setFullName(name);

        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setActive(active);
        doctor.setSpecialization(specialization);
        doctor.setConsultationFee(100);
        doctor.setUser(user);
        return doctor;
    }

    private DoctorSchedule schedule(DayOfWeek day) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(10, 0));
        return schedule;
    }
}