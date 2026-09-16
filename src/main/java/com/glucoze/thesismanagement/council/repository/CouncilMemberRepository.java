package com.glucoze.thesismanagement.council.repository;

import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {

    List<CouncilMember> findByCouncilId(Long councilId);

    @EntityGraph(attributePaths = {"council", "lecturer"})
    List<CouncilMember> findByCouncilIdIn(Collection<Long> councilIds);

    @Query("select tv.council.id as councilId, count(tv.id) as memberCount "
            + "from CouncilMember tv where tv.council.id in :councilIds group by tv.council.id")
    List<CouncilMemberCount> countMembersByCouncilIds(@Param("councilIds") Collection<Long> councilIds);

    boolean existsByCouncilIdAndLecturerId(Long councilId, Long lecturerId);

    boolean existsByLecturerId(Long lecturerId);

    boolean existsByCouncilId(Long councilId);

    boolean existsByCouncilIdAndRole(Long councilId, CouncilRole role);

    interface CouncilMemberCount {
        Long getCouncilId();

        long getMemberCount();
    }
}
