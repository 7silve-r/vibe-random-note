package com.silver.poetry.config;

import com.silver.common.exception.BusinessException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PoetryDatasets {
    private final List<Definition> definitions;

    public PoetryDatasets(JsonMapper mapper) throws IOException {
        try (var input = new ClassPathResource("poetry-datasets.json").getInputStream()) {
            definitions = List.copyOf(Arrays.asList(mapper.readValue(input, Definition[].class)));
        }
    }

    public List<Definition> all() {
        return definitions;
    }

    public String validate(String dataset) {
        if (dataset == null || dataset.isBlank()) return null;
        String code = dataset.strip();
        if (definitions.stream().noneMatch(item -> item.code().equals(code)))
            throw new BusinessException(400, "不支持的诗词文集");
        return code;
    }

    public record Definition(
            String code,
            String name,
            String folder,
            String pattern,
            String authors,
            String dynasty,
            String kind,
            String contentField,
            String defaultAuthor,
            List<String> titleFields) {}
}
