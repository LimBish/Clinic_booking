package com.clinic.controller;

import com.clinic.dto.Dtos.AdminDashboardPatientStats;
import com.clinic.dto.Dtos.DoctorRequest;
import com.clinic.exception.AppException;
import com.clinic.model.User;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DoctorLeaveRepository;
import com.clinic.service.AdminPatientService;
import com.clinic.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminControllerTest {

    @Mock private DoctorService doctorService;
    @Mock private AppointmentRepository appointmentRepo;
    @Mock private DoctorLeaveRepository leaveRepo;
    @Mock private AdminPatientService adminPatientService;

    @Mock private Model model;
    @Mock private RedirectAttributes flash;

    @InjectMocks private AdminController controller;

    @Test
    void dashboard_populatesMetricsAndReturnsView() {
        AdminDashboardPatientStats stats = new AdminDashboardPatientStats();
        stats.setTotalPatients(2);
        stats.setNewPatientsThisMonth(1);
        stats.setActivePatients(1);

        when(adminPatientService.getDashboardPatientStats()).thenReturn(stats);
        when(adminPatientService.getMostFrequentPatients(5)).thenReturn(List.of());
        when(adminPatientService.getNoShowPatients()).thenReturn(List.of());
        when(doctorService.getAll()).thenReturn(List.of());
        when(appointmentRepo.countByStatus(AppointmentStatus.CONFIRMED)).thenReturn(3L);
        when(appointmentRepo.countByStatus(AppointmentStatus.COMPLETED)).thenReturn(4L);
        when(appointmentRepo.findAll()).thenReturn(List.of());
        when(leaveRepo.findAll()).thenReturn(List.of());

        String view = controller.dashboard(model);

        assertEquals("admin/dashboard", view);
        verify(model).addAttribute(eq("totalDoctors"), any());
        verify(model).addAttribute(eq("totalPatients"), any());
        verify(model).addAttribute(eq("newPatientsThisMonth"), any());
        verify(model).addAttribute(eq("activePatients"), any());
        verify(model).addAttribute(eq("frequentPatients"), any());
        verify(model).addAttribute(eq("noShowPatients"), any());
        verify(model).addAttribute(eq("mostFrequentPatients"), any());
        verify(model).addAttribute(eq("noShowPatientList"), any());
        verify(model).addAttribute(eq("confirmedAppointments"), any());
        verify(model).addAttribute(eq("completedAppointments"), any());
        verify(model).addAttribute(eq("recentAppointments"), any());
        verify(model).addAttribute(eq("doctorLeaves"), any());
    }

    @Test
    void patients_passesFiltersToModel() {
        when(adminPatientService.getPatients("q", "ACTIVE", "RECENT", "FEW")).thenReturn(List.of());

        String view = controller.patients("q", "ACTIVE", "RECENT", "FEW", model);

        assertEquals("admin/patients", view);
        verify(model).addAttribute("q", "q");
    }

    @Test
    void patientProfile_returnsProfileView() {
        when(adminPatientService.getPatient(10L)).thenReturn(new User());
        when(adminPatientService.getPatientAppointments(10L)).thenReturn(List.of());

        String view = controller.patientProfile(10L, model);

        assertEquals("admin/patient-profile", view);
    }

    @Test
    void togglePatient_withProfileBack_redirectsProfile() {
        String view = controller.togglePatient(5L, "profile", flash);

        assertEquals("redirect:/admin/patients/5", view);
    }

    @Test
    void createDoctor_whenError_returnsForm() {
        DoctorRequest req = new DoctorRequest();
        when(doctorService.createDoctor(req)).thenThrow(new AppException("exists"));

        String view = controller.createDoctor(req, model);

        assertEquals("admin/doctor-form", view);
        verify(model).addAttribute("error", "exists");
    }

    @Test
    void setSchedule_whenError_returnsEditView() {
        var req = new com.clinic.dto.Dtos.ScheduleRequest();
        req.setDayOfWeek("MONDAY");
        doThrow(new AppException("bad schedule")).when(doctorService).setSchedule(7L, req);
        when(doctorService.getById(7L)).thenReturn(new com.clinic.model.Doctor());
        when(doctorService.getSchedules(7L)).thenReturn(List.of());
        when(doctorService.getUpcomingLeaves(7L)).thenReturn(List.of());

        String view = controller.setSchedule(7L, req, model);

        assertEquals("admin/doctor-edit", view);
        verify(model).addAttribute("error", "bad schedule");
    }

    @Test
    void toggleDoctor_success_redirectsDoctors() {
        String view = controller.toggleDoctor(3L, flash);
        assertEquals("redirect:/admin/doctors", view);
    }
}