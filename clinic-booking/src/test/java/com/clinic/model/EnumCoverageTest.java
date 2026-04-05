package com.clinic.model;

import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.model.enums.Specialization;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumCoverageTest {

    @Test
    void enums_haveExpectedValues() {
        assertTrue(Role.valueOf("ADMIN") == Role.ADMIN);
        assertTrue(AppointmentStatus.valueOf("CONFIRMED") == AppointmentStatus.CONFIRMED);
        assertTrue(Specialization.valueOf("CARDIOLOGY") == Specialization.CARDIOLOGY);
    }
}