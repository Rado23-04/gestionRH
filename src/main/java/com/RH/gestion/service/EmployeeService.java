package com.RH.gestion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.RH.gestion.model.Employee;
import com.RH.gestion.repository.EmployeeRepository;
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee savEmployee(Employee employee){
        return employeeRepository.save(employee);
    }


    public BigDecimal calculateHourlyRate(Long employeeId) {
        final BigDecimal MONTHLY_HOURS = new BigDecimal("173.33");

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()->new EntityNotFoundException());

        BigDecimal baseSalary= employee.getSalaireBase();

        if(baseSalary== null || baseSalary.compareTo(BigDecimal.ZERO)<0){
            return BigDecimal.ZERO;
        }
        BigDecimal horlyRate = baseSalary.divide(MONTHLY_HOURS,4, RoundingMode.HALF_UP);
        return horlyRate;
    }
//couplage faible (ou Loose Coupling)
}
