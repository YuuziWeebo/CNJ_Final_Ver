package com.glucoze.thesismanagement.common.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.util.unit.DataSize;

@ControllerAdvice
public class MvcExceptionHandler {

    private final DataSize reportMaxSize;
    private final DataSize avatarMaxSize;

    public MvcExceptionHandler(@Value("${app.upload.report-max-size:10MB}") DataSize reportMaxSize,
                               @Value("${app.upload.avatar-max-size:2MB}") DataSize avatarMaxSize) {
        this.reportMaxSize = reportMaxSize;
        this.avatarMaxSize = avatarMaxSize;
    }

    MvcExceptionHandler() {
        this(DataSize.ofMegabytes(10), DataSize.ofMegabytes(2));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException exception, Model model) {
        model.addAttribute("errorTitle", "Không tìm thấy dữ liệu");
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/domain-error";
    }

    @ExceptionHandler({DomainRuleViolationException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDomainError(RuntimeException exception, Model model) {
        model.addAttribute("errorTitle", "Không thể thực hiện thao tác");
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/domain-error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataConflict(Model model) {
        model.addAttribute("errorTitle", "Dữ liệu bị xung đột");
        model.addAttribute("errorMessage", "Dữ liệu đã thay đổi hoặc đang được sử dụng. Vui lòng tải lại và thử lại.");
        return "error/domain-error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleUploadTooLarge(Model model) {
        model.addAttribute("errorTitle", "Tệp tải lên quá lớn");
        model.addAttribute("errorMessage",
                "Tệp vượt quá giới hạn cho phép. Báo cáo tối đa " + displaySize(reportMaxSize)
                        + "; ảnh đại diện tối đa " + displaySize(avatarMaxSize) + ".");
        return "error/domain-error";
    }

    private String displaySize(DataSize size) {
        return size.toMegabytes() + " MB";
    }
}
