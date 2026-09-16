package com.glucoze.thesismanagement.common.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.council.controller.CouncilController;
import com.glucoze.thesismanagement.council.dto.CouncilForm;
import com.glucoze.thesismanagement.council.service.CouncilSchedulingService;
import com.glucoze.thesismanagement.grading.controller.LecturerGradingController;
import com.glucoze.thesismanagement.grading.service.GradingService;
import com.glucoze.thesismanagement.grading.service.GradingQueryService;
import com.glucoze.thesismanagement.thesis.controller.LecturerRegistrationController;
import com.glucoze.thesismanagement.thesis.controller.LecturerReportController;
import com.glucoze.thesismanagement.thesis.controller.LecturerThesisController;
import com.glucoze.thesismanagement.thesis.dto.ReviewForm;
import com.glucoze.thesismanagement.thesis.dto.ThesisForm;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
import com.glucoze.thesismanagement.user.controller.AdminUserController;
import com.glucoze.thesismanagement.user.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class MutationErrorFlowTest {

    private static final String DOMAIN_MESSAGE = "Thao tác không hợp lệ";

    @Test
    void gradingConflictUsesPrgFlashInsteadOfGenericErrorPage() {
        GradingService service = mock(GradingService.class);
        doThrow(new DomainRuleViolationException(DOMAIN_MESSAGE)).when(service).publishResult("lecturer", 1L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = new LecturerGradingController(service, mock(GradingQueryService.class))
                .publish(1L, user("lecturer"), redirect);

        assertErrorRedirect(view, "redirect:/lecturer/grading/{id}", redirect);
    }

    @Test
    void gradingControllerUsesQueryServiceForReadAndCommandServiceForMutation() {
        GradingService commands = mock(GradingService.class);
        GradingQueryService queries = mock(GradingQueryService.class);
        when(queries.listLecturerSchedules("lecturer")).thenReturn(java.util.List.of());
        LecturerGradingController controller = new LecturerGradingController(commands, queries);
        ConcurrentModel model = new ConcurrentModel();

        String listView = controller.list(user("lecturer"), model);
        String publishView = controller.publish(
                1L, user("lecturer"), new RedirectAttributesModelMap());

        assertThat(listView).isEqualTo("lecturer/grading");
        assertThat(publishView).isEqualTo("redirect:/lecturer/grading/{id}");
        verify(queries).listLecturerSchedules("lecturer");
        verify(commands).publishResult("lecturer", 1L);
    }

    @Test
    void registrationTransitionConflictUsesPrgFlash() {
        ThesisManagementService service = mock(ThesisManagementService.class);
        doThrow(new DomainRuleViolationException(DOMAIN_MESSAGE)).when(service)
                .approveRegistration("lecturer", 1L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = new LecturerRegistrationController(service, mock(ThesisQueryService.class)).approve(1L, user("lecturer"), redirect);

        assertErrorRedirect(view, "redirect:/lecturer/registrations", redirect);
    }

    @Test
    void thesisControllerUsesQueryServiceForReadAndCommandServiceForMutation() {
        ThesisManagementService commands = mock(ThesisManagementService.class);
        ThesisQueryService queries = mock(ThesisQueryService.class);
        when(queries.listLecturerTheses("lecturer")).thenReturn(java.util.List.of());
        LecturerThesisController controller = new LecturerThesisController(commands, queries);

        String listView = controller.list(user("lecturer"), new ConcurrentModel());
        String deleteView = controller.delete(
                user("lecturer"), 1L, new RedirectAttributesModelMap());

        assertThat(listView).isEqualTo("lecturer/theses");
        assertThat(deleteView).isEqualTo("redirect:/lecturer/theses");
        verify(queries).listLecturerTheses("lecturer");
        verify(commands).deleteThesis("lecturer", 1L);
    }

    @Test
    void reportTransitionConflictUsesPrgFlash() {
        ThesisManagementService service = mock(ThesisManagementService.class);
        ReviewForm form = new ReviewForm();
        doThrow(new DomainRuleViolationException(DOMAIN_MESSAGE)).when(service)
                .approveReport("lecturer", 1L, form);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = new LecturerReportController(service, mock(ThesisQueryService.class)).approve(1L, user("lecturer"), form,
                new BeanPropertyBindingResult(form, "reviewForm"), redirect);

        assertErrorRedirect(view, "redirect:/lecturer/reports", redirect);
    }

    @Test
    void thesisDeleteConflictUsesPrgFlash() {
        ThesisManagementService service = mock(ThesisManagementService.class);
        doThrow(new DomainRuleViolationException(DOMAIN_MESSAGE)).when(service).deleteThesis("lecturer", 1L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = new LecturerThesisController(service, mock(ThesisQueryService.class)).delete(user("lecturer"), 1L, redirect);

        assertErrorRedirect(view, "redirect:/lecturer/theses", redirect);
    }

    @Test
    void councilDeleteConflictUsesPrgFlash() {
        CouncilSchedulingService service = mock(CouncilSchedulingService.class);
        doThrow(new DomainRuleViolationException(DOMAIN_MESSAGE)).when(service).deleteCouncil(1L);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = new CouncilController(service).delete(1L, redirect);

        assertErrorRedirect(view, "redirect:/admin/councils", redirect);
    }

    @Test
    void accountDeleteConflictUsesPrgFlash() {
        UserManagementService service = mock(UserManagementService.class);
        doThrow(new DomainRuleViolationException(DOMAIN_MESSAGE)).when(service).deleteAccount(1L, "admin");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = new AdminUserController(service).delete(1L, user("admin"), redirect);

        assertErrorRedirect(view, "redirect:/admin/users", redirect);
    }

    @Test
    void invalidThesisEditKeepsEditIdentityWhenRenderingForm() {
        ThesisManagementService service = mock(ThesisManagementService.class);
        ThesisForm form = new ThesisForm();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "thesisForm");
        errors.rejectValue("title", "required");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = new LecturerThesisController(service, mock(ThesisQueryService.class)).update(
                user("lecturer"), 7L, form, errors, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("lecturer/thesis-form");
        assertThat(model).containsEntry("thesisId", 7L);
    }

    @Test
    void invalidCouncilEditKeepsEditIdentityWhenRenderingForm() {
        CouncilSchedulingService service = mock(CouncilSchedulingService.class);
        CouncilForm form = new CouncilForm();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "councilForm");
        errors.rejectValue("name", "required");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = new CouncilController(service).update(
                8L, form, errors, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/council-form");
        assertThat(model).containsEntry("councilId", 8L);
    }

    private UserDetails user(String username) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    private void assertErrorRedirect(String actualView, String expectedView, RedirectAttributesModelMap redirect) {
        assertThat(actualView).isEqualTo(expectedView);
        assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo(DOMAIN_MESSAGE);
        assertThat(redirect.getFlashAttributes().containsKey("successMessage")).isFalse();
    }
}
