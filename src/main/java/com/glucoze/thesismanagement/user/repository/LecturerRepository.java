package com.glucoze.thesismanagement.user.repository;

import com.glucoze.thesismanagement.user.entity.Lecturer;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.glucoze.thesismanagement.export.dto.LecturerExportRow;
import org.springframework.data.domain.Pageable;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    Optional<Lecturer> findByUserAccountUsername(String username);

    @EntityGraph(attributePaths = "userAccount")
    List<Lecturer> findByUserAccountIdIn(Collection<Long> accountIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Lecturer g where g.id in :ids order by g.id")
    List<Lecturer> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    boolean existsByLecturerCode(String lecturerCode);

    @Query("select new com.glucoze.thesismanagement.export.dto.LecturerExportRow(g.lecturerCode, g.fullName, g.department) "
            + "from Lecturer g order by g.lecturerCode")
    List<LecturerExportRow> findExportRows(Pageable pageable);
}
