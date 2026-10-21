package com.ray.knowledgeflow.common.utils;

import com.ray.knowledgeflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadValidatorTest {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Test
    void shouldDetectActualContentInsteadOfClientMimeOrExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.html", "text/html", ONE_PIXEL_PNG);

        var metadata = ImageUploadValidator.validate(file, 1024);

        assertEquals("image/png", metadata.contentType());
        assertEquals(".png", metadata.extension());
    }

    @Test
    void shouldRejectSpoofedImageMime() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThrows(BusinessException.class,
                () -> ImageUploadValidator.validate(file, 1024));
    }

    @Test
    void shouldRejectOversizedFileBeforeParsing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", ONE_PIXEL_PNG);

        assertThrows(BusinessException.class,
                () -> ImageUploadValidator.validate(file, 10));
    }
}
