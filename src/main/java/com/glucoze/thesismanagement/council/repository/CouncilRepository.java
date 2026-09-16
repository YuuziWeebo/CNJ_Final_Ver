package com.glucoze.thesismanagement.council.repository;

import com.glucoze.thesismanagement.council.entity.Council;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.glucoze.thesismanagement.export.dto.CouncilExportRow;
import org.springframework.data.domain.Pageable;

public interface CouncilRepository extends JpaRepository<Council, Long> {

    List<Council> findByAcademicYearAndSemester(String academicYear, String semester);

    /**
     * Scheduling is a low-volume administrative operation. Locking one stable
     * council row serializes conflict-check-and-insert across application
     * instances, including the case where no schedule exists for a room yet.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Council> findFirstByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Council h where h.id = :id")
    Optional<Council> findByIdForUpdate(@Param("id") Long id);

    @Query("select new com.glucoze.thesismanagement.export.dto.CouncilExportRow(h.name, h.academicYear, h.semester, count(tv.id)) "
            + "from Council h left join CouncilMember tv on tv.council = h "
            + "group by h.id, h.name, h.academicYear, h.semester order by h.academicYear desc, h.semester, h.name")
    List<CouncilExportRow> findExportRows(Pageable pageable);
}
