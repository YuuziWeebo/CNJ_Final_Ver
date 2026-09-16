package com.glucoze.thesismanagement.user.repository;

import com.glucoze.thesismanagement.user.entity.Student;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.glucoze.thesismanagement.export.dto.StudentExportRow;
import org.springframework.data.domain.Pageable;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserAccountUsername(String username);

    @EntityGraph(attributePaths = "userAccount")
    List<Student> findByUserAccountIdIn(Collection<Long> accountIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s where s.userAccount.username = :username")
    Optional<Student> findByUsernameForUpdate(@Param("username") String username);

    boolean existsByStudentCode(String studentCode);

    @Query("select new com.glucoze.thesismanagement.export.dto.StudentExportRow(s.studentCode, s.fullName, s.className) "
            + "from Student s order by s.studentCode")
    List<StudentExportRow> findExportRows(Pageable pageable);
}
