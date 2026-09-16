package com.glucoze.thesismanagement.council.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_bao_ve", indexes = {
        @Index(name = "idx_lich_bao_ve_range", columnList = "bat_dau,ket_thuc")
})
public class DefenseSchedule extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "de_tai_id", nullable = false)
    private Thesis thesis;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hoi_dong_id", nullable = false)
    private Council council;

    @Column(name = "phong", nullable = false, length = 150)
    private String room;

    @Column(name = "bat_dau", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "ket_thuc", nullable = false)
    private LocalDateTime endTime;

    protected DefenseSchedule() {
    }

    public DefenseSchedule(Thesis thesis, Council council, String room, LocalDateTime startTime, LocalDateTime endTime) {
        this.thesis = thesis;
        this.council = council;
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Thesis getThesis() {
        return thesis;
    }

    public Council getCouncil() {
        return council;
    }

    public String getRoom() {
        return room;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
