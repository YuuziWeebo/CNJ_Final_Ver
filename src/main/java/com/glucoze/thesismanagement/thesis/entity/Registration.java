package com.glucoze.thesismanagement.thesis.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.converter.RegistrationStatusConverter;
import com.glucoze.thesismanagement.user.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "(trang_thai IN ('CHO_DUYET', 'DA_DUYET') AND active_registration_key = true) "
    + "OR (trang_thai IN ('TU_CHOI', 'HOAN_THANH', 'HUY') AND active_registration_key IS NULL)")
@Table(name = "dang_ky", uniqueConstraints = {
    @UniqueConstraint(name = "uk_dang_ky_active_de_tai", columnNames = {"de_tai_id", "active_registration_key"}),
    @UniqueConstraint(name = "uk_dang_ky_active_sinh_vien", columnNames = {"sinh_vien_id", "active_registration_key"})
})
public class Registration extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "de_tai_id", nullable = false)
    private Thesis thesis;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student student;

    @jakarta.persistence.Convert(converter = RegistrationStatusConverter.class)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    /**
     * Active registrations use TRUE; inactive historical registrations use NULL.
     * MySQL permits multiple NULL values in a unique constraint, while TRUE is
     * limited to one row per student and per thesis topic.
     */
    @Column(name = "active_registration_key")
    private Boolean activeRegistrationKey = Boolean.TRUE;

    protected Registration() {
    }

    public Registration(Thesis thesis, Student student) {
        this.thesis = thesis;
        this.student = student;
    }

    public Thesis getThesis() {
        return thesis;
    }

    public Student getStudent() {
        return student;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(activeRegistrationKey);
    }

    public void cancelForChange() {
        if (!isActive()) {
            throw new IllegalStateException("Đăng ký không còn hoạt động");
        }
        if (status != RegistrationStatus.PENDING && status != RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Đăng ký không thể đổi đề tài");
        }
        status = RegistrationStatus.CANCELLED;
        activeRegistrationKey = null;
    }

    public void transitionTo(RegistrationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Chuyển trạng thái đăng ký không hợp lệ: " + status + " -> " + target);
        }
        status = target;
        activeRegistrationKey = target == RegistrationStatus.PENDING
                || target == RegistrationStatus.APPROVED ? Boolean.TRUE : null;
    }
}
