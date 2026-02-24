package com.clinic.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data @NoArgsConstructor
@Entity @Table(name = "doctor_leaves",
    uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "leave_date"}))
public class DoctorLeave {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    private String reason;

    public DoctorLeave(Doctor doctor, LocalDate leaveDate, String reason) {
        this.doctor = doctor;
        this.leaveDate = leaveDate;
        this.reason = reason;
    }
}
