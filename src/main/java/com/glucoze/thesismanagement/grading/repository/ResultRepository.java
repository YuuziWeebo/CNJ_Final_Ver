package com.glucoze.thesismanagement.grading.repository;

import com.glucoze.thesismanagement.grading.entity.Result;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.export.dto.ResultExportRow;
import org.springframework.data.domain.Pageable;

public interface ResultRepository extends JpaRepository<Result, Long> {

    Optional<Result> findByDefenseScheduleId(Long defenseScheduleId);

    Optional<Result> findByDefenseScheduleIdAndPublishedTrue(Long defenseScheduleId);

    long countByPublishedTrue();

    @EntityGraph(attributePaths = "defenseSchedule")
    List<Result> findByDefenseScheduleIdIn(Collection<Long> defenseScheduleIds);

    @Query("select count(distinct k.id) from Result k, Registration d "
            + "where k.defenseSchedule.thesis = d.thesis and k.published = true "
            + "and d.student.userAccount.username = :username and d.status = :status")
    long countPublishedByStudent(@Param("username") String username,
                                 @Param("status") RegistrationStatus status);

    @Query("select new com.glucoze.thesismanagement.export.dto.ResultExportRow(d.student.fullName, "
            + "k.defenseSchedule.thesis.title, k.defenseSchedule.thesis.supervisor.fullName, "
            + "k.defenseSchedule.council.name, k.finalScore, k.defenseSchedule.startTime, "
            + "k.defenseSchedule.thesis.academicYear, k.defenseSchedule.thesis.semester) "
            + "from Result k join Registration d on d.thesis = k.defenseSchedule.thesis and d.status = :approved "
            + "where k.published = true and (:academicYear is null or k.defenseSchedule.thesis.academicYear = :academicYear) "
            + "and (:semester is null or k.defenseSchedule.thesis.semester = :semester) "
            + "order by k.defenseSchedule.startTime, d.student.studentCode")
    List<ResultExportRow> findPublishedExportRows(@Param("academicYear") String academicYear,
                                                  @Param("semester") String semester,
                                                  @Param("approved") RegistrationStatus approved,
                                                  Pageable pageable);
}
