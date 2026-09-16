package com.glucoze.thesismanagement.thesis.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import org.junit.jupiter.api.Test;

class ThesisWorkflowStateTest {

    @Test
    void registrationApprovalKeepsRegistrationActive() {
        Registration registration = new Registration(null, null);

        registration.transitionTo(RegistrationStatus.APPROVED);

        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(registration.isActive()).isTrue();
    }

    @Test
    void rejectedRegistrationReleasesActiveSlot() {
        Registration registration = new Registration(null, null);

        registration.transitionTo(RegistrationStatus.REJECTED);

        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
        assertThat(registration.isActive()).isFalse();
    }

    @Test
    void completedRegistrationCannotBeApprovedAgain() {
        Registration registration = new Registration(null, null);
        registration.transitionTo(RegistrationStatus.APPROVED);
        registration.transitionTo(RegistrationStatus.COMPLETED);

        assertThatThrownBy(() -> registration.transitionTo(RegistrationStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class);
    }

            @Test
            void terminalRegistrationsRejectApprovalAndRejection() {
            Registration rejected = new Registration(null, null);
            rejected.transitionTo(RegistrationStatus.REJECTED);
            Registration cancelled = new Registration(null, null);
            cancelled.transitionTo(RegistrationStatus.CANCELLED);

            assertThatThrownBy(() -> rejected.transitionTo(RegistrationStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> rejected.transitionTo(RegistrationStatus.REJECTED))
                .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> cancelled.transitionTo(RegistrationStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class);
            }

    @Test
    void reportSupportsSubmissionRevisionAndApproval() {
        Report report = new Report(null);

        report.transitionTo(ReportStatus.SUBMITTED);
        report.transitionTo(ReportStatus.REVISION_REQUIRED);
        report.transitionTo(ReportStatus.SUBMITTED);
        report.transitionTo(ReportStatus.APPROVED);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
    }

    @Test
    void approvedReportCannotBeSubmittedAgain() {
        Report report = new Report(null);
        report.transitionTo(ReportStatus.SUBMITTED);
        report.transitionTo(ReportStatus.APPROVED);

        assertThatThrownBy(() -> report.transitionTo(ReportStatus.SUBMITTED))
                .isInstanceOf(IllegalStateException.class);
    }
}