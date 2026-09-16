package com.glucoze.thesismanagement.thesis.repository;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByStudentIdAndActiveRegistrationKeyTrue(Long studentId);

    Optional<Registration> findByStudentUserAccountUsernameAndActiveRegistrationKeyTrue(String username);

    Optional<Registration> findFirstByStudentUserAccountUsernameOrderByCreatedAtDesc(String username);

    boolean existsByThesisIdAndActiveRegistrationKeyTrue(Long thesisId);

    boolean existsByStudentId(Long studentId);

    List<Registration> findByStudentUserAccountUsername(String username);

    List<Registration> findByThesisSupervisorUserAccountUsername(String username);

    Optional<Registration> findByIdAndStudentUserAccountUsername(Long id, String username);

    Optional<Registration> findByIdAndThesisSupervisorUserAccountUsername(Long id, String username);

    boolean existsByThesisId(Long thesisId);

    boolean existsByThesisIdAndStatus(Long thesisId, RegistrationStatus status);

    Optional<Registration> findFirstByThesisIdAndStatus(Long thesisId, RegistrationStatus status);

    List<Registration> findByStudentUserAccountUsernameAndStatus(String username, RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    long countByThesisSupervisorUserAccountUsernameAndStatus(String username, RegistrationStatus status);

    Optional<Registration> findByThesisIdAndStudentUserAccountUsernameAndStatus(Long thesisId, String username,
                                                                              RegistrationStatus status);

}
