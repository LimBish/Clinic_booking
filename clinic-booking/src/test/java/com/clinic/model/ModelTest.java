package com.clinic.model;

import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.model.enums.Specialization;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // ── User ──────────────────────────────────────────────────────────────────

    @Test
    void user_noArgConstructor_defaultsEnabledTrue() {
        User user = new User();
        assertTrue(user.isEnabled());
    }

    @Test
    void user_allArgConstructor_setsFields() {
        User user = new User("Alice", "alice@clinic.com", "pass123", "9876543210", Role.PATIENT);
        assertEquals("Alice", user.getFullName());
        assertEquals("alice@clinic.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
        assertEquals("9876543210", user.getPhone());
        assertEquals(Role.PATIENT, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    void user_setters_overrideValues() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Bob");
        user.setEmail("bob@clinic.com");
        user.setPassword("secret");
        user.setPhone("1234567890");
        user.setRole(Role.DOCTOR);
        user.setEnabled(false);

        assertEquals(1L, user.getId());
        assertEquals("Bob", user.getFullName());
        assertEquals("bob@clinic.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("1234567890", user.getPhone());
        assertEquals(Role.DOCTOR, user.getRole());
        assertFalse(user.isEnabled());
    }

    @Test
    void user_prePersist_setsCreatedAtIfNull() {
        User user = new User();
        assertNull(user.getCreatedAt());
        user.prePersist();
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void user_prePersist_doesNotOverrideExistingCreatedAt() {
        User user = new User();
        LocalDateTime existing = LocalDateTime.of(2024, 1, 1, 0, 0);
        user.setCreatedAt(existing);
        user.prePersist();
        assertEquals(existing, user.getCreatedAt());
    }

    @Test
    void user_equalsAndHashCode_basedOnFields() {
        User u1 = new User("Alice", "alice@clinic.com", "pass", "123", Role.PATIENT);
        User u2 = new User("Alice", "alice@clinic.com", "pass", "123", Role.PATIENT);
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    @Test
    void user_toString_containsEmail() {
        User user = new User();
        user.setEmail("test@clinic.com");
        assertTrue(user.toString().contains("test@clinic.com"));
    }

    @Test
    void user_notEqualToNull() {
        assertNotEquals(null, new User());
    }

    @Test
    void user_notEqualToDifferentType() {
        assertNotEquals("string", new User());
    }

    @Test
    void user_sameInstance_isEqual() {
        User u = new User();
        assertEquals(u, u);
    }

    @Test
    void user_notEqualWhenEmailDiffers() {
        User a = new User(); a.setEmail("a@b.com");
        User b = new User(); b.setEmail("c@d.com");
        assertNotEquals(a, b);
    }

    // ── Doctor ────────────────────────────────────────────────────────────────

    @Test
    void doctor_noArgConstructor_defaultsActiveTrue() {
        Doctor doctor = new Doctor();
        assertTrue(doctor.isActive());
    }

    @Test
    void doctor_setters_workCorrectly() {
        User user = new User("Dr. Jane", "jane@clinic.com", "pass", "999", Role.DOCTOR);
        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setUser(user);
        doctor.setSpecialization(Specialization.CARDIOLOGY);
        doctor.setBio("Experienced cardiologist");
        doctor.setConsultationFee(500);
        doctor.setActive(false);

        assertEquals(10L, doctor.getId());
        assertEquals(user, doctor.getUser());
        assertEquals(Specialization.CARDIOLOGY, doctor.getSpecialization());
        assertEquals("Experienced cardiologist", doctor.getBio());
        assertEquals(500, doctor.getConsultationFee());
        assertFalse(doctor.isActive());
    }

    @Test
    void doctor_nullSpecializationAndFee_allowed() {
        Doctor doctor = new Doctor();
        doctor.setSpecialization(null);
        doctor.setConsultationFee(null);
        assertNull(doctor.getSpecialization());
        assertNull(doctor.getConsultationFee());
    }

    @Test
    void doctor_equalsAndHashCode() {
        Doctor d1 = new Doctor();
        d1.setId(1L); d1.setBio("Bio");

        Doctor d2 = new Doctor();
        d2.setId(1L); d2.setBio("Bio");

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void doctor_toString_containsBio() {
        Doctor doctor = new Doctor();
        doctor.setBio("Neurologist");
        assertTrue(doctor.toString().contains("Neurologist"));
    }

    @Test
    void doctor_notEqualToNull() {
        assertNotEquals(null, new Doctor());
    }

    @Test
    void doctor_notEqualToDifferentType() {
        assertNotEquals("x", new Doctor());
    }

    @Test
    void doctor_sameInstance_isEqual() {
        Doctor d = new Doctor();
        assertEquals(d, d);
    }

    @Test
    void doctor_notEqualWhenBioDiffers() {
        Doctor a = new Doctor(); a.setBio("A");
        Doctor b = new Doctor(); b.setBio("B");
        assertNotEquals(a, b);
    }

    // ── Appointment ───────────────────────────────────────────────────────────

    @Test
    void appointment_noArgConstructor_defaultsStatusConfirmed() {
        Appointment appt = new Appointment();
        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());
    }

    @Test
    void appointment_setters_workCorrectly() {
        User patient = new User("Pat", "pat@clinic.com", "pw", "111", Role.PATIENT);
        Doctor doctor = new Doctor();
        doctor.setId(2L);

        LocalDate date = LocalDate.of(2025, 6, 15);
        LocalTime time = LocalTime.of(10, 30);
        LocalDateTime consultedAt = LocalDateTime.of(2025, 6, 15, 11, 0);

        Appointment appt = new Appointment();
        appt.setId(5L);
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setAppointmentDate(date);
        appt.setAppointmentTime(time);
        appt.setReason("Headache");
        appt.setStatus(AppointmentStatus.COMPLETED);
        appt.setConsultationNotes("Prescribed rest.");
        appt.setConsultedAt(consultedAt);

        assertEquals(5L, appt.getId());
        assertEquals(patient, appt.getPatient());
        assertEquals(doctor, appt.getDoctor());
        assertEquals(date, appt.getAppointmentDate());
        assertEquals(time, appt.getAppointmentTime());
        assertEquals("Headache", appt.getReason());
        assertEquals(AppointmentStatus.COMPLETED, appt.getStatus());
        assertEquals("Prescribed rest.", appt.getConsultationNotes());
        assertEquals(consultedAt, appt.getConsultedAt());
    }

    @Test
    void appointment_nullableFields_allowed() {
        Appointment appt = new Appointment();
        appt.setReason(null);
        appt.setConsultationNotes(null);
        appt.setConsultedAt(null);
        assertNull(appt.getReason());
        assertNull(appt.getConsultationNotes());
        assertNull(appt.getConsultedAt());
    }

    @Test
    void appointment_equalsAndHashCode() {
        Appointment a1 = new Appointment();
        a1.setId(1L); a1.setReason("Cold");

        Appointment a2 = new Appointment();
        a2.setId(1L); a2.setReason("Cold");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void appointment_notEqualToNull() {
        assertNotEquals(null, new Appointment());
    }

    @Test
    void appointment_notEqualToDifferentType() {
        assertNotEquals("x", new Appointment());
    }

    @Test
    void appointment_sameInstance_isEqual() {
        Appointment a = new Appointment();
        assertEquals(a, a);
    }

    // ── DoctorLeave ───────────────────────────────────────────────────────────

    @Test
    void doctorLeave_noArgConstructor_works() {
        DoctorLeave leave = new DoctorLeave();
        assertNull(leave.getLeaveDate());
        assertNull(leave.getReason());
    }

    @Test
    void doctorLeave_allArgConstructor_setsFields() {
        Doctor doctor = new Doctor();
        doctor.setId(3L);
        LocalDate date = LocalDate.of(2025, 7, 4);

        DoctorLeave leave = new DoctorLeave(doctor, date, "Vacation");
        assertEquals(doctor, leave.getDoctor());
        assertEquals(date, leave.getLeaveDate());
        assertEquals("Vacation", leave.getReason());
    }

    @Test
    void doctorLeave_setters_workCorrectly() {
        DoctorLeave leave = new DoctorLeave();
        Doctor doctor = new Doctor();
        LocalDate date = LocalDate.now().plusDays(5);

        leave.setId(1L);
        leave.setDoctor(doctor);
        leave.setLeaveDate(date);
        leave.setReason("Sick");

        assertEquals(1L, leave.getId());
        assertEquals(doctor, leave.getDoctor());
        assertEquals(date, leave.getLeaveDate());
        assertEquals("Sick", leave.getReason());
    }

    @Test
    void doctorLeave_nullReason_allowed() {
        Doctor doctor = new Doctor();
        DoctorLeave leave = new DoctorLeave(doctor, LocalDate.now(), null);
        assertNull(leave.getReason());
    }

    @Test
    void doctorLeave_equalsAndHashCode() {
        Doctor doctor = new Doctor();
        LocalDate date = LocalDate.of(2025, 8, 1);

        DoctorLeave a = new DoctorLeave(doctor, date, "Rest");
        DoctorLeave b = new DoctorLeave(doctor, date, "Rest");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void doctorLeave_notEqualToNull() {
        assertNotEquals(null, new DoctorLeave());
    }

    @Test
    void doctorLeave_notEqualToDifferentType() {
        assertNotEquals("x", new DoctorLeave());
    }

    @Test
    void doctorLeave_sameInstance_isEqual() {
        DoctorLeave l = new DoctorLeave();
        assertEquals(l, l);
    }

    @Test
    void doctorLeave_notEqualWhenReasonDiffers() {
        Doctor d = new Doctor();
        LocalDate date = LocalDate.now();
        DoctorLeave a = new DoctorLeave(d, date, "Sick");
        DoctorLeave b = new DoctorLeave(d, date, "Vacation");
        assertNotEquals(a, b);
    }

    // ── DoctorSchedule ────────────────────────────────────────────────────────

    @Test
    void doctorSchedule_noArgConstructor_defaultsSlotDuration30() {
        DoctorSchedule schedule = new DoctorSchedule();
        assertEquals(30, schedule.getSlotDurationMinutes());
    }

    @Test
    void doctorSchedule_setters_workCorrectly() {
        Doctor doctor = new Doctor();
        doctor.setId(4L);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setSlotDurationMinutes(45);

        assertEquals(1L, schedule.getId());
        assertEquals(doctor, schedule.getDoctor());
        assertEquals(DayOfWeek.MONDAY, schedule.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), schedule.getStartTime());
        assertEquals(LocalTime.of(17, 0), schedule.getEndTime());
        assertEquals(45, schedule.getSlotDurationMinutes());
    }

    @Test
    void doctorSchedule_allDaysOfWeek_accepted() {
        for (DayOfWeek day : DayOfWeek.values()) {
            DoctorSchedule schedule = new DoctorSchedule();
            schedule.setDayOfWeek(day);
            assertEquals(day, schedule.getDayOfWeek());
        }
    }

    @Test
    void doctorSchedule_equalsAndHashCode() {
        DoctorSchedule s1 = new DoctorSchedule();
        s1.setDayOfWeek(DayOfWeek.FRIDAY);
        s1.setStartTime(LocalTime.of(8, 0));

        DoctorSchedule s2 = new DoctorSchedule();
        s2.setDayOfWeek(DayOfWeek.FRIDAY);
        s2.setStartTime(LocalTime.of(8, 0));

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void doctorSchedule_toString_containsDayOfWeek() {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDayOfWeek(DayOfWeek.WEDNESDAY);
        assertTrue(schedule.toString().contains("WEDNESDAY"));
    }

    @Test
    void doctorSchedule_notEqualToNull() {
        assertNotEquals(null, new DoctorSchedule());
    }

    @Test
    void doctorSchedule_notEqualToDifferentType() {
        assertNotEquals("x", new DoctorSchedule());
    }

    @Test
    void doctorSchedule_sameInstance_isEqual() {
        DoctorSchedule s = new DoctorSchedule();
        assertEquals(s, s);
    }

    @Test
    void doctorSchedule_notEqualWhenDayDiffers() {
        DoctorSchedule a = new DoctorSchedule(); a.setDayOfWeek(DayOfWeek.MONDAY);
        DoctorSchedule b = new DoctorSchedule(); b.setDayOfWeek(DayOfWeek.FRIDAY);
        assertNotEquals(a, b);
    }
}