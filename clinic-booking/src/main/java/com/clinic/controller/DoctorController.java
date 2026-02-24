package com.clinic.controller;

import com.clinic.dto.Dtos.*;
import com.clinic.exception.AppException;
import com.clinic.model.Doctor;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Controller
@RequestMapping("/doctor")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final UserService userService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails user) {
        Doctor doctor = doctorService.getDoctorByUserId(userService.findByEmail(user.getUsername()).getId());
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        model.addAttribute("user", userService.findByEmail(user.getUsername()));
        model.addAttribute("doctor", doctor);
        model.addAttribute("todayAppointments", appointmentService.getDoctorTodayAppointments(user.getUsername()));
        model.addAttribute("weekAppointments", appointmentService.getDoctorWeekAppointments(user.getUsername(), weekStart));
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("today", LocalDate.now());
        return "doctor/dashboard";
    }

    // ── Consultation: mark as completed with notes ────────────────────────────

    @PostMapping("/appointments/{id}/complete")
    public String complete(@PathVariable Long id,
                           @Valid @ModelAttribute ConsultationRequest req,
                           @AuthenticationPrincipal UserDetails user,
                           RedirectAttributes flash) {
        try {
            appointmentService.complete(id, user.getUsername(), req);
            flash.addFlashAttribute("success", "Appointment marked as consulted.");
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }

    // ── Schedule management ───────────────────────────────────────────────────

    @GetMapping("/schedule")
    public String schedulePage(Model model, @AuthenticationPrincipal UserDetails user) {
        Doctor doctor = doctorService.getDoctorByUserId(userService.findByEmail(user.getUsername()).getId());
        model.addAttribute("doctor", doctor);
        model.addAttribute("schedules", doctorService.getSchedules(doctor.getId()));
        model.addAttribute("leaves", doctorService.getUpcomingLeaves(doctor.getId()));
        model.addAttribute("scheduleRequest", new ScheduleRequest());
        model.addAttribute("leaveRequest", new LeaveRequest());
        model.addAttribute("days", DayOfWeek.values());
        return "doctor/schedule";
    }

    @PostMapping("/schedule")
    public String saveSchedule(@Valid @ModelAttribute ScheduleRequest req,
                               @AuthenticationPrincipal UserDetails user,
                               RedirectAttributes flash) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userService.findByEmail(user.getUsername()).getId());
            doctorService.setSchedule(doctor.getId(), req);
            flash.addFlashAttribute("success", "Schedule updated for " + req.getDayOfWeek() + ".");
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/schedule";
    }

    @PostMapping("/schedule/delete/{day}")
    public String deleteSchedule(@PathVariable String day,
                                 @AuthenticationPrincipal UserDetails user,
                                 RedirectAttributes flash) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userService.findByEmail(user.getUsername()).getId());
            doctorService.deleteSchedule(doctor.getId(), DayOfWeek.valueOf(day.toUpperCase()));
            flash.addFlashAttribute("success", day + " removed from your schedule.");
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/schedule";
    }

    // ── Leave management ──────────────────────────────────────────────────────

    @PostMapping("/leave")
    public String addLeave(@Valid @ModelAttribute LeaveRequest req,
                           @AuthenticationPrincipal UserDetails user,
                           RedirectAttributes flash) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userService.findByEmail(user.getUsername()).getId());
//            int affectedCount = appointmentService
//                    .getDoctorWeekAppointments(user.getUsername(), req.getLeaveDate()).size();

            int affectedCount = (int) appointmentService
                    .getDoctorAppointmentsForDate(user.getUsername(), req.getLeaveDate()).stream()
                    .filter(appt -> appt.getStatus() == AppointmentStatus.CONFIRMED)
                    .count();
            doctorService.addLeave(doctor.getId(), req);
            flash.addFlashAttribute("success",
                    "Leave marked for " + req.getLeaveDate() + ". " +
                    (affectedCount > 0 ? affectedCount + " patient(s) have been notified." : ""));
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/schedule";
    }

    @PostMapping("/leave/{id}/remove")
    public String removeLeave(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails user,
                              RedirectAttributes flash) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userService.findByEmail(user.getUsername()).getId());
            doctorService.removeLeave(id, doctor.getId());
            flash.addFlashAttribute("success", "Leave entry removed. You are now available on that date.");
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/schedule";
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("user", userService.findByEmail(user.getUsername()));
        model.addAttribute("doctor", doctorService.getDoctorByUserId(
                userService.findByEmail(user.getUsername()).getId()));
        return "doctor/profile";
    }
}
