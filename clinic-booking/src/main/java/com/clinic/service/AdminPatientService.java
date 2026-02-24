package com.clinic.service;

import com.clinic.dto.Dtos.AdminDashboardPatientStats;
import com.clinic.dto.Dtos.AdminPatientRow;
import com.clinic.exception.NotFoundException;
import com.clinic.model.Appointment;
import com.clinic.model.User;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPatientService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    public List<AdminPatientRow> getPatients(String search, String status, String registration, String appointments) {
        List<User> patients = userRepository.findByRole(Role.PATIENT);

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            patients = patients.stream()
                    .filter(p -> p.getFullName().toLowerCase().contains(q) || p.getEmail().toLowerCase().contains(q))
                    .toList();
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            patients = patients.stream().filter(User::isEnabled).toList();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            patients = patients.stream().filter(p -> !p.isEnabled()).toList();
        }

        if ("RECENT".equalsIgnoreCase(registration)) {
            LocalDateTime from = LocalDateTime.now().minusDays(30);
            patients = patients.stream()
                    .filter(
                            p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(from))
                    .toList();
        }

        Map<Long, Long> appointmentCounts = patients.stream()
                .collect(Collectors.toMap(User::getId, p -> appointmentRepository.countByPatientId(p.getId())));

        if ("NONE".equalsIgnoreCase(appointments)) {
            patients = patients.stream().filter(p -> appointmentCounts.getOrDefault(p.getId(), 0L) == 0).toList();
        } else if ("FEW".equalsIgnoreCase(appointments)) {
            patients = patients.stream().filter(p -> {
                long c = appointmentCounts.getOrDefault(p.getId(), 0L);
                return c > 0 && c < 3;
            }).toList();
        } else if ("MANY".equalsIgnoreCase(appointments)) {
            patients = patients.stream().filter(p -> appointmentCounts.getOrDefault(p.getId(), 0L) >= 3).toList();
        }

        return patients.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(p -> AdminPatientRow.of(
                        p.getId(),
                        p.getFullName(),
                        p.getEmail(),
                        p.getCreatedAt() == null ? null : p.getCreatedAt().toLocalDate(),
                        p.isEnabled(),
                        appointmentCounts.getOrDefault(p.getId(), 0L)
                ))
                .toList();
    }

    public User getPatient(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient", patientId));
        if (patient.getRole() != Role.PATIENT) {
            throw new NotFoundException("Patient", patientId);
        }
        return patient;
    }

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId);
    }

    public void togglePatient(Long patientId) {
        User patient = getPatient(patientId);
        patient.setEnabled(!patient.isEnabled());
        userRepository.save(patient);
    }

    public AdminDashboardPatientStats getDashboardPatientStats() {
        AdminDashboardPatientStats stats = new AdminDashboardPatientStats();
        stats.setTotalPatients(userRepository.countByRole(Role.PATIENT));

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime from = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime to = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        stats.setNewPatientsThisMonth(userRepository.countByRoleAndCreatedAtBetween(Role.PATIENT, from, to));

        stats.setActivePatients(userRepository.countByRoleAndEnabledTrue(Role.PATIENT));

        List<User> patients = userRepository.findByRole(Role.PATIENT);
        List<Long> patientIds = patients.stream().map(User::getId).toList();
        if (patientIds.isEmpty()) {
            stats.setFrequentPatients(0);
            stats.setNoShowPatients(0);
            return stats;
        }

        Map<Long, Long> appointmentCounts = appointmentRepository.findAppointmentCountsByPatientIds(patientIds).stream()
                .collect(Collectors.toMap(AppointmentRepository.PatientAppointmentCount::getPatientId,
                        AppointmentRepository.PatientAppointmentCount::getTotal));

        stats.setFrequentPatients(appointmentCounts.values().stream().filter(c -> c >= 3).count());

        long noShows = patientIds.stream()
                .filter(id -> appointmentRepository.countByPatientIdAndStatus(id, AppointmentStatus.NO_SHOW) > 0)
                .count();
        stats.setNoShowPatients(noShows);
        return stats;
    }

    public List<AdminPatientRow> getMostFrequentPatients(int limit) {
        List<User> patients = userRepository.findByRole(Role.PATIENT);
        Map<Long, User> byId = patients.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        if (byId.isEmpty()) {
            return List.of();
        }

        return appointmentRepository.findAppointmentCountsByPatientIds(new ArrayList<>(byId.keySet())).stream()
                .limit(limit)
                .map(c -> {
                    User patient = byId.get(c.getPatientId());
                    return AdminPatientRow.of(patient.getId(), patient.getFullName(), patient.getEmail(),
                            patient.getCreatedAt() == null ? null : patient.getCreatedAt().toLocalDate(),
                            patient.isEnabled(), c.getTotal());
                })
                .toList();
    }

    public List<AdminPatientRow> getNoShowPatients() {
        List<User> patients = userRepository.findByRole(Role.PATIENT);
        return patients.stream()
                .filter(p -> appointmentRepository.countByPatientIdAndStatus(p.getId(), AppointmentStatus.NO_SHOW) > 0)
                .map(p -> AdminPatientRow.of(
                        p.getId(),
                        p.getFullName(),
                        p.getEmail(),
                        p.getCreatedAt() == null ? null : p.getCreatedAt().toLocalDate(),
                        p.isEnabled(),
                        appointmentRepository.countByPatientId(p.getId())
                ))
                .sorted(Comparator.comparing(AdminPatientRow::getAppointmentCount).reversed())
                .toList();
    }
}