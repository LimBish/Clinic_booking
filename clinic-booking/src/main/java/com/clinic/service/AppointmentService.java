package com.clinic.service;

import com.clinic.dto.Dtos.AppointmentRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final DoctorScheduleRepository scheduleRepo;
    private final DoctorLeaveRepository leaveRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    public Appointment book(String patientEmail, AppointmentRequest req) {
        User patient = userRepo.findByEmail(patientEmail)
                .orElseThrow(() -> new NotFoundException("User", patientEmail));
        Doctor doctor = doctorRepo.findById(req.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Doctor", req.getDoctorId()));

        if (!doctor.isActive())
            throw new AppException("This doctor is currently not accepting appointments.");

        if (req.getAppointmentDate().isBefore(LocalDate.now()))
            throw new AppException("Appointment date cannot be in the past.");

        // Check doctor has a weekly schedule for that day
//        DoctorSchedule schedule = scheduleRepo
//                .findByDoctorIdAndDayOfWeek(doctor.getId(), req.getAppointmentDate().getDayOfWeek())
//                .orElseThrow(() -> new AppException(
//                    "Dr. " + doctor.getUser().getFullName() + " is not available on "
        List<DoctorSchedule> schedules = scheduleRepo
                .findByDoctorIdAndDayOfWeek(doctor.getId(), req.getAppointmentDate().getDayOfWeek());
        if (schedules.isEmpty())
            throw new AppException(
                    + req.getAppointmentDate().getDayOfWeek().toString().charAt(0)
                    + req.getAppointmentDate().getDayOfWeek().toString().substring(1).toLowerCase() + "s.");

        // Check doctor has not marked that specific date as leave
        if (leaveRepo.existsByDoctorIdAndLeaveDate(doctor.getId(), req.getAppointmentDate()))
            throw new AppException("Dr. " + doctor.getUser().getFullName()
                    + " is on leave on " + req.getAppointmentDate() + ". Please choose another date.");

        LocalTime time = LocalTime.parse(req.getAppointmentTime());

//        // Validate time is within schedule and aligns with slot duration
//        LocalTime slotEnd = time.plusMinutes(schedule.getSlotDurationMinutes());
//        if (time.isBefore(schedule.getStartTime()) || !slotEnd.isAfter(time) || slotEnd.isAfter(schedule.getEndTime()))
//            throw new AppException("Selected time is outside of the doctor's working hours.");

        // Validate time falls in at least one schedule window and aligns with that slot duration
        DoctorSchedule matchingSchedule = schedules.stream()
                .filter(schedule -> {
                    LocalTime slotEnd = time.plusMinutes(schedule.getSlotDurationMinutes());
                    if (time.isBefore(schedule.getStartTime()) || !slotEnd.isAfter(time) || slotEnd.isAfter(schedule.getEndTime()))
                        return false;
                    long minutesFromStart = java.time.Duration.between(schedule.getStartTime(), time).toMinutes();
                    return minutesFromStart % schedule.getSlotDurationMinutes() == 0;
                })
                .findFirst()
                .orElse(null);

//        long minutesFromStart = java.time.Duration.between(schedule.getStartTime(), time).toMinutes();
//        if (minutesFromStart % schedule.getSlotDurationMinutes() != 0)
//            throw new AppException("Selected time does not align with the doctor's slot schedule.");

        if (matchingSchedule == null)
            throw new AppException("Selected time is outside of the doctor's working hours or does not match slot intervals.");


        if (isSlotTaken(doctor.getId(), req.getAppointmentDate(), time))
            throw new AppException("This time slot is already booked. Please choose another.");

        Appointment appt = new Appointment();
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setAppointmentDate(req.getAppointmentDate());
        appt.setAppointmentTime(time);
        appt.setReason(req.getReason());
        appt.setStatus(AppointmentStatus.CONFIRMED);

        Appointment saved = appointmentRepo.save(appt);
        notificationService.sendConfirmation(saved);
        return saved;
    }

    /**
     * Returns available time slots for a doctor on a given date.
     * Respects: weekly schedule, leaves, already-booked slots, past times.
     */
    public List<String> getAvailableSlots(Long doctorId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) return List.of();

        // Doctor on leave that day?
        if (leaveRepo.existsByDoctorIdAndLeaveDate(doctorId, date)) return List.of();

        List<DoctorSchedule> schedules = scheduleRepo
                .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek());
        if (schedules.isEmpty()) return List.of();

        List<String> slots = new ArrayList<>();
        LocalTime now = LocalTime.now();

        for (DoctorSchedule schedule : schedules) {
            LocalTime current = schedule.getStartTime();
            while (current.isBefore(schedule.getEndTime())) {
                // Skip past slots for today
                boolean isPast = date.equals(LocalDate.now()) && current.isBefore(now);
                if (!isPast && !isSlotTaken(doctorId, date, current))
                    slots.add(current.toString());
                current = current.plusMinutes(schedule.getSlotDurationMinutes());
            }
        }
        return slots;
    }

    /** Doctor marks appointment as consulted and adds notes */
    public Appointment complete(Long appointmentId, String doctorEmail, ConsultationRequest req) {
        Appointment appt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment", appointmentId));

        // Verify it belongs to this doctor
        if (!appt.getDoctor().getUser().getEmail().equals(doctorEmail))
            throw new AppException("You can only update your own appointments.");

        if (appt.getStatus() == AppointmentStatus.CANCELLED)
            throw new AppException("Cannot mark a cancelled appointment as consulted.");
        if (appt.getStatus() == AppointmentStatus.COMPLETED)
            throw new AppException("This appointment is already marked as completed.");

        appt.setStatus(AppointmentStatus.COMPLETED);
        appt.setConsultationNotes(req.getConsultationNotes());
        appt.setConsultedAt(LocalDateTime.now());

        Appointment saved = appointmentRepo.save(appt);
        notificationService.sendConsultationComplete(saved);
        return saved;
    }

    public Appointment cancel(Long appointmentId, String patientEmail) {
        Appointment appt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment", appointmentId));
        validatePatientOwnership(appt, patientEmail);

        if (appt.getStatus() == AppointmentStatus.CANCELLED)
            throw new AppException("Appointment is already cancelled.");
        if (appt.getStatus() == AppointmentStatus.COMPLETED)
            throw new AppException("Completed appointments cannot be cancelled.");

        appt.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepo.save(appt);
        notificationService.sendCancellation(saved);
        return saved;
    }

    public List<Appointment> getPatientHistory(String email) {
        User patient = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("User", email));
        return appointmentRepo.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId());
    }

    public List<Appointment> getPatientUpcoming(String email) {
        User patient = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("User", email));
        return appointmentRepo.findByPatientIdAndStatusIn(patient.getId(),
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING));
    }

    /** Doctor: today's appointments */
    public List<Appointment> getDoctorTodayAppointments(String email) {
        Doctor doctor = getDoctorByEmail(email);
        return appointmentRepo.findByDoctorIdAndAppointmentDateOrderByAppointmentTime(
                doctor.getId(), LocalDate.now());
    }

    /** Doctor: appointments for a specific date */
    public List<Appointment> getDoctorAppointmentsForDate(String email, LocalDate date) {
        Doctor doctor = getDoctorByEmail(email);
        return appointmentRepo.findByDoctorIdAndAppointmentDateOrderByAppointmentTime(doctor.getId(), date);
    }

    /** Doctor: appointments for the current week (Mon–Sun) */
    public List<Appointment> getDoctorWeekAppointments(String email, LocalDate weekStart) {
        Doctor doctor = getDoctorByEmail(email);
        return appointmentRepo.findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
                doctor.getId(), weekStart, weekStart.plusDays(6));
    }

    private Doctor getDoctorByEmail(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("User", email));
        return doctorRepo.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));
    }

    private boolean isSlotTaken(Long doctorId, LocalDate date, LocalTime time) {
        return appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                doctorId, date, time, AppointmentStatus.CANCELLED);
    }

    private void validatePatientOwnership(Appointment appt, String email) {
        if (!appt.getPatient().getEmail().equals(email))
            throw new AppException("You do not have permission to modify this appointment.");
    }
}
