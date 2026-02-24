package com.clinic.service;

import com.clinic.dto.Dtos.*;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.*;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepo;
    private final UserRepository userRepo;
    private final DoctorScheduleRepository scheduleRepo;
    private final DoctorLeaveRepository leaveRepo;
    private final AppointmentRepository appointmentRepo;
    private final PasswordEncoder encoder;
    private final NotificationService notificationService;

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional
    public Doctor createDoctor(DoctorRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AppException("An account with this email already exists.");
        User user = userRepo.save(new User(req.getFullName(), req.getEmail(),
                encoder.encode(req.getPassword()), req.getPhone(), Role.DOCTOR));
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialization(req.getSpecialization());
        doctor.setBio(req.getBio());
        doctor.setConsultationFee(req.getConsultationFee());
        return doctorRepo.save(doctor);
    }

    public Doctor updateDoctor(Long id, DoctorRequest req) {
        Doctor doctor = doctorRepo.findById(id).orElseThrow(() -> new NotFoundException("Doctor", id));
        doctor.setSpecialization(req.getSpecialization());
        doctor.setBio(req.getBio());
        doctor.setConsultationFee(req.getConsultationFee());
        doctor.getUser().setFullName(req.getFullName());
        doctor.getUser().setPhone(req.getPhone());
        userRepo.save(doctor.getUser());
        return doctorRepo.save(doctor);
    }

    public void toggleActive(Long id) {
        Doctor doctor = doctorRepo.findById(id).orElseThrow(() -> new NotFoundException("Doctor", id));
        doctor.setActive(!doctor.isActive());
        doctorRepo.save(doctor);
    }

    // ── Schedule management (doctor self-service or admin) ────────────────────

    public void setSchedule(Long doctorId, ScheduleRequest req) {
        Doctor doctor = doctorRepo.findById(doctorId).orElseThrow(() -> new NotFoundException("Doctor", doctorId));

        DayOfWeek day;
        try { day = DayOfWeek.valueOf(req.getDayOfWeek().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new AppException("Invalid day: " + req.getDayOfWeek()); }

        LocalTime start = LocalTime.parse(req.getStartTime());
        LocalTime end   = LocalTime.parse(req.getEndTime());
        if (!end.isAfter(start))
            throw new AppException("End time must be after start time.");

        List<DoctorSchedule> daySchedules = scheduleRepo.findByDoctorIdAndDayOfWeek(doctorId, day);
        boolean overlaps = daySchedules.stream().anyMatch(existing ->
                start.isBefore(existing.getEndTime()) && end.isAfter(existing.getStartTime()));
        if (overlaps)
            throw new AppException("This time range overlaps with an existing schedule on " + day + ".");

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setSlotDurationMinutes(req.getSlotDurationMinutes());
        scheduleRepo.save(schedule);

        notificationService.sendScheduleUpdated(doctor, day.toString());
    }

    public void deleteSchedule(Long doctorId, Long scheduleId) {
        DoctorSchedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Schedule", scheduleId));
        if (!schedule.getDoctor().getId().equals(doctorId))
            throw new AppException("You can only remove your own schedule entries.");
        scheduleRepo.delete(schedule);
    }

    // ── Leave management (doctor marks specific dates as unavailable) ──────────

    @Transactional
    public DoctorLeave addLeave(Long doctorId, LeaveRequest req) {
        Doctor doctor = doctorRepo.findById(doctorId).orElseThrow(() -> new NotFoundException("Doctor", doctorId));

        if (req.getLeaveDate().isBefore(LocalDate.now()))
            throw new AppException("Cannot mark past dates as leave.");
        if (leaveRepo.existsByDoctorIdAndLeaveDate(doctorId, req.getLeaveDate()))
            throw new AppException("You have already marked " + req.getLeaveDate() + " as leave.");

        // Auto-cancel confirmed appointments on that day and notify patients
        List<Appointment> affected = appointmentRepo
                .findByDoctorIdAndAppointmentDateOrderByAppointmentTime(doctorId, req.getLeaveDate());
        for (Appointment appt : affected) {
            if (appt.getStatus() == AppointmentStatus.CONFIRMED) {
                appt.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepo.save(appt);
                notificationService.sendLeaveAffectedPatient(appt, req.getReason());
            }
        }

        DoctorLeave leave = leaveRepo.save(new DoctorLeave(doctor, req.getLeaveDate(), req.getReason()));
        notificationService.sendLeaveNotification(doctor, req.getLeaveDate(), req.getReason());
        return leave;
    }

    public void removeLeave(Long leaveId, Long doctorId) {
        DoctorLeave leave = leaveRepo.findById(leaveId).orElseThrow(() -> new NotFoundException("Leave", leaveId));
        if (!leave.getDoctor().getId().equals(doctorId))
            throw new AppException("You can only remove your own leave entries.");
        leaveRepo.delete(leave);
    }

    public List<DoctorLeave> getUpcomingLeaves(Long doctorId) {
        return leaveRepo.findByDoctorIdAndLeaveDateGreaterThanEqualOrderByLeaveDate(doctorId, LocalDate.now());
    }

    // ── Doctor self-profile (doctor looks up their own record) ────────────────

    public Doctor getDoctorByUserId(Long userId) {
        return doctorRepo.findByUserId(userId).orElseThrow(() -> new NotFoundException("Doctor profile not found"));
    }

    // ── General queries ───────────────────────────────────────────────────────

    public Doctor getById(Long id) {
        return doctorRepo.findById(id).orElseThrow(() -> new NotFoundException("Doctor", id));
    }

    public List<Doctor> getAllActive() { return doctorRepo.findByActiveTrue(); }
    public List<Doctor> getAll()       { return doctorRepo.findAll(); }
    public List<DoctorSchedule> getSchedules(Long doctorId) {
        return scheduleRepo.findByDoctorId(doctorId).stream()
                .sorted(Comparator.comparing(DoctorSchedule::getDayOfWeek)
                        .thenComparing(DoctorSchedule::getStartTime))
                .toList();
    }
}
