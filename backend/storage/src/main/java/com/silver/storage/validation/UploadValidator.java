package com.silver.storage.validation;

import com.silver.storage.exception.FileStorageException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

public final class UploadValidator {
    private UploadValidator() {}

    public record Content(byte[] bytes, String type, String extension) {}

    public static Content validate(MultipartFile file, boolean audio) throws IOException {
        int limit = (audio ? 50 : 5) * 1024 * 1024;
        if (file == null || file.isEmpty()) throw new FileStorageException("请选择非空文件");
        if (file.getSize() > limit)
            throw new FileStorageException(413, audio ? "音频不能超过50MB" : "图片不能超过5MB");
        byte[] bytes;
        try (var stream = file.getInputStream()) {
            bytes = stream.readNBytes(limit + 1);
        }
        if (bytes.length > limit) throw new FileStorageException(413, "文件超过大小限制");
        if (bytes.length == 0) throw new FileStorageException("请选择非空文件");
        if (audio) return audio(bytes);
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new FileStorageException(415, "图片仅支持有效的 JPEG、PNG");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png"))
                    throw new FileStorageException(415, "图片仅支持 JPEG、PNG");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096)
                    throw new FileStorageException(400, "图片宽高不能超过4096像素");
                var image = reader.read(0);
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(image, format, output))
                    throw new FileStorageException(415, "无法编码图片");
                return new Content(
                        output.toByteArray(),
                        format.equals("png") ? "image/png" : "image/jpeg",
                        format.equals("png") ? ".png" : ".jpg");
            } finally {
                reader.dispose();
            }
        } catch (javax.imageio.IIOException ex) {
            throw new FileStorageException(415, "图片已损坏，请重新选择");
        }
    }

    private static String text(byte[] bytes, int offset, int size) {
        return bytes.length >= offset + size
                ? new String(bytes, offset, size, StandardCharsets.US_ASCII)
                : "";
    }

    private static Content audio(byte[] b) {
        if (b.length >= 44 && text(b, 0, 4).equals("RIFF") && text(b, 8, 4).equals("WAVE"))
            return new Content(b, "audio/wav", ".wav");
        if (b.length >= 10
                && (text(b, 0, 3).equals("ID3") || ((b[0] & 255) == 255 && (b[1] & 224) == 224)))
            return new Content(b, "audio/mpeg", ".mp3");
        if (b.length >= 27 && text(b, 0, 4).equals("OggS"))
            return new Content(b, "audio/ogg", ".ogg");
        if (b.length >= 42 && text(b, 0, 4).equals("fLaC"))
            return new Content(b, "audio/flac", ".flac");
        if (b.length >= 24
                && text(b, 4, 4).equals("ftyp")
                && Set.of("M4A ", "M4B ", "isom", "mp42").contains(text(b, 8, 4)))
            return new Content(b, "audio/mp4", ".m4a");
        throw new FileStorageException(415, "音频仅支持 MP3、WAV、OGG、FLAC、M4A");
    }
}
