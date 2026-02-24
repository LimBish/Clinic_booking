package com.clinic.dto;

import com.clinic.model.enums.Specialization;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

public class Dtos {

    @Data
    public static class RegisterRequest {
        @NotBlank String fullName;
        @Email @NotBlank String email;
        @NotBlank @Size(min = 6) String password;
        String phone;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank String email;
        @NotBlank String password;
    }

    @Data
    public static class AuthResponse {
        String token;
        String role;
        String name;
        AuthResponse(String token, String role, String name) {
            this.token = token; this.role = role; this.name = name;
        }
        public static AuthResponse of(String t, String r, String n) { return new AuthResponse(t, r, n); }
    }

    @Data
    public static class AppointmentRequest {
        @NotNull Long doctorId;
        @NotNull LocalDate appointmentDate;
        @NotBlank String appointmentTime;
        String reason;
    }

    @Data
    public static class ConsultationRequest {
        @NotBlank(message = "Consultation notes are required")
        String consultationNotes;
    }

    @Data
    public static class DoctorRequest {
        @NotBlank String fullName;
        @Email @NotBlank String email;
        @NotBlank String password;
        String phone;
        Specialization specialization;
        String bio;
        Integer consultationFee;
    }

    @Data
    public static class ScheduleRequest {
        @NotBlank String dayOfWeek;
        @NotBlank String startTime;
        @NotBlank String endTime;
        @NotNull
        @Positive
        Integer slotDurationMinutes = 30;
    }

    @Data
    public static class LeaveRequest {
        @NotNull(message = "Leave date is required")
        LocalDate leaveDate;
        String reason;
    }
}
