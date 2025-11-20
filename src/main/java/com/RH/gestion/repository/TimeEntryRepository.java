package com.RH.gestion.repository;

import com.RH.gestion.model.Employee;
import com.RH.gestion.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry,Long> {
    List<TimeEntry> findByEmployeeAndDateJourBetween(Long employee,LocalDate startDate, LocalDate endDate);
}
