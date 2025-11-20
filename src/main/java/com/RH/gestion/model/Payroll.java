package com.RH.gestion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Payroll")
public class Payroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Employee employee;
    private LocalDate month;
    private BigDecimal grossSalary;
    private BigDecimal totalOvertimeAmount;
    private BigDecimal totalCotisations;
    private BigDecimal irsaDue;
    private BigDecimal netToPay;
}
