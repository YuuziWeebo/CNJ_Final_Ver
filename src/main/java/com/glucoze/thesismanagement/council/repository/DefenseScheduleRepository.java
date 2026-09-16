package com.glucoze.thesismanagement.council.repository;

import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import com.glucoze.thesismanagement.export.dto.ScheduleExportRow;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.council.dto.CalendarScheduleRow;

public interface DefenseScheduleRepository extends JpaRepository<DefenseSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from DefenseSchedule l where l.id = :id")
    Optional<DefenseSchedule> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCouncilId(Long councilId);

    boolean existsByThesisId(Long thesisId);

    Optional<DefenseSchedule> findByThesisId(Long thesisId);

    @Query("select l from DefenseSchedule l join fetch l.thesis join fetch l.council "
            + "where l.startTime >= :now order by l.startTime")
    List<DefenseSchedule> findUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("select distinct l from DefenseSchedule l join fetch l.thesis d join fetch l.council h "
            + "where l.startTime >= :now and (d.supervisor.userAccount.username = :username or exists ("
            + "select tv.id from CouncilMember tv where tv.council = h "
            + "and tv.lecturer.userAccount.username = :username)) order by l.startTime")
    List<DefenseSchedule> findUpcomingByLecturerAccess(@Param("username") String username,
                                                       @Param("now") LocalDateTime now, Pageable pageable);

    @Query("select count(l.id) from DefenseSchedule l where l.thesis.supervisor.userAccount.username = :username "
            + "and not exists (select d.id from Grade d where d.defenseSchedule = l "
            + "and d.lecturer = l.thesis.supervisor and d.scoreSource = com.glucoze.thesismanagement.common.enums.ScoreSource.SUPERVISOR)")
    long countIncompleteSupervisorTasks(@Param("username") String username);

    @Query("select count(distinct l.id) from DefenseSchedule l join CouncilMember tv on tv.council = l.council "
            + "where tv.lecturer.userAccount.username = :username and not exists (select d.id from Grade d "
            + "where d.defenseSchedule = l and d.lecturer = tv.lecturer "
            + "and d.scoreSource = com.glucoze.thesismanagement.common.enums.ScoreSource.COUNCIL)")
    long countIncompleteCouncilTasks(@Param("username") String username);

    List<DefenseSchedule> findByThesisSupervisorUserAccountUsername(String username);

    List<DefenseSchedule> findByThesisIdIn(Collection<Long> thesisIds);

    @Query("select l from DefenseSchedule l join CouncilMember tv on tv.council = l.council "
            + "where tv.lecturer.userAccount.username = :username")
    List<DefenseSchedule> findByCouncilMemberUsername(@Param("username") String username);

    @Query("select distinct l from DefenseSchedule l where l.id = :id and ("
            + "l.thesis.supervisor.userAccount.username = :username or exists ("
            + "select tv.id from CouncilMember tv where tv.council = l.council "
            + "and tv.lecturer.userAccount.username = :username))")
    Optional<DefenseSchedule> findByIdAndLecturerAccess(@Param("id") Long id,
                                                  @Param("username") String username);

    @Query("select count(distinct l.id) from DefenseSchedule l where "
            + "l.thesis.supervisor.userAccount.username = :username or exists ("
            + "select tv.id from CouncilMember tv where tv.council = l.council "
            + "and tv.lecturer.userAccount.username = :username)")
    long countByLecturerAccess(@Param("username") String username);

    @Query("select l from DefenseSchedule l where l.council.id = :councilId "
            + "and l.startTime < :endTime and l.endTime > :startTime")
    List<DefenseSchedule> findCouncilConflicts(@Param("councilId") Long councilId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Query("select l from DefenseSchedule l join CouncilMember tv on tv.council = l.council "
            + "where tv.lecturer.id = :lecturerId and l.startTime < :endTime and l.endTime > :startTime")
    List<DefenseSchedule> findLecturerConflicts(@Param("lecturerId") Long lecturerId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Query("select l from DefenseSchedule l where l.room = :room "
            + "and l.startTime < :endTime and l.endTime > :startTime")
    List<DefenseSchedule> findRoomConflicts(@Param("room") String room,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    @Query("select new com.glucoze.thesismanagement.export.dto.ScheduleExportRow(l.thesis.title, d.student.fullName, "
            + "l.council.name, l.room, l.startTime, l.endTime, l.thesis.academicYear, l.thesis.semester) "
            + "from DefenseSchedule l join Registration d on d.thesis = l.thesis and d.status = :approved "
            + "where (:academicYear is null or l.thesis.academicYear = :academicYear) "
            + "and (:semester is null or l.thesis.semester = :semester) order by l.startTime")
    List<ScheduleExportRow> findExportRows(@Param("academicYear") String academicYear,
                                           @Param("semester") String semester,
                                           @Param("approved") RegistrationStatus approved, Pageable pageable);

    @Query("select new com.glucoze.thesismanagement.council.dto.CalendarScheduleRow(l.id, l.thesis.title, "
            + "d.student.fullName, l.thesis.supervisor.fullName, l.council.name, l.room, l.startTime, l.endTime) "
            + "from DefenseSchedule l join Registration d on d.thesis = l.thesis and d.status = "
            + "com.glucoze.thesismanagement.common.enums.RegistrationStatus.APPROVED "
            + "where l.startTime < :end and l.endTime > :start order by l.startTime")
    List<CalendarScheduleRow> findCalendarRows(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("select distinct new com.glucoze.thesismanagement.council.dto.CalendarScheduleRow(l.id, l.thesis.title, "
            + "d.student.fullName, l.thesis.supervisor.fullName, l.council.name, l.room, l.startTime, l.endTime) "
            + "from DefenseSchedule l join Registration d on d.thesis = l.thesis and d.status = "
            + "com.glucoze.thesismanagement.common.enums.RegistrationStatus.APPROVED "
            + "where l.startTime < :end and l.endTime > :start and ("
            + "l.thesis.supervisor.userAccount.username = :username or exists (select tv.id from CouncilMember tv "
            + "where tv.council = l.council and tv.lecturer.userAccount.username = :username)) order by l.startTime")
    List<CalendarScheduleRow> findLecturerCalendarRows(@Param("username") String username,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("select distinct new com.glucoze.thesismanagement.council.dto.CalendarScheduleRow(l.id, l.thesis.title, "
            + "d.student.fullName, l.thesis.supervisor.fullName, l.council.name, l.room, l.startTime, l.endTime) "
            + "from DefenseSchedule l join Registration d on d.thesis = l.thesis "
            + "where d.student.userAccount.username = :username and d.status = :status "
            + "and l.startTime < :end and l.endTime > :start order by l.startTime")
    List<CalendarScheduleRow> findStudentCalendarRows(@Param("username") String username,
                                                      @Param("status") RegistrationStatus status,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}
