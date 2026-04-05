package com.clinic.controller;

import com.clinic.dto.Dtos.ConsultationRequest;
import com.clinic.dto.Dtos.LeaveRequest;
import com.clinic.dto.Dtos.ScheduleRequest;
import com.clinic.exception.AppException;
import com.clinic.model.Doctor;
import com.clinic.model.User;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    @Mock private AppointmentService appointmentService;
    @Mock private DoctorService doctorService;
    @Mock private UserService userService;
    @Mock private Model model;
    @Mock private RedirectAttributes flash;

    @InjectMocks private DoctorController controller;

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User.withUsername("doctor@clinic.com")
                .password("x").roles("DOCTOR").build();
    }

    private User doctorUser() {
        User user = new User();
        user.setId(5L);
        user.setEmail("doctor@clinic.com");
        return user;
    }

    private Doctor doctor() {
        Doctor d = new Doctor();
        d.setId(7L);
        return d;
    }

    @Test
    void dashboard_populatesDoctorAndAppointments() {
        when(userService.findByEmail("doctor@clinic.com")).thenReturn(doctorUser());
        when(doctorService.getDoctorByUserId(5L)).thenReturn(doctor());
        when(appointmentService.getDoctorTodayAppointments("doctor@clinic.com")).thenReturn(List.of());
        when(appointmentService.getDoctorWeekAppointments(any(), any())).thenReturn(List.of());

        String view = controller.dashboard(model, principal());

        assertEquals("doctor/dashboard", view);
        verify(model).addAttribute(eq("doctor"), any());
        verify(model).addAttribute(eq("user"), any());
        verify(model).addAttribute(eq("todayAppointments"), any());
        verify(model).addAttribute(eq("weekAppointments"), any());
        verify(model).addAttribute(eq("weekStart"), any());
        verify(model).addAttribute(eq("today"), any());
    }

    @Test
    void complete_whenError_setsFlashError() {
        ConsultationRequest req = new ConsultationRequest();
        req.setConsultationNotes("notes");
        when(appointmentService.complete(1L, "doctor@clinic.com", req)).thenThrow(new AppException("err"));

        String redirect = controller.complete(1L, req, principal(), flash);

        assertEquals("redirect:/doctor/dashboard", redirect);
        verify(flash).addFlashAttribute("error", "err");
    }

    @Test
    void saveSchedule_success_setsSuccessFlash() {
        User user = doctorUser();
        Doctor doctor = doctor();
        when(userService.findByEmail("doctor@clinic.com")).thenReturn(user);
        when(doctorService.getDoctorByUserId(5L)).thenReturn(doctor);

        ScheduleRequest req = new ScheduleRequest();
        req.setDayOfWeek("MONDAY");

        String redirect = controller.saveSchedule(req, principal(), flash);

        assertEquals("redirect:/doctor/schedule", redirect);
        verify(flash).addFlashAttribute("success", "Schedule updated for MONDAY.");
    }

    @Test
    void addLeave_success_includesAffectedCountMessage() {
        User user = doctorUser();
        Doctor doctor = doctor();
        LeaveRequest req = new LeaveRequest();
        req.setLeaveDate(LocalDate.now().plusDays(2));

        when(userService.findByEmail("doctor@clinic.com")).thenReturn(user);
        when(doctorService.getDoctorByUserId(5L)).thenReturn(doctor);
        when(appointmentService.getDoctorWeekAppointments("doctor@clinic.com", req.getLeaveDate()))
                .thenReturn(List.of(new com.clinic.model.Appointment()));

        String redirect = controller.addLeave(req, principal(), flash);

        assertEquals("redirect:/doctor/schedule", redirect);
        verify(flash).addFlashAttribute(any(), any());
    }

    @Test
    void removeLeave_whenError_setsFlashError() {
        User user = doctorUser();
        Doctor doctor = doctor();
        when(userService.findByEmail("doctor@clinic.com")).thenReturn(user);
        when(doctorService.getDoctorByUserId(5L)).thenReturn(doctor);
        doThrow(new AppException("nope")).when(doctorService).removeLeave(3L, 7L);

        String redirect = controller.removeLeave(3L, principal(), flash);

        assertEquals("redirect:/doctor/schedule", redirect);
        verify(flash).addFlashAttribute("error", "nope");
    }

    @Test
    void profile_returnsProfileView() {
        when(userService.findByEmail("doctor@clinic.com")).thenReturn(doctorUser());
        when(doctorService.getDoctorByUserId(5L)).thenReturn(doctor());

        String view = controller.profile(model, principal());

        assertEquals("doctor/profile", view);
    }
}