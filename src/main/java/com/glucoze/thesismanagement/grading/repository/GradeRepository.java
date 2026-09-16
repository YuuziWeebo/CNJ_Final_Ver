package com.glucoze.thesismanagement.grading.repository;

import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.grading.entity.Grade;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByDefenseScheduleId(Long defenseScheduleId);

    @EntityGraph(attributePaths = {"defenseSchedule", "lecturer"})
    List<Grade> findByDefenseScheduleIdIn(Collection<Long> defenseScheduleIds);

    List<Grade> findAllByDefenseScheduleIdAndScoreSource(Long defenseScheduleId, ScoreSource scoreSource);

    Optional<Grade> findByDefenseScheduleIdAndLecturerIdAndScoreSource(Long defenseScheduleId, Long lecturerId,
                                                                    ScoreSource scoreSource);

    boolean existsByLecturerId(Long lecturerId);
}
