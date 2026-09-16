package com.glucoze.thesismanagement.thesis.repository;

import com.glucoze.thesismanagement.thesis.entity.Thesis;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.glucoze.thesismanagement.export.dto.ThesisExportRow;
import org.springframework.data.domain.Pageable;

public interface ThesisRepository extends JpaRepository<Thesis, Long> {

    List<Thesis> findByAcademicYearAndSemester(String academicYear, String semester);

    List<Thesis> findBySupervisorUserAccountUsername(String username);

    Optional<Thesis> findByIdAndSupervisorUserAccountUsername(Long id, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Thesis d where d.id = :id")
    Optional<Thesis> findByIdForUpdate(@Param("id") Long id);

    boolean existsBySupervisorId(Long lecturerId);

    long countBySupervisorUserAccountUsername(String username);

    @Query("select new com.glucoze.thesismanagement.export.dto.ThesisExportRow(d.title, d.supervisor.fullName, "
            + "d.academicYear, d.semester) from Thesis d order by d.academicYear desc, d.semester, d.title")
    List<ThesisExportRow> findExportRows(Pageable pageable);
}
