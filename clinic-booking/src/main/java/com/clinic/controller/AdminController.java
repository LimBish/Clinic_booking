package com.clinic.controller;

import com.clinic.dto.Dtos.*;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.User;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Specialization;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DoctorLeaveRepository;
import com.clinic.service.AdminPatientService;
import com.clinic.service.DoctorService;
import com.clinic.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final DoctorService doctorService;
//    private final UserService userService;
    private final AppointmentRepository appointmentRepo;
    private final DoctorLeaveRepository leaveRepo;
    private final AdminPatientService adminPatientService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AdminDashboardPatientStats patientStats = adminPatientService.getDashboardPatientStats();
        model.addAttribute("totalDoctors", doctorService.getAll().size());
        model.addAttribute("totalPatients", patientStats.getTotalPatients());
        model.addAttribute("newPatientsThisMonth", patientStats.getNewPatientsThisMonth());
        model.addAttribute("activePatients", patientStats.getActivePatients());
        model.addAttribute("frequentPatients", patientStats.getFrequentPatients());
        model.addAttribute("noShowPatients", patientStats.getNoShowPatients());
        model.addAttribute("mostFrequentPatients", adminPatientService.getMostFrequentPatients(5));
        model.addAttribute("noShowPatientList", adminPatientService.getNoShowPatients());
        model.addAttribute("confirmedAppointments", appointmentRepo.countByStatus(AppointmentStatus.CONFIRMED));
        model.addAttribute("completedAppointments", appointmentRepo.countByStatus(AppointmentStatus.COMPLETED));
        model.addAttribute("recentAppointments", appointmentRepo.findAll().stream()
                .sorted((a, b) -> b.getAppointmentDate().compareTo(a.getAppointmentDate()))
                .limit(10).toList());
        model.addAttribute("doctorLeaves",
                leaveRepo.findAll().stream()
                        .filter(l -> !l.getLeaveDate().isBefore(LocalDate.now()))
                        .sorted((a, b) -> a.getLeaveDate().compareTo(b.getLeaveDate()))
                        .limit(8).toList());
        return "admin/dashboard";
    }

    @GetMapping("/patients")
    public String patients(@RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String registration,
                           @RequestParam(required = false) String appointments,
                           Model model) {
        model.addAttribute("patients", adminPatientService.getPatients(q, status, registration, appointments));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("registration", registration);
        model.addAttribute("appointments", appointments);
        return "admin/patients";
    }

    @GetMapping("/patients/{id}")
    public String patientProfile(@PathVariable Long id, Model model) {
        User patient = adminPatientService.getPatient(id);
        model.addAttribute("patient", patient);
        model.addAttribute("appointments", adminPatientService.getPatientAppointments(id));
        return "admin/patient-profile";
    }

    @PostMapping("/patients/{id}/toggle")
    public String togglePatient(@PathVariable Long id,
                                @RequestParam(defaultValue = "patients") String back,
                                RedirectAttributes flash) {
        try {
            adminPatientService.togglePatient(id);
            flash.addFlashAttribute("success", "Patient status updated.");
        } catch (NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        if ("profile".equalsIgnoreCase(back)) {
            return "redirect:/admin/patients/" + id;
        }
        return "redirect:/admin/patients";
    }


    @GetMapping("/doctors")
    public String doctors(Model model) {
        model.addAttribute("doctors", doctorService.getAll());
        return "admin/doctors";
    }

    @GetMapping("/doctors/new")
    public String newDoctorForm(Model model) {
        model.addAttribute("doctorRequest", new DoctorRequest());
        model.addAttribute("specializations", Specialization.values());
        return "admin/doctor-form";
    }

    @PostMapping("/doctors")
    public String createDoctor(@Valid @ModelAttribute DoctorRequest req, Model model) {
        try {
            doctorService.createDoctor(req);
            return "redirect:/admin/doctors?created";
        } catch (AppException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("specializations", Specialization.values());
            return "admin/doctor-form";
        }
    }

    @GetMapping("/doctors/{id}/edit")
    public String editDoctorForm(@PathVariable Long id, Model model) {
        model.addAttribute("doctor", doctorService.getById(id));
        model.addAttribute("specializations", Specialization.values());
        model.addAttribute("schedules", doctorService.getSchedules(id));
        model.addAttribute("leaves", doctorService.getUpcomingLeaves(id));
        return "admin/doctor-edit";
    }

    @PostMapping("/doctors/{id}")
    public String updateDoctor(@PathVariable Long id, @ModelAttribute DoctorRequest req,
                               Model model, RedirectAttributes flash) {
        try {
            doctorService.updateDoctor(id, req);
            flash.addFlashAttribute("success", "Doctor updated successfully.");
            return "redirect:/admin/doctors/" + id + "/edit";
        } catch (AppException | NotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("doctor", doctorService.getById(id));
            model.addAttribute("specializations", Specialization.values());
            model.addAttribute("schedules", doctorService.getSchedules(id));
            model.addAttribute("leaves", doctorService.getUpcomingLeaves(id));
            return "admin/doctor-edit";
        }
    }

    @PostMapping("/doctors/{id}/schedule")
    public String setSchedule(@PathVariable Long id, @Valid @ModelAttribute ScheduleRequest req,
                              Model model) {
        try {
            doctorService.setSchedule(id, req);
            return "redirect:/admin/doctors/" + id + "/edit?scheduled";
        } catch (AppException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("doctor", doctorService.getById(id));
            model.addAttribute("specializations", Specialization.values());
            model.addAttribute("schedules", doctorService.getSchedules(id));
            model.addAttribute("leaves", doctorService.getUpcomingLeaves(id));
            return "admin/doctor-edit";
        }
    }

    @PostMapping("/doctors/{id}/toggle")
    public String toggleDoctor(@PathVariable Long id, RedirectAttributes flash) {
        try {
            doctorService.toggleActive(id);
            flash.addFlashAttribute("success", "Doctor status updated.");
        } catch (NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/doctors";
    }
}
