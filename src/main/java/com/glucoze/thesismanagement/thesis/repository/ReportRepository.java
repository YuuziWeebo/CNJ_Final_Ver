package com.glucoze.thesismanagement.thesis.repository;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.thesis.entity.Report;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByRegistrationId(Long registrationId);

    List<Report> findByRegistrationIdIn(List<Long> registrationIds);

    List<Report> findByRegistrationStudentUserAccountUsername(String username);

    List<Report> findByRegistrationThesisSupervisorUserAccountUsername(String username);

    long countByRegistrationThesisSupervisorUserAccountUsername(String username);

    long countByStatus(ReportStatus status);

    long countByRegistrationThesisSupervisorUserAccountUsernameAndStatus(String username, ReportStatus status);

    Optional<Report> findByIdAndRegistrationThesisSupervisorUserAccountUsername(Long id, String username);

    Optional<Report> findByIdAndRegistrationStudentUserAccountUsername(Long id, String username);
}
