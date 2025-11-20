package com.RH.gestion.controller;

import com.RH.gestion.model.Payroll;
import com.RH.gestion.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/payroll")
public class PayrollController {
    @Autowired
    private PayrollService payrollService;

    @PostMapping("")
    public Payroll calculateNetPay (@PathVariable Long employeeId, LocalDate date,){
        payrollService.calculateNetPay(employeeId,date);
        return payrollService.savePayroll()
    }
}
