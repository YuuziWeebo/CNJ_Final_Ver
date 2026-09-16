package com.glucoze.thesismanagement.council.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class ScheduleForm {

    @NotNull
    private Long thesisId;

    @NotNull
    private Long councilId;

    @NotBlank
    @Size(max = 150)
    private String room;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    public Long getThesisId() { return thesisId; }
    public void setThesisId(Long thesisId) { this.thesisId = thesisId; }
    public Long getCouncilId() { return councilId; }
    public void setCouncilId(Long councilId) { this.councilId = councilId; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
