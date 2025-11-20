package com.RH.gestion.service;

import com.RH.gestion.model.Employee;
import com.RH.gestion.model.Payroll;
import com.RH.gestion.repository.EmployeeRepository;
import com.RH.gestion.repository.PayrollRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PayrollService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private TimeEntryService timeEntryService;

    @Autowired
    private PayrollRepository payrollRepository;

    public BigDecimal calculatePay(Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()->new EntityNotFoundException("employee not found"));

        BigDecimal salaireBase = employee.getSalaireBase();

        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.withMonth(date.lengthOfMonth());

        BigDecimal HS = timeEntryService.calculateMonthlyOvertime(employeeId,start,end);

        return salaireBase.add(HS);
    }

    private BigDecimal calculateEmployeeCotisations(BigDecimal salaireBrute) {
        final BigDecimal CNAPS_CEILING = new BigDecimal("580000");

        final BigDecimal CNAPS_RATE = new BigDecimal("0.01");
        final BigDecimal OSTIE_RATE = new BigDecimal("0.01");

        BigDecimal cnapsBase = salaireBrute.min(CNAPS_CEILING);
        BigDecimal cnapsAmount = cnapsBase.multiply(CNAPS_RATE);

        BigDecimal ostieAmount = salaireBrute.multiply(OSTIE_RATE);

        BigDecimal totalCotisations = cnapsAmount.add(ostieAmount);

        return totalCotisations.setScale(0, RoundingMode.HALF_UP);
    }


    private BigDecimal calculateRevenuImposable(BigDecimal salaireBrute, BigDecimal totalCotisations) {
        return salaireBrute.subtract(totalCotisations);
    }


    private BigDecimal calculateIRSA(BigDecimal revenuImposable) {
        final BigDecimal T1_LIMIT = new BigDecimal("350000");
        final BigDecimal T2_LIMIT = new BigDecimal("400000");
        final BigDecimal T3_LIMIT = new BigDecimal("500000");
        final BigDecimal T4_LIMIT = new BigDecimal("600000");
        final BigDecimal T5_LIMIT = new BigDecimal("4000000");

        if (revenuImposable.compareTo(T1_LIMIT) <= 0) {
            return BigDecimal.ZERO;
        }

        // 2. Tranche 2 : De 350,001 à 400,000 (5%)
        if (revenuImposable.compareTo(T2_LIMIT) <= 0) {
            // Base pour T2 : RI - 350 000
            BigDecimal baseTranche = revenuImposable.subtract(T1_LIMIT);

            // Impôt : (Base * 5%)
            return baseTranche.multiply(new BigDecimal("0.05"));
        }

        // 3. Tranche 3 : De 400,001 à 500,000 (10%)
        if (revenuImposable.compareTo(T3_LIMIT) <= 0) {

            // Impôt cumulé de la T2 complète
            final BigDecimal FIXED_TAX_UP_TO_500K = new BigDecimal("10000");

            // a. Quelle est la Base Imposable pour cette tranche (le montant au-delà de 400 000) ?
            BigDecimal baseTranche = revenuImposable.subtract(T2_LIMIT); // RI - 400 000

            // b. Calcul de l'impôt de cette tranche : Base * 10%
            BigDecimal taxOnTranche = baseTranche.multiply(new BigDecimal("0.1"));

            // c. Résultat : Impôt fixe + Impôt de la tranche actuelle
            return FIXED_TAX_UP_TO_500K.add(taxOnTranche);
        }
        // 3. Tranche 4 : De 500,001 à 600,000
        if(revenuImposable.compareTo(T4_LIMIT)<=0){

            // Impôt cumulé de la T3 complète
            final BigDecimal FIXED_TAX_UP_TO_600K = new BigDecimal("15000");

            BigDecimal baseTranche = revenuImposable.subtract(T3_LIMIT);

            BigDecimal taxOnTranche = baseTranche.multiply(new BigDecimal("0.15"));

            return FIXED_TAX_UP_TO_600K.add(taxOnTranche);
        }
        // 4. Tranche 5 : De 600,000 à 4,000,000
        if(revenuImposable.compareTo(T5_LIMIT)<=0){

            // Impôt cumulé de la T3 complète
            final BigDecimal FIXED_TAX_UP_TO_4M = new BigDecimal("20000");

            BigDecimal baseTranche = revenuImposable.subtract(T4_LIMIT);

            BigDecimal taxOnTranche = baseTranche.multiply(new BigDecimal("0.2"));

            return FIXED_TAX_UP_TO_4M.add(taxOnTranche);
        }
        return BigDecimal.ZERO;
    }
    public BigDecimal calculateNetPay(Long employeeId, LocalDate date) {

        LocalDate startDate = date.withDayOfMonth(1);
        LocalDate endDate = date.withDayOfMonth(date.lengthOfMonth());

        BigDecimal HS = timeEntryService.calculateMonthlyOvertime(employeeId,startDate,endDate);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()->new EntityNotFoundException());
        BigDecimal getSalary = employee.getSalaireBase();
        BigDecimal grosseSalary = getSalary.add(HS);

        BigDecimal absenceDeduction = leaveRequestService.calculateAbsenceDeduction(employeeId,date);
        grosseSalary = grosseSalary.subtract(absenceDeduction);

        BigDecimal getCotisation =
                calculateEmployeeCotisations(grosseSalary);

        BigDecimal revenuImposable=
                calculateRevenuImposable(grosseSalary,getCotisation);

        BigDecimal irsaDue = calculateIRSA(revenuImposable);

        BigDecimal totalRetenues = getCotisation.add(irsaDue);

        return grosseSalary.subtract(totalRetenues);
    }

    public Payroll savePayroll(Payroll payroll){
        return payrollRepository.save(payroll);
    }

}
