package com.RH.gestion.service;

import com.RH.gestion.model.Employee;
import com.RH.gestion.model.LeaveRequest;
import com.RH.gestion.repository.EmployeeRepository;
import com.RH.gestion.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    public BigDecimal calculateLeaveBalance(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()->new EntityNotFoundException("Employé non trouvé"));
        LocalDate date = employee.getDateEmbauche();
        LocalDate today = LocalDate.now();
        long mounths= ChronoUnit.MONTHS.between(date,today);

        BigDecimal acquiredDays= BigDecimal.valueOf(mounths)
                .multiply(new BigDecimal("2.5"));

        //initialisation solde de congé
        BigDecimal leaveConge = acquiredDays;

        List<LeaveRequest> approuvedRequest = leaveRequestRepository.findByEmployeeIdAndStatus(employeeId,"APPROUVE");

        BigDecimal totalTakenDays= approuvedRequest.stream()
                .map(LeaveRequest::getNombreJour)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        BigDecimal finalBalance = acquiredDays.subtract(totalTakenDays);
        return finalBalance;
    }

    // Dans PayrollService.java (ou LeaveService.java, où la fonction se trouve)

    public BigDecimal calculateAbsenceDeduction(Long employeeId, LocalDate date) {

        // ... (Code existant pour trouver l'employé et le Taux Journalier)
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()-> new EntityNotFoundException("Employee not found"));

        final BigDecimal MONTHLY_DAYS = new BigDecimal("30.4167");
        BigDecimal tauxJournalier = employee.getSalaireBase().divide(MONTHLY_DAYS, 4, RoundingMode.HALF_UP);

        // 1. Définir la période de recherche
        LocalDate startDate = date.withDayOfMonth(1);
        LocalDate endDate = date.withDayOfMonth(date.lengthOfMonth());

        // 2. Récupérer les jours SANS SOLDE
        List<LeaveRequest> sansSoldeRequests = leaveRequestRepository
                .findByEmpoyeeIdAndStatusAndDateDebutBetween(employeeId,"SANS_SOLDE",startDate,endDate);

        // 3. Calculer le total des jours d'absence (utilisation du Stream API)
        BigDecimal totalAbsenceDays = sansSoldeRequests.stream()
                .map(LeaveRequest::getNombreJour)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        // 4. Calcul de la retenue finale
        BigDecimal deductionAmount = totalAbsenceDays.multiply(tauxJournalier);

        return deductionAmount;
    }
}
