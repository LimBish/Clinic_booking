package com.clinic.performance;

import com.clinic.dto.Dtos;
import com.clinic.model.Doctor;
import com.clinic.model.DoctorSchedule;
import com.clinic.model.User;
import com.clinic.model.enums.Role;
import com.clinic.model.enums.Specialization;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.DoctorScheduleRepository;
import com.clinic.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.management.OperatingSystemMXBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Disabled by default: long-running load test; run manually when needed.")
class PatientAppointmentBookingPerformanceTest {

    private static final int TOTAL_CALLS = 50;
    private static final Duration TEST_DURATION = Duration.ofSeconds(30);
    private static final Duration SAMPLE_INTERVAL = Duration.ofSeconds(2);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpData() {
        doctorScheduleRepository.deleteAll();
        doctorRepository.deleteAll();
        userRepository.deleteAll();

        User patient = new User("Load Test Patient", "patient-load@clinic.com",
                passwordEncoder.encode("patient123"), "9800000000", Role.PATIENT);
        userRepository.save(patient);

        User doctorUser = new User("Dr. Load Test", "doctor-load@clinic.com",
                passwordEncoder.encode("doctor123"), "9800000001", Role.DOCTOR);
        userRepository.save(doctorUser);

        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setSpecialization(Specialization.GENERAL_PHYSICIAN);
        doctor.setConsultationFee(600);
        doctor.setActive(true);
        Doctor savedDoctor = doctorRepository.save(doctor);

        for (DayOfWeek day : DayOfWeek.values()) {
            DoctorSchedule schedule = new DoctorSchedule();
            schedule.setDoctor(savedDoctor);
            schedule.setDayOfWeek(day);
            schedule.setStartTime(LocalTime.of(0, 0));
            schedule.setEndTime(LocalTime.of(23, 55));
            schedule.setSlotDurationMinutes(5);
            doctorScheduleRepository.save(schedule);
        }
    }

    @Test
    void runPatientBookingLoadTest() throws Exception {
        String authCookie = loginAndGetJwtCookie();
        assertFalse(authCookie.isBlank());

        Long doctorId = doctorRepository.findAll().get(0).getId();

        List<BookingSlot> bookingSlots = generateBookingSlots(TOTAL_CALLS);
        assertTrue(bookingSlots.size() >= TOTAL_CALLS);

        var monitor = new ResourceMonitor();
        var stopSignal = new AtomicBoolean(false);
        var monitorThread = new Thread(() -> monitor.start(stopSignal));
        monitorThread.start();

        long successCount = 0;
        long failureCount = 0;
        long totalLatencyMs = 0;

        Duration intervalBetweenCalls = TEST_DURATION.dividedBy(TOTAL_CALLS);
        Instant startedAt = Instant.now();

        for (int i = 0; i < TOTAL_CALLS; i++) {
            BookingSlot slot = bookingSlots.get(i);
            String payload = buildBookingFormPayload(doctorId, slot.date(), slot.time(), "Load test " + i);

            long start = System.nanoTime();
            ResponseEntity<String> response = invokeBooking(payload, authCookie);
            long latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            totalLatencyMs += latency;

            if (response.getStatusCode().is3xxRedirection()) {
                successCount++;
            } else {
                failureCount++;
            }

            long expected = intervalBetweenCalls.toMillis() * (i + 1L);
            long actual = Duration.between(startedAt, Instant.now()).toMillis();
            if (expected > actual) {
                Thread.sleep(expected - actual);
            }
        }

        stopSignal.set(true);
        monitorThread.join();

        double avgLatency = totalLatencyMs / (double) TOTAL_CALLS;

        System.out.println("\n=== LOAD TEST REPORT ===");
        System.out.println("Total Calls: " + TOTAL_CALLS);
        System.out.println("Success: " + successCount);
        System.out.println("Failures: " + failureCount);
        System.out.println("Avg Latency: " + avgLatency + " ms");

        monitor.printSummary();

        assertTrue(successCount > 0);
        assertEquals(0, failureCount);
    }

    private String loginAndGetJwtCookie() throws IOException {
        String url = "http://localhost:" + port + "/api/auth/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Dtos.LoginRequest req = new Dtos.LoginRequest();
        req.setEmail("patient-load@clinic.com");
        req.setPassword("patient123");

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(req), headers),
                String.class
        );

        JsonNode body = objectMapper.readTree(response.getBody());
        return "jwt=" + body.get("token").asText();
    }

    private ResponseEntity<String> invokeBooking(String payload, String cookie) {
        String url = "http://localhost:" + port + "/patient/book";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, cookie);

        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
    }

    private String buildBookingFormPayload(Long doctorId, LocalDate date, LocalTime time, String reason) {
        return "doctorId=" + doctorId +
                "&appointmentDate=" + date +
                "&appointmentTime=" + time +
                "&reason=" + reason.replace(" ", "+");
    }

    private List<BookingSlot> generateBookingSlots(int count) {
        List<BookingSlot> slots = new ArrayList<>();
        LocalDate date = LocalDate.now().plusDays(1);

        while (slots.size() < count) {
            LocalTime time = LocalTime.of(0, 0);
            while (!time.isAfter(LocalTime.of(23, 50)) && slots.size() < count) {
                slots.add(new BookingSlot(date, time));
                time = time.plusMinutes(5);
            }
            date = date.plusDays(1);
        }
        return slots;
    }

    private record BookingSlot(LocalDate date, LocalTime time) {}

    private static class ResourceMonitor {
        private final OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        private final List<Double> cpuSamples = new ArrayList<>();

        void start(AtomicBoolean stopSignal) {
            while (!stopSignal.get()) {
                double cpu = osBean.getSystemCpuLoad();
                if (cpu >= 0) cpuSamples.add(cpu * 100);
                try {
                    Thread.sleep(SAMPLE_INTERVAL.toMillis());
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        void printSummary() {
            double avg = cpuSamples.stream().mapToDouble(d -> d).average().orElse(0);
            double max = cpuSamples.stream().mapToDouble(d -> d).max().orElse(0);

            System.out.println("\nCPU Avg: " + avg + "%");
            System.out.println("CPU Max: " + max + "%");
        }
    }
}

