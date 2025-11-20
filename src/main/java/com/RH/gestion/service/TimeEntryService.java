package com.RH.gestion.service;

import com.RH.gestion.model.Employee;
import com.RH.gestion.model.TimeEntry;
import com.RH.gestion.repository.EmployeeRepository;
import com.RH.gestion.repository.TimeEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeEntryService {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;
    public BigDecimal calculateMonthlyOvertime(Long employeeId, LocalDate startDate, LocalDate endDate) {
        //Trouver l'employee
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()->new EntityNotFoundException("Employee not found"));

        BigDecimal hourlyRate = employeeService.calculateHourlyRate(employeeId);

        List<TimeEntry> entries = timeEntryRepository.findByEmployeeAndDateJourBetween(employeeId,startDate,endDate);

        // L'objet WeekFields qui définit les règles locales de la semaine (lundi=début, etc.)
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        // Regroupement par numéro de semaine (Clé : Integer, Valeur : List<TimeEntry>)
        Map<Integer, List<TimeEntry>> entriesByWeek = entries.stream()
                .collect(Collectors.groupingBy(entry ->
                        entry.getDateJour().get(weekFields.weekOfWeekBasedYear())
                ));

        BigDecimal totalOverTimeAmount = BigDecimal.ZERO;
        for(List<TimeEntry> weekEntries: entriesByWeek.values()){
            BigDecimal weeklyAmount = calculateWeeklyOvertimeAmount(weekEntries,hourlyRate);
            totalOverTimeAmount = totalOverTimeAmount.add(weeklyAmount);
        }

         return totalOverTimeAmount;
    }

    private BigDecimal calculateWeeklyOvertimeAmount(List<TimeEntry> weekEntries, BigDecimal hourlyRate) {
        BigDecimal totalHours = weekEntries.stream()
                .map(TimeEntry::getDureeTravaillee)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        final BigDecimal LEGAL_LIMIT = new BigDecimal("40.0");

        if(totalHours == null || totalHours.compareTo(LEGAL_LIMIT)<0){
            return BigDecimal.ZERO;
        }

        BigDecimal overTimeHours = totalHours.subtract(LEGAL_LIMIT);

        BigDecimal totalOverTimeHours = BigDecimal.ZERO;

        final BigDecimal FIRST_TIER_LIMIT = new BigDecimal("8");
        final BigDecimal FIRST_TIER_RATE = new BigDecimal("1.3");
        final BigDecimal SECOND_TIER_LIMIT = new BigDecimal("12");
        final BigDecimal SECOND_TIER_RATE = new BigDecimal("1.5");

        BigDecimal firstTiersHours = overTimeHours.min(FIRST_TIER_LIMIT);

        totalOverTimeHours = totalOverTimeHours.add(firstTiersHours)
                .multiply(hourlyRate)
                .multiply(FIRST_TIER_RATE);

        overTimeHours = overTimeHours.subtract(firstTiersHours);
        if(overTimeHours.compareTo(BigDecimal.ZERO)>0){
            BigDecimal secondTiersHours = overTimeHours.min(SECOND_TIER_LIMIT);

            totalOverTimeHours = totalOverTimeHours.add(secondTiersHours)
                    .multiply(hourlyRate)
                    .multiply(SECOND_TIER_RATE);
        }
        return totalOverTimeHours;
    }
}
