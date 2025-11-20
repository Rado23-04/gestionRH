package com.RH.gestion.repository;

import com.RH.gestion.model.LeaveRequest;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;


public interface LeaveRequestRepository extends JpaRepository<LeaveRequest,Long> {
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, String status);
    List<LeaveRequest> findByEmpoyeeIdAndStatusAndDateDebutBetween(Long employeeId, String status,LocalDate startDate,
                                                                   LocalDate endDate);
}
