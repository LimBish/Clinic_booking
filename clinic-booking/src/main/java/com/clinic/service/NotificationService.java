package com.clinic.service;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorLeave;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
@Slf4j
public class NotificationService {

    public void sendConfirmation(Appointment appt) {
        log.info("[NOTIFY] BOOKING CONFIRMED → Patient: {} | Dr. {} | {} at {}",
                appt.getPatient().getFullName(),
                appt.getDoctor().getUser().getFullName(),
                appt.getAppointmentDate(),
                appt.getAppointmentTime());
        // TODO: wire JavaMailSender / Twilio here
    }

    public void sendCancellation(Appointment appt) {
        log.info("[NOTIFY] APPOINTMENT CANCELLED → Patient: {} | Dr. {} | {}",
                appt.getPatient().getFullName(),
                appt.getDoctor().getUser().getFullName(),
                appt.getAppointmentDate());
    }

    public void sendConsultationComplete(Appointment appt) {
        log.info("[NOTIFY] CONSULTATION COMPLETED → Patient: {} | Dr. {} | {}",
                appt.getPatient().getFullName(),
                appt.getDoctor().getUser().getFullName(),
                appt.getAppointmentDate());
    }

    // Notifies the clinic (admin) when a doctor marks a day as unavailable
    public void sendLeaveNotification(Doctor doctor, LocalDate date, String reason) {
        log.info("[NOTIFY] DOCTOR LEAVE → Dr. {} is unavailable on {} | Reason: {}",
                doctor.getUser().getFullName(), date, reason != null ? reason : "Not specified");
    }

    // Notifies affected patients when doctor marks a leave on a day they have bookings
    public void sendLeaveAffectedPatient(Appointment appt, String reason) {
        log.info("[NOTIFY] LEAVE AFFECTS PATIENT → Patient: {} | Appointment {} at {} cancelled due to: {}",
                appt.getPatient().getFullName(),
                appt.getAppointmentDate(),
                appt.getAppointmentTime(),
                reason != null ? reason : "Doctor unavailable");
    }

    // Notifies clinic when doctor updates their weekly schedule
    public void sendScheduleUpdated(Doctor doctor, String dayOfWeek) {
        log.info("[NOTIFY] SCHEDULE UPDATED → Dr. {} updated availability for {}",
                doctor.getUser().getFullName(), dayOfWeek);
    }
}
