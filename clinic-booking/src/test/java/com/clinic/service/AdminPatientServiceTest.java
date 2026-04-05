package com.clinic.service;

import com.clinic.dto.Dtos.AdminDashboardPatientStats;
import com.clinic.dto.Dtos.AdminPatientRow;
import com.clinic.exception.NotFoundException;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.User;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPatientServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AppointmentRepository appointmentRepository;

    @InjectMocks private AdminPatientService adminPatientService;

    private User p1;
    private User p2;

    @BeforeEach
    void setUp() {
        p1 = new User();
        p1.setId(1L);
        p1.setRole(Role.PATIENT);
        p1.setEmail("a@clinic.com");
        p1.setFullName("Alice");
        p1.setEnabled(true);
        p1.setCreatedAt(LocalDateTime.now().minusDays(10));

        p2 = new User();
        p2.setId(2L);
        p2.setRole(Role.PATIENT);
        p2.setEmail("b@clinic.com");
        p2.setFullName("Bob");
        p2.setEnabled(false);
        p2.setCreatedAt(LocalDateTime.now().minusDays(40));
    }

    @Test
    void getPatients_appliesSearchStatusAndAppointmentFilters() {
        Appointment appt = appointmentWithDoctor("Cuddy");
        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of(p1, p2));
        when(appointmentRepository.countByPatientId(1L)).thenReturn(2L);
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(1L)).thenReturn(List.of(appt));

        List<AdminPatientRow> rows = adminPatientService.getPatients("ali", "ACTIVE", null, "FEW");

        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).getFullName());
        assertEquals("Dr. Cuddy", rows.get(0).getBookedDoctors());
    }

    @Test
    void getPatients_whenNoAppointmentsFilterNone_returnsOnlyWithoutAppointments() {
        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of(p1, p2));
        when(appointmentRepository.countByPatientId(1L)).thenReturn(1L);
        when(appointmentRepository.countByPatientId(2L)).thenReturn(0L);
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(2L)).thenReturn(List.of());

        List<AdminPatientRow> rows = adminPatientService.getPatients(null, null, null, "NONE");

        assertEquals(1, rows.size());
        assertEquals(2L, rows.get(0).getId());
        assertEquals("—", rows.get(0).getBookedDoctors());
    }

    @Test
    void getPatient_whenWrongRole_throwsNotFound() {
        User doctor = new User();
        doctor.setId(7L);
        doctor.setRole(Role.DOCTOR);

        when(userRepository.findById(7L)).thenReturn(Optional.of(doctor));

        assertThrows(NotFoundException.class, () -> adminPatientService.getPatient(7L));
    }

    @Test
    void togglePatient_flipsEnabledAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(p1));

        adminPatientService.togglePatient(1L);

        assertFalse(p1.isEnabled());
        verify(userRepository).save(p1);
    }

    @Test
    void getDashboardPatientStats_whenNoPatients_setsZeroesForDerivedFields() {
        when(userRepository.countByRole(Role.PATIENT)).thenReturn(0L);
        when(userRepository.countByRoleAndCreatedAtBetween(eq(Role.PATIENT), any(), any())).thenReturn(0L);
        when(userRepository.countByRoleAndEnabledTrue(Role.PATIENT)).thenReturn(0L);
        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of());

        AdminDashboardPatientStats stats = adminPatientService.getDashboardPatientStats();

        assertEquals(0, stats.getFrequentPatients());
        assertEquals(0, stats.getNoShowPatients());
    }

    @Test
    void getDashboardPatientStats_computesFrequentAndNoShows() {
        when(userRepository.countByRole(Role.PATIENT)).thenReturn(2L);
        when(userRepository.countByRoleAndCreatedAtBetween(eq(Role.PATIENT), any(), any())).thenReturn(1L);
        when(userRepository.countByRoleAndEnabledTrue(Role.PATIENT)).thenReturn(1L);
        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of(p1, p2));

        AppointmentRepository.PatientAppointmentCount count = new AppointmentRepository.PatientAppointmentCount() {
            @Override public Long getPatientId() { return 1L; }
            @Override public Long getTotal() { return 3L; }
        };
        when(appointmentRepository.findAppointmentCountsByPatientIds(List.of(1L, 2L))).thenReturn(List.of(count));
        when(appointmentRepository.countByPatientIdAndStatus(1L, AppointmentStatus.NO_SHOW)).thenReturn(0L);
        when(appointmentRepository.countByPatientIdAndStatus(2L, AppointmentStatus.NO_SHOW)).thenReturn(2L);

        AdminDashboardPatientStats stats = adminPatientService.getDashboardPatientStats();

        assertEquals(2L, stats.getTotalPatients());
        assertEquals(1L, stats.getFrequentPatients());
        assertEquals(1L, stats.getNoShowPatients());
    }

    @Test
    void getMostFrequentPatients_buildsRowsFromCounts() {
        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of(p1));
        AppointmentRepository.PatientAppointmentCount count = new AppointmentRepository.PatientAppointmentCount() {
            @Override public Long getPatientId() { return 1L; }
            @Override public Long getTotal() { return 5L; }
        };
        when(appointmentRepository.findAppointmentCountsByPatientIds(List.of(1L))).thenReturn(List.of(count));
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(1L))
                .thenReturn(List.of(appointmentWithDoctor("Gregory House")));

        List<AdminPatientRow> rows = adminPatientService.getMostFrequentPatients(5);

        assertEquals(1, rows.size());
        assertEquals(5L, rows.get(0).getAppointmentCount());
        assertEquals("Dr. Gregory House", rows.get(0).getBookedDoctors());
    }

    @Test
    void getNoShowPatients_returnsSortedRows() {
        when(userRepository.findByRole(Role.PATIENT)).thenReturn(List.of(p1, p2));
        when(appointmentRepository.countByPatientIdAndStatus(1L, AppointmentStatus.NO_SHOW)).thenReturn(1L);
        when(appointmentRepository.countByPatientIdAndStatus(2L, AppointmentStatus.NO_SHOW)).thenReturn(1L);
        when(appointmentRepository.countByPatientId(1L)).thenReturn(5L);
        when(appointmentRepository.countByPatientId(2L)).thenReturn(2L);
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(1L))
                .thenReturn(List.of(appointmentWithDoctor("A")));
        when(appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(2L))
                .thenReturn(List.of(appointmentWithDoctor("B")));

        List<AdminPatientRow> rows = adminPatientService.getNoShowPatients();

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).getAppointmentCount() >= rows.get(1).getAppointmentCount());
    }

    private Appointment appointmentWithDoctor(String name) {
        User doctorUser = new User();
        doctorUser.setFullName(name);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        Appointment appt = new Appointment();
        appt.setDoctor(doctor);
        appt.setAppointmentDate(LocalDate.now());
        return appt;
    }
}