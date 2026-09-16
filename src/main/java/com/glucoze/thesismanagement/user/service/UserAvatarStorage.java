package com.glucoze.thesismanagement.user.service;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserAvatarStorage {

    static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(2);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private final long maxFileSize;

    public UserAvatarStorage(@Value("${app.upload.avatar-max-size:2MB}") DataSize maxFileSize) {
        this.maxFileSize = maxFileSize.toBytes();
    }

    UserAvatarStorage() {
        this(DEFAULT_MAX_FILE_SIZE);
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > maxFileSize) {
            throw new DomainRuleViolationException(
                    "Ảnh đại diện không được vượt quá " + displaySize(maxFileSize));
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new DomainRuleViolationException("Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP");
        }
        try {
            byte[] content = file.getBytes();
            if (!matchesContentType(file.getContentType(), content)) {
                throw new DomainRuleViolationException("Nội dung ảnh không khớp định dạng JPG, PNG hoặc WEBP");
            }
        } catch (java.io.IOException exception) {
            throw new DomainRuleViolationException("Không thể đọc ảnh đại diện", exception);
        }
    }

    private boolean matchesContentType(String contentType, byte[] content) {
        return switch (contentType) {
            case "image/jpeg" -> content.length >= 3
                    && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF;
            case "image/png" -> startsWith(content, PNG_SIGNATURE);
            case "image/webp" -> content.length >= 12
                    && asciiEquals(content, 0, "RIFF") && asciiEquals(content, 8, "WEBP");
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean asciiEquals(byte[] content, int offset, String expected) {
        for (int index = 0; index < expected.length(); index++) {
            if (content[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private String displaySize(long bytes) {
        if (bytes % (1024 * 1024) == 0) {
            return bytes / (1024 * 1024) + " MB";
        }
        return bytes >= 1024 ? bytes / 1024 + " KB" : bytes + " bytes";
    }
}
