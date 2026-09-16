package com.glucoze.thesismanagement.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class UserAvatarStorageTest {

    private final UserAvatarStorage storage = new UserAvatarStorage();

    @Test
    void acceptsMatchingImageSignatures() {
        assertThatCode(() -> storage.validate(file("image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}))).doesNotThrowAnyException();
        assertThatCode(() -> storage.validate(file("image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})))
                .doesNotThrowAnyException();
        assertThatCode(() -> storage.validate(file("image/webp",
                new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'})))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsSpoofedImageContentType() {
        assertThatThrownBy(() -> storage.validate(file("image/png", new byte[] {'n', 'o', 't'})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không khớp");
    }

    @Test
    void avatarSizeBoundaryRemainsIndependentFromReportLimit() {
        UserAvatarStorage oneKilobyteStorage = new UserAvatarStorage(DataSize.ofKilobytes(1));
        byte[] oversized = new byte[1025];
        oversized[0] = (byte) 0xFF;
        oversized[1] = (byte) 0xD8;
        oversized[2] = (byte) 0xFF;

        assertThatThrownBy(() -> oneKilobyteStorage.validate(file("image/jpeg", oversized)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 KB");
    }

    @Test
    void defaultAvatarLimitRemainsTwoMegabytes() {
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];
        oversized[0] = (byte) 0xFF;
        oversized[1] = (byte) 0xD8;
        oversized[2] = (byte) 0xFF;

        assertThatThrownBy(() -> storage.validate(file("image/jpeg", oversized)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 MB");
    }

    private MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("avatar", "avatar", contentType, content);
    }
}
