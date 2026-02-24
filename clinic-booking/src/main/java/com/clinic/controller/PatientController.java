package com.clinic.controller;

import com.clinic.dto.Dtos.AppointmentRequest;
import com.clinic.exception.AppException;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorSchedule;
import com.clinic.model.enums.Specialization;
import com.clinic.repository.DoctorLeaveRepository;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/patient")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final DoctorLeaveRepository leaveRepo;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("upcoming", appointmentService.getPatientUpcoming(user.getUsername()));
        model.addAttribute("user", userService.findByEmail(user.getUsername()));
        return "patient/dashboard";
    }

    @GetMapping("/book")
    public String bookPage(Model model,
                           @RequestParam(required = false) String specialization,
                           @AuthenticationPrincipal UserDetails user) throws JsonProcessingException {
        List<Doctor> doctors = specialization != null
                ? doctorService.getAll().stream()
                    .filter(d -> d.getSpecialization() != null &&
                                 d.getSpecialization().name().equals(specialization) && d.isActive()).toList()
                : doctorService.getAllActive();
        model.addAttribute("specializations", Specialization.values());
        model.addAttribute("doctors", doctors);
        model.addAttribute("doctorsJson", serializeDoctors(doctors));
        return "patient/book";
    }

    @PostMapping("/book")
    public String book(@Valid @ModelAttribute AppointmentRequest req,
                       @AuthenticationPrincipal UserDetails user,
                       Model model) throws JsonProcessingException {
        try {
            appointmentService.book(user.getUsername(), req);
            return "redirect:/patient/appointments?booked";
        } catch (AppException e) {
            List<Doctor> doctors = doctorService.getAllActive();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("specializations", Specialization.values());
            model.addAttribute("doctors", doctors);
            model.addAttribute("doctorsJson", serializeDoctors(doctors));
            return "patient/book";
        }
    }

    @GetMapping("/appointments")
    public String appointments(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("appointments", appointmentService.getPatientHistory(user.getUsername()));
        return "patient/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails user,
                         RedirectAttributes flash) {
        try {
            appointmentService.cancel(id, user.getUsername());
            flash.addFlashAttribute("success", "Appointment cancelled successfully.");
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("user", userService.findByEmail(user.getUsername()));
        return "patient/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String phone,
                                @AuthenticationPrincipal UserDetails user,
                                RedirectAttributes flash) {
        try {
            userService.updateProfile(user.getUsername(), fullName, phone);
            flash.addFlashAttribute("success", "Profile updated successfully.");
        } catch (AppException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/profile";
    }

    /** REST: available slots — called by React slot picker */
    @GetMapping("/api/slots")
    @ResponseBody
    public ResponseEntity<List<String>> getSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, date));
    }

    /** REST: doctor availability info — working days and leave dates for date picker hints */
    @GetMapping("/api/doctor-availability")
    @ResponseBody
    public ResponseEntity<?> getDoctorAvailability(@RequestParam Long doctorId) {
        List<DoctorSchedule> schedules = doctorService.getSchedules(doctorId);
        List<String> workingDays = schedules.stream()
                .map(s -> s.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase())
                .toList();
        List<String> leaveDates = leaveRepo
                .findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDate(doctorId, LocalDate.now())
                .stream().map(l -> l.getLeaveDate().toString()).toList();

        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode days = objectMapper.createArrayNode();
        workingDays.forEach(days::add);
        ArrayNode leaves = objectMapper.createArrayNode();
        leaveDates.forEach(leaves::add);
        result.set("workingDays", days);
        result.set("leaveDates", leaves);
        return ResponseEntity.ok(result);
    }

    private String serializeDoctors(List<Doctor> doctors) throws JsonProcessingException {
        ArrayNode arr = objectMapper.createArrayNode();
        for (Doctor d : doctors) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", d.getId());
            node.put("consultationFee", d.getConsultationFee() != null ? d.getConsultationFee() : 0);
            node.put("specialization", d.getSpecialization() != null ?
                    d.getSpecialization().name().replace("_", " ") : "");
            node.put("bio", d.getBio() != null ? d.getBio() : "");
            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.put("fullName", d.getUser().getFullName());
            node.set("user", userNode);

            // Working days hint for the date picker
            List<String> workingDays = doctorService.getSchedules(d.getId()).stream()
                    .map(s -> s.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase())
                    .toList();
            ArrayNode wdNode = objectMapper.createArrayNode();
            workingDays.forEach(wdNode::add);
            node.set("workingDays", wdNode);
            arr.add(node);
        }
        return objectMapper.writeValueAsString(arr);
    }
}
