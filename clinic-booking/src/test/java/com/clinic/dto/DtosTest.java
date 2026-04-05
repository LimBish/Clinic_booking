package com.clinic.dto;

import com.clinic.model.enums.Specialization;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    // ── RegisterRequest ───────────────────────────────────────────────────────

    @Test
    void registerRequest_gettersAndSetters() {
        Dtos.RegisterRequest req = new Dtos.RegisterRequest();
        req.setFullName("John Doe");
        req.setEmail("john@clinic.com");
        req.setPassword("secret123");
        req.setPhone("9876543210");

        assertEquals("John Doe", req.getFullName());
        assertEquals("john@clinic.com", req.getEmail());
        assertEquals("secret123", req.getPassword());
        assertEquals("9876543210", req.getPhone());
    }

    @Test
    void registerRequest_equalObjects() {
        Dtos.RegisterRequest r1 = new Dtos.RegisterRequest();
        r1.setFullName("Alice"); r1.setEmail("a@b.com");
        r1.setPassword("pw123"); r1.setPhone("111");

        Dtos.RegisterRequest r2 = new Dtos.RegisterRequest();
        r2.setFullName("Alice"); r2.setEmail("a@b.com");
        r2.setPassword("pw123"); r2.setPhone("111");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void registerRequest_sameInstance_isEqual() {
        Dtos.RegisterRequest r = new Dtos.RegisterRequest();
        r.setFullName("Alice");
        assertEquals(r, r);
    }

    @Test
    void registerRequest_notEqualToNull() {
        Dtos.RegisterRequest r = new Dtos.RegisterRequest();
        assertNotEquals(null, r);
    }

    @Test
    void registerRequest_notEqualToDifferentType() {
        Dtos.RegisterRequest r = new Dtos.RegisterRequest();
        assertNotEquals("string", r);
    }

    @Test
    void registerRequest_notEqualWhenFieldDiffers() {
        Dtos.RegisterRequest r1 = new Dtos.RegisterRequest();
        r1.setFullName("Alice");

        Dtos.RegisterRequest r2 = new Dtos.RegisterRequest();
        r2.setFullName("Bob");

        assertNotEquals(r1, r2);
    }

    @Test
    void registerRequest_nullFields_equalIfBothNull() {
        Dtos.RegisterRequest r1 = new Dtos.RegisterRequest();
        Dtos.RegisterRequest r2 = new Dtos.RegisterRequest();
        assertEquals(r1, r2);
    }

    @Test
    void registerRequest_toString_containsName() {
        Dtos.RegisterRequest r = new Dtos.RegisterRequest();
        r.setFullName("Bob");
        assertTrue(r.toString().contains("Bob"));
    }

    // ── LoginRequest ──────────────────────────────────────────────────────────

    @Test
    void loginRequest_gettersAndSetters() {
        Dtos.LoginRequest req = new Dtos.LoginRequest();
        req.setEmail("login@clinic.com");
        req.setPassword("pass123");

        assertEquals("login@clinic.com", req.getEmail());
        assertEquals("pass123", req.getPassword());
    }

    @Test
    void loginRequest_equalObjects() {
        Dtos.LoginRequest a = new Dtos.LoginRequest();
        a.setEmail("a@b.com"); a.setPassword("pw");

        Dtos.LoginRequest b = new Dtos.LoginRequest();
        b.setEmail("a@b.com"); b.setPassword("pw");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void loginRequest_sameInstance_isEqual() {
        Dtos.LoginRequest r = new Dtos.LoginRequest();
        assertEquals(r, r);
    }

    @Test
    void loginRequest_notEqualToNull() {
        assertNotEquals(null, new Dtos.LoginRequest());
    }

    @Test
    void loginRequest_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.LoginRequest());
    }

    @Test
    void loginRequest_notEqualWhenEmailDiffers() {
        Dtos.LoginRequest a = new Dtos.LoginRequest();
        a.setEmail("a@b.com");

        Dtos.LoginRequest b = new Dtos.LoginRequest();
        b.setEmail("c@d.com");

        assertNotEquals(a, b);
    }

    // ── AuthResponse ──────────────────────────────────────────────────────────

    @Test
    void authResponse_of_setsAllFields() {
        Dtos.AuthResponse res = Dtos.AuthResponse.of("tok123", "ROLE_PATIENT", "Alice");
        assertEquals("tok123", res.getToken());
        assertEquals("ROLE_PATIENT", res.getRole());
        assertEquals("Alice", res.getName());
    }

    @Test
    void authResponse_setters_overrideValues() {
        Dtos.AuthResponse res = Dtos.AuthResponse.of("t", "r", "n");
        res.setToken("newToken");
        res.setRole("ROLE_DOCTOR");
        res.setName("Dr. Smith");

        assertEquals("newToken", res.getToken());
        assertEquals("ROLE_DOCTOR", res.getRole());
        assertEquals("Dr. Smith", res.getName());
    }

    @Test
    void authResponse_equalObjects() {
        Dtos.AuthResponse a = Dtos.AuthResponse.of("t", "r", "n");
        Dtos.AuthResponse b = Dtos.AuthResponse.of("t", "r", "n");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void authResponse_sameInstance_isEqual() {
        Dtos.AuthResponse r = Dtos.AuthResponse.of("t", "r", "n");
        assertEquals(r, r);
    }

    @Test
    void authResponse_notEqualToNull() {
        assertNotEquals(null, Dtos.AuthResponse.of("t", "r", "n"));
    }

    @Test
    void authResponse_notEqualToDifferentType() {
        assertNotEquals("x", Dtos.AuthResponse.of("t", "r", "n"));
    }

    @Test
    void authResponse_notEqualWhenTokenDiffers() {
        Dtos.AuthResponse a = Dtos.AuthResponse.of("t1", "r", "n");
        Dtos.AuthResponse b = Dtos.AuthResponse.of("t2", "r", "n");
        assertNotEquals(a, b);
    }

    @Test
    void authResponse_notEqualWhenRoleDiffers() {
        Dtos.AuthResponse a = Dtos.AuthResponse.of("t", "PATIENT", "n");
        Dtos.AuthResponse b = Dtos.AuthResponse.of("t", "DOCTOR", "n");
        assertNotEquals(a, b);
    }

    @Test
    void authResponse_notEqualWhenNameDiffers() {
        Dtos.AuthResponse a = Dtos.AuthResponse.of("t", "r", "Alice");
        Dtos.AuthResponse b = Dtos.AuthResponse.of("t", "r", "Bob");
        assertNotEquals(a, b);
    }

    // ── AppointmentRequest ────────────────────────────────────────────────────

    @Test
    void appointmentRequest_gettersAndSetters() {
        Dtos.AppointmentRequest req = new Dtos.AppointmentRequest();
        LocalDate date = LocalDate.of(2025, 6, 15);
        req.setDoctorId(3L);
        req.setAppointmentDate(date);
        req.setAppointmentTime("10:00");
        req.setReason("Checkup");

        assertEquals(3L, req.getDoctorId());
        assertEquals(date, req.getAppointmentDate());
        assertEquals("10:00", req.getAppointmentTime());
        assertEquals("Checkup", req.getReason());
    }

    @Test
    void appointmentRequest_equalObjects() {
        Dtos.AppointmentRequest a = new Dtos.AppointmentRequest();
        a.setDoctorId(1L); a.setAppointmentTime("09:00");

        Dtos.AppointmentRequest b = new Dtos.AppointmentRequest();
        b.setDoctorId(1L); b.setAppointmentTime("09:00");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void appointmentRequest_sameInstance_isEqual() {
        Dtos.AppointmentRequest r = new Dtos.AppointmentRequest();
        assertEquals(r, r);
    }

    @Test
    void appointmentRequest_notEqualToNull() {
        assertNotEquals(null, new Dtos.AppointmentRequest());
    }

    @Test
    void appointmentRequest_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.AppointmentRequest());
    }

    @Test
    void appointmentRequest_notEqualWhenDoctorIdDiffers() {
        Dtos.AppointmentRequest a = new Dtos.AppointmentRequest();
        a.setDoctorId(1L);

        Dtos.AppointmentRequest b = new Dtos.AppointmentRequest();
        b.setDoctorId(2L);

        assertNotEquals(a, b);
    }

    @Test
    void appointmentRequest_notEqualWhenTimeDiffers() {
        Dtos.AppointmentRequest a = new Dtos.AppointmentRequest();
        a.setAppointmentTime("09:00");

        Dtos.AppointmentRequest b = new Dtos.AppointmentRequest();
        b.setAppointmentTime("10:00");

        assertNotEquals(a, b);
    }

    // ── ConsultationRequest ───────────────────────────────────────────────────

    @Test
    void consultationRequest_gettersAndSetters() {
        Dtos.ConsultationRequest req = new Dtos.ConsultationRequest();
        req.setConsultationNotes("Patient is recovering well.");
        assertEquals("Patient is recovering well.", req.getConsultationNotes());
    }

    @Test
    void consultationRequest_equalObjects() {
        Dtos.ConsultationRequest a = new Dtos.ConsultationRequest();
        a.setConsultationNotes("notes");

        Dtos.ConsultationRequest b = new Dtos.ConsultationRequest();
        b.setConsultationNotes("notes");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void consultationRequest_sameInstance_isEqual() {
        Dtos.ConsultationRequest r = new Dtos.ConsultationRequest();
        assertEquals(r, r);
    }

    @Test
    void consultationRequest_notEqualToNull() {
        assertNotEquals(null, new Dtos.ConsultationRequest());
    }

    @Test
    void consultationRequest_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.ConsultationRequest());
    }

    @Test
    void consultationRequest_notEqualWhenNotesDiffer() {
        Dtos.ConsultationRequest a = new Dtos.ConsultationRequest();
        a.setConsultationNotes("note A");

        Dtos.ConsultationRequest b = new Dtos.ConsultationRequest();
        b.setConsultationNotes("note B");

        assertNotEquals(a, b);
    }

    // ── DoctorRequest ─────────────────────────────────────────────────────────

    @Test
    void doctorRequest_gettersAndSetters() {
        Dtos.DoctorRequest req = new Dtos.DoctorRequest();
        req.setFullName("Dr. Jane");
        req.setEmail("jane@clinic.com");
        req.setPassword("docpass");
        req.setPhone("1234567890");
        req.setSpecialization(Specialization.CARDIOLOGY);
        req.setBio("Experienced cardiologist.");
        req.setConsultationFee(800);

        assertEquals("Dr. Jane", req.getFullName());
        assertEquals("jane@clinic.com", req.getEmail());
        assertEquals("docpass", req.getPassword());
        assertEquals("1234567890", req.getPhone());
        assertEquals(Specialization.CARDIOLOGY, req.getSpecialization());
        assertEquals("Experienced cardiologist.", req.getBio());
        assertEquals(800, req.getConsultationFee());
    }

    @Test
    void doctorRequest_equalObjects() {
        Dtos.DoctorRequest a = new Dtos.DoctorRequest();
        a.setEmail("dr@clinic.com"); a.setSpecialization(Specialization.CARDIOLOGY);

        Dtos.DoctorRequest b = new Dtos.DoctorRequest();
        b.setEmail("dr@clinic.com"); b.setSpecialization(Specialization.CARDIOLOGY);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void doctorRequest_sameInstance_isEqual() {
        Dtos.DoctorRequest r = new Dtos.DoctorRequest();
        assertEquals(r, r);
    }

    @Test
    void doctorRequest_notEqualToNull() {
        assertNotEquals(null, new Dtos.DoctorRequest());
    }

    @Test
    void doctorRequest_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.DoctorRequest());
    }

    @Test
    void doctorRequest_notEqualWhenSpecializationDiffers() {
        Dtos.DoctorRequest a = new Dtos.DoctorRequest();
        a.setSpecialization(Specialization.CARDIOLOGY);

        Dtos.DoctorRequest b = new Dtos.DoctorRequest();
        b.setSpecialization(Specialization.NEUROLOGY);

        assertNotEquals(a, b);
    }

    @Test
    void doctorRequest_nullSpecialization_isAllowed() {
        Dtos.DoctorRequest req = new Dtos.DoctorRequest();
        req.setSpecialization(null);
        assertNull(req.getSpecialization());
    }

    // ── ScheduleRequest ───────────────────────────────────────────────────────

    @Test
    void scheduleRequest_gettersAndSetters() {
        Dtos.ScheduleRequest req = new Dtos.ScheduleRequest();
        req.setDayOfWeek("MONDAY");
        req.setStartTime("09:00");
        req.setEndTime("17:00");
        req.setSlotDurationMinutes(30);

        assertEquals("MONDAY", req.getDayOfWeek());
        assertEquals("09:00", req.getStartTime());
        assertEquals("17:00", req.getEndTime());
        assertEquals(30, req.getSlotDurationMinutes());
    }

    @Test
    void scheduleRequest_defaultSlotDuration_is30() {
        assertEquals(30, new Dtos.ScheduleRequest().getSlotDurationMinutes());
    }

    @Test
    void scheduleRequest_equalObjects() {
        Dtos.ScheduleRequest a = new Dtos.ScheduleRequest();
        a.setDayOfWeek("MONDAY"); a.setStartTime("09:00"); a.setEndTime("17:00");

        Dtos.ScheduleRequest b = new Dtos.ScheduleRequest();
        b.setDayOfWeek("MONDAY"); b.setStartTime("09:00"); b.setEndTime("17:00");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void scheduleRequest_sameInstance_isEqual() {
        Dtos.ScheduleRequest r = new Dtos.ScheduleRequest();
        assertEquals(r, r);
    }

    @Test
    void scheduleRequest_notEqualToNull() {
        assertNotEquals(null, new Dtos.ScheduleRequest());
    }

    @Test
    void scheduleRequest_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.ScheduleRequest());
    }

    @Test
    void scheduleRequest_notEqualWhenDayDiffers() {
        Dtos.ScheduleRequest a = new Dtos.ScheduleRequest();
        a.setDayOfWeek("MONDAY");

        Dtos.ScheduleRequest b = new Dtos.ScheduleRequest();
        b.setDayOfWeek("FRIDAY");

        assertNotEquals(a, b);
    }

    // ── AdminPatientRow ───────────────────────────────────────────────────────

    @Test
    void adminPatientRow_of_setsAllFields() {
        LocalDate date = LocalDate.of(2024, 1, 10);
        Dtos.AdminPatientRow row = Dtos.AdminPatientRow.of(
                1L, "Alice", "alice@clinic.com", date, true, 5L, "Dr. Smith");

        assertEquals(1L, row.getId());
        assertEquals("Alice", row.getFullName());
        assertEquals("alice@clinic.com", row.getEmail());
        assertEquals(date, row.getRegistrationDate());
        assertTrue(row.isEnabled());
        assertEquals(5L, row.getAppointmentCount());
        assertEquals("Dr. Smith", row.getBookedDoctors());
    }

    @Test
    void adminPatientRow_disabledPatient() {
        Dtos.AdminPatientRow row = Dtos.AdminPatientRow.of(
                2L, "Bob", "bob@clinic.com", LocalDate.now(), false, 0L, "");
        assertFalse(row.isEnabled());
        assertEquals(0L, row.getAppointmentCount());
    }

    @Test
    void adminPatientRow_setters_work() {
        Dtos.AdminPatientRow row = new Dtos.AdminPatientRow();
        row.setId(99L);
        row.setFullName("Charlie");
        row.setEnabled(true);

        assertEquals(99L, row.getId());
        assertEquals("Charlie", row.getFullName());
        assertTrue(row.isEnabled());
    }

    @Test
    void adminPatientRow_equalObjects() {
        LocalDate date = LocalDate.of(2024, 3, 1);
        Dtos.AdminPatientRow a = Dtos.AdminPatientRow.of(1L, "A", "a@b.com", date, true, 2L, "Dr X");
        Dtos.AdminPatientRow b = Dtos.AdminPatientRow.of(1L, "A", "a@b.com", date, true, 2L, "Dr X");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void adminPatientRow_sameInstance_isEqual() {
        Dtos.AdminPatientRow r = new Dtos.AdminPatientRow();
        assertEquals(r, r);
    }

    @Test
    void adminPatientRow_notEqualToNull() {
        assertNotEquals(null, new Dtos.AdminPatientRow());
    }

    @Test
    void adminPatientRow_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.AdminPatientRow());
    }

    @Test
    void adminPatientRow_notEqualWhenNameDiffers() {
        Dtos.AdminPatientRow a = new Dtos.AdminPatientRow();
        a.setFullName("Alice");

        Dtos.AdminPatientRow b = new Dtos.AdminPatientRow();
        b.setFullName("Bob");

        assertNotEquals(a, b);
    }

    // ── AdminDashboardPatientStats ────────────────────────────────────────────

    @Test
    void adminDashboardPatientStats_gettersAndSetters() {
        Dtos.AdminDashboardPatientStats stats = new Dtos.AdminDashboardPatientStats();
        stats.setTotalPatients(100);
        stats.setNewPatientsThisMonth(10);
        stats.setActivePatients(80);
        stats.setFrequentPatients(5);
        stats.setNoShowPatients(3);

        assertEquals(100, stats.getTotalPatients());
        assertEquals(10, stats.getNewPatientsThisMonth());
        assertEquals(80, stats.getActivePatients());
        assertEquals(5, stats.getFrequentPatients());
        assertEquals(3, stats.getNoShowPatients());
    }

    @Test
    void adminDashboardPatientStats_defaultValues_areZero() {
        Dtos.AdminDashboardPatientStats stats = new Dtos.AdminDashboardPatientStats();
        assertEquals(0, stats.getTotalPatients());
        assertEquals(0, stats.getActivePatients());
        assertEquals(0, stats.getFrequentPatients());
        assertEquals(0, stats.getNoShowPatients());
    }

    @Test
    void adminDashboardPatientStats_equalObjects() {
        Dtos.AdminDashboardPatientStats a = new Dtos.AdminDashboardPatientStats();
        a.setTotalPatients(10); a.setActivePatients(8);

        Dtos.AdminDashboardPatientStats b = new Dtos.AdminDashboardPatientStats();
        b.setTotalPatients(10); b.setActivePatients(8);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void adminDashboardPatientStats_sameInstance_isEqual() {
        Dtos.AdminDashboardPatientStats s = new Dtos.AdminDashboardPatientStats();
        assertEquals(s, s);
    }

    @Test
    void adminDashboardPatientStats_notEqualToNull() {
        assertNotEquals(null, new Dtos.AdminDashboardPatientStats());
    }

    @Test
    void adminDashboardPatientStats_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.AdminDashboardPatientStats());
    }

    @Test
    void adminDashboardPatientStats_notEqualWhenTotalDiffers() {
        Dtos.AdminDashboardPatientStats a = new Dtos.AdminDashboardPatientStats();
        a.setTotalPatients(5);

        Dtos.AdminDashboardPatientStats b = new Dtos.AdminDashboardPatientStats();
        b.setTotalPatients(10);

        assertNotEquals(a, b);
    }

    // ── LeaveRequest ──────────────────────────────────────────────────────────

    @Test
    void leaveRequest_gettersAndSetters() {
        Dtos.LeaveRequest req = new Dtos.LeaveRequest();
        LocalDate date = LocalDate.of(2025, 7, 20);
        req.setLeaveDate(date);
        req.setReason("Personal");

        assertEquals(date, req.getLeaveDate());
        assertEquals("Personal", req.getReason());
    }

    @Test
    void leaveRequest_nullReason_isAllowed() {
        Dtos.LeaveRequest req = new Dtos.LeaveRequest();
        req.setLeaveDate(LocalDate.now());
        req.setReason(null);
        assertNull(req.getReason());
    }

    @Test
    void leaveRequest_equalObjects() {
        LocalDate date = LocalDate.of(2025, 8, 1);

        Dtos.LeaveRequest a = new Dtos.LeaveRequest();
        a.setLeaveDate(date); a.setReason("Sick");

        Dtos.LeaveRequest b = new Dtos.LeaveRequest();
        b.setLeaveDate(date); b.setReason("Sick");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void leaveRequest_sameInstance_isEqual() {
        Dtos.LeaveRequest r = new Dtos.LeaveRequest();
        assertEquals(r, r);
    }

    @Test
    void leaveRequest_notEqualToNull() {
        assertNotEquals(null, new Dtos.LeaveRequest());
    }

    @Test
    void leaveRequest_notEqualToDifferentType() {
        assertNotEquals("x", new Dtos.LeaveRequest());
    }

    @Test
    void leaveRequest_notEqualWhenDateDiffers() {
        Dtos.LeaveRequest a = new Dtos.LeaveRequest();
        a.setLeaveDate(LocalDate.of(2025, 1, 1));

        Dtos.LeaveRequest b = new Dtos.LeaveRequest();
        b.setLeaveDate(LocalDate.of(2025, 2, 1));

        assertNotEquals(a, b);
    }

    @Test
    void leaveRequest_notEqualWhenOneReasonIsNull() {
        Dtos.LeaveRequest a = new Dtos.LeaveRequest();
        a.setReason("Sick");

        Dtos.LeaveRequest b = new Dtos.LeaveRequest();
        b.setReason(null);

        assertNotEquals(a, b);
    }
}