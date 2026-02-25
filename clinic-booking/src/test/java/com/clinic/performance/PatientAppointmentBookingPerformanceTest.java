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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PatientAppointmentBookingPerformanceTest {

    private static final int TOTAL_CALLS = 1_000;
    private static final Duration TEST_DURATION = Duration.ofMinutes(5);
    private static final Duration SAMPLE_INTERVAL = Duration.ofSeconds(1);

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
    void runPatientBookingLoadForFiveMinutesAndCollectServerMetrics() throws Exception {
        String authCookie = loginAndGetJwtCookie();
        assertFalse(authCookie.isBlank(), "JWT cookie should be created for patient user");

        Long doctorId = doctorRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        List<BookingSlot> bookingSlots = generateBookingSlots(TOTAL_CALLS);
        assertTrue(bookingSlots.size() >= TOTAL_CALLS, "Should generate at least 1000 unique booking slots");

        var monitor = new ResourceMonitor();
        var stopSignal = new AtomicBoolean(false);
        var monitorThread = new Thread(() -> monitor.start(stopSignal));
        monitorThread.setName("resource-monitor");
        monitorThread.start();

        long successCount = 0;
        long failureCount = 0;
        long totalLatencyMs = 0;
        long minLatencyMs = Long.MAX_VALUE;
        long maxLatencyMs = Long.MIN_VALUE;

        Duration intervalBetweenCalls = TEST_DURATION.dividedBy(TOTAL_CALLS);
        Instant startedAt = Instant.now();

        for (int i = 0; i < TOTAL_CALLS; i++) {
            BookingSlot slot = bookingSlots.get(i);
            String formPayload = buildBookingFormPayload(doctorId, slot.date(), slot.time(), "Load test booking " + i);

            long callStart = System.nanoTime();
            ResponseEntity<String> response = invokeBooking(formPayload, authCookie);
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - callStart);

            totalLatencyMs += latencyMs;
            minLatencyMs = Math.min(minLatencyMs, latencyMs);
            maxLatencyMs = Math.max(maxLatencyMs, latencyMs);

            if (response.getStatusCode().is3xxRedirection()) {
                successCount++;
            } else {
                failureCount++;
                System.out.printf("Unexpected response at call %d: status=%s, body=%s%n",
                        i + 1, response.getStatusCode(), response.getBody());
            }

            long expectedElapsedMs = intervalBetweenCalls.toMillis() * (i + 1L);
            long actualElapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
            if (expectedElapsedMs > actualElapsedMs) {
                Thread.sleep(expectedElapsedMs - actualElapsedMs);
            }
        }

        stopSignal.set(true);
        monitorThread.join();

        long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
        double avgLatencyMs = totalLatencyMs / (double) TOTAL_CALLS;

        System.out.println("\n=== PATIENT APPOINTMENT BOOKING LOAD TEST REPORT ===");
        System.out.printf(Locale.US, "Endpoint: %s%n", "http://localhost:" + port + "/patient/book");
        System.out.printf(Locale.US, "Configured total calls: %d%n", TOTAL_CALLS);
        System.out.printf(Locale.US, "Configured duration: %d seconds%n", TEST_DURATION.toSeconds());
        System.out.printf(Locale.US, "Actual duration: %.2f seconds%n", elapsedMs / 1_000.0);
        System.out.printf(Locale.US, "Success calls: %d%n", successCount);
        System.out.printf(Locale.US, "Failed calls: %d%n", failureCount);
        System.out.printf(Locale.US, "Average latency: %.2f ms%n", avgLatencyMs);
        System.out.printf(Locale.US, "Min latency: %d ms%n", minLatencyMs == Long.MAX_VALUE ? 0 : minLatencyMs);
        System.out.printf(Locale.US, "Max latency: %d ms%n", maxLatencyMs == Long.MIN_VALUE ? 0 : maxLatencyMs);
        monitor.printSummary();

        assertTrue(successCount > 0, "At least one booking request should succeed");
        assertTrue(failureCount == 0, "No booking request should fail");
    }

    private String loginAndGetJwtCookie() throws IOException {
        String endpoint = "http://localhost:" + port + "/api/auth/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Dtos.LoginRequest loginRequest = new Dtos.LoginRequest();
        loginRequest.setEmail("patient-load@clinic.com");
        loginRequest.setPassword("patient123");

        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers), String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Login should return 200");
        JsonNode body = objectMapper.readTree(response.getBody());
        assertTrue(body.hasNonNull("token"), "Login body should include token");

        String token = body.get("token").asText();
        return "jwt=" + token;
    }

    private ResponseEntity<String> invokeBooking(String payload, String authCookie) {
        String endpoint = "http://localhost:" + port + "/patient/book";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, authCookie);

        return restTemplate.exchange(endpoint, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
    }

    private String buildBookingFormPayload(Long doctorId, LocalDate date, LocalTime time, String reason) {
        return "doctorId=" + doctorId
                + "&appointmentDate=" + date
                + "&appointmentTime=" + time
                + "&reason=" + reason.replace(" ", "+");
    }

    private List<BookingSlot> generateBookingSlots(int requiredSlots) {
        List<BookingSlot> slots = new ArrayList<>();
        LocalDate date = LocalDate.now().plusDays(1);

        while (slots.size() < requiredSlots) {
            LocalTime current = LocalTime.of(0, 0);
            while (!current.isAfter(LocalTime.of(23, 50)) && slots.size() < requiredSlots) {
                slots.add(new BookingSlot(date, current));
                current = current.plusMinutes(5);
            }
            date = date.plusDays(1);
        }
        return slots;
    }

    private record BookingSlot(LocalDate date, LocalTime time) {}

    private static class ResourceMonitor {
        private final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        private final List<Double> cpuSamples = new ArrayList<>();
        private final List<Long> usedMemorySamples = new ArrayList<>();
        private final List<Double> rxBandwidthSamplesKb = new ArrayList<>();
        private final List<Double> txBandwidthSamplesKb = new ArrayList<>();

        private NetSnapshot previousSnapshot;

        void start(AtomicBoolean stopSignal) {
            while (!stopSignal.get()) {
                sampleSystemUsage();
                try {
                    Thread.sleep(SAMPLE_INTERVAL.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            sampleSystemUsage();
        }

        private void sampleSystemUsage() {
            double cpuLoad = osBean.getSystemCpuLoad();
            if (cpuLoad >= 0) {
                cpuSamples.add(cpuLoad * 100.0);
            }

            long totalMem = osBean.getTotalMemorySize();
            long freeMem = osBean.getFreeMemorySize();
            usedMemorySamples.add(totalMem - freeMem);

            var current = readNetworkSnapshot();
            if (current != null && previousSnapshot != null) {
                long elapsedMs = current.timestampMs - previousSnapshot.timestampMs;
                if (elapsedMs > 0) {
                    double seconds = elapsedMs / 1_000.0;
                    rxBandwidthSamplesKb.add((current.rxBytes - previousSnapshot.rxBytes) / 1024.0 / seconds);
                    txBandwidthSamplesKb.add((current.txBytes - previousSnapshot.txBytes) / 1024.0 / seconds);
                }
            }
            previousSnapshot = current;
        }

        private NetSnapshot readNetworkSnapshot() {
            Path path = Path.of("/proc/net/dev");
            if (!Files.exists(path)) {
                return null;
            }

            try {
                long totalRx = 0;
                long totalTx = 0;
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.contains(":")) {
                        continue;
                    }
                    String[] parts = line.split(":");
                    String iface = parts[0].trim();
                    if (iface.equals("lo")) {
                        continue;
                    }
                    String[] columns = parts[1].trim().split("\\s+");
                    if (columns.length < 16) {
                        continue;
                    }
                    totalRx += Long.parseLong(columns[0]);
                    totalTx += Long.parseLong(columns[8]);
                }
                return new NetSnapshot(totalRx, totalTx, System.currentTimeMillis());
            } catch (IOException | NumberFormatException e) {
                return null;
            }
        }

        void printSummary() {
            System.out.println("\n=== SERVER RESOURCE SUMMARY (during test window) ===");
            System.out.printf(Locale.US, "CPU load avg/max: %.2f%% / %.2f%%%n", avg(cpuSamples), max(cpuSamples));
            System.out.printf(Locale.US, "Used memory avg/max: %.2f MB / %.2f MB%n",
                    avgBytes(usedMemorySamples), maxBytes(usedMemorySamples));
            System.out.printf(Locale.US, "Network RX avg/max: %.2f KB/s / %.2f KB/s%n",
                    avg(rxBandwidthSamplesKb), max(rxBandwidthSamplesKb));
            System.out.printf(Locale.US, "Network TX avg/max: %.2f KB/s / %.2f KB/s%n",
                    avg(txBandwidthSamplesKb), max(txBandwidthSamplesKb));
        }

        private double avg(List<Double> values) {
            return values.isEmpty() ? 0.0 : values.stream().mapToDouble(v -> v).average().orElse(0.0);
        }

        private double max(List<Double> values) {
            return values.isEmpty() ? 0.0 : values.stream().mapToDouble(v -> v).max().orElse(0.0);
        }

        private double avgBytes(List<Long> values) {
            if (values.isEmpty()) return 0.0;
            return values.stream().mapToLong(v -> v).average().orElse(0.0) / (1024.0 * 1024.0);
        }

        private double maxBytes(List<Long> values) {
            if (values.isEmpty()) return 0.0;
            return values.stream().mapToLong(v -> v).max().orElse(0L) / (1024.0 * 1024.0);
        }
    }

    private record NetSnapshot(long rxBytes, long txBytes, long timestampMs) {}
}