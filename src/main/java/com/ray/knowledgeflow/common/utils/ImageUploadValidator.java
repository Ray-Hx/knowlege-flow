package com.ray.knowledgeflow.common.utils;

import com.ray.knowledgeflow.common.exception.BusinessException;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

/** 基于文件实际内容而非客户端 MIME/后缀校验上传图片。 */
public final class ImageUploadValidator {

    private static final Tika TIKA = new Tika();
    private static final Map<String, String> SAFE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif");
    private static final int MAX_DIMENSION = 12_000;
    private static final long MAX_PIXELS = 40_000_000L;

    private ImageUploadValidator() {
    }

    public static ImageMetadata validate(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传图片不能为空");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(400, "图片文件过大");
        }

        final String detectedType;
        try (InputStream input = file.getInputStream()) {
            detectedType = TIKA.detect(input, file.getOriginalFilename());
        } catch (Exception e) {
            throw new BusinessException(400, "无法识别图片格式");
        }
        String extension = SAFE_EXTENSIONS.get(detectedType);
        if (extension == null) {
            throw new BusinessException(400, "仅支持 JPEG、PNG、GIF 图片");
        }

        validateDimensions(file);
        return new ImageMetadata(detectedType, extension);
    }

    private static void validateDimensions(MultipartFile file) {
        try (InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new BusinessException(400, "图片内容无效");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BusinessException(400, "图片内容无效");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new BusinessException(400, "图片尺寸超出限制");
                }
            } finally {
                reader.dispose();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "图片内容无效");
        }
    }

    public record ImageMetadata(String contentType, String extension) {
    }
}
