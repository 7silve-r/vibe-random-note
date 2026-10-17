package com.silver.storage.validation;

import com.silver.storage.exception.FileStorageException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

public final class LyricFile {
    private LyricFile() {}

    public static String read(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileStorageException("请选择非空歌词文件");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).matches(".*\\.(lrc|txt)$"))
            throw new FileStorageException(415, "歌词仅支持 UTF-8 编码的 LRC、TXT 文件");
        if (file.getSize() > 60000) throw new FileStorageException(413, "歌词不能超过60KB");
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(60001);
            if (bytes.length > 60000) throw new FileStorageException(413, "歌词不能超过60KB");
            String text =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .decode(ByteBuffer.wrap(bytes))
                            .toString()
                            .replace("\uFEFF", "")
                            .replace("\r\n", "\n")
                            .replace('\r', '\n')
                            .strip();
            if (text.isBlank()
                    || text.chars()
                            .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))
                throw new FileStorageException(415, "歌词内容为空或包含无效字符");
            return text;
        } catch (CharacterCodingException ex) {
            throw new FileStorageException(415, "请将歌词保存为 UTF-8 编码后上传");
        } catch (IOException ex) {
            throw new FileStorageException("歌词读取失败", ex);
        }
    }
}
