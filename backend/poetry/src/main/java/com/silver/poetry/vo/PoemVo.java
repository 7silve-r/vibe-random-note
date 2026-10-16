package com.silver.poetry.vo;

import com.silver.poetry.entity.Poem;
import java.util.List;

public record PoemVo(
        Integer id,
        String title,
        String author,
        Integer authorId,
        String dynasty,
        String kind,
        String dataset,
        String rhythmic,
        List<String> paragraphs,
        String excerpt,
        String authorDescription,
        boolean collected,
        String sourceRef,
        String sourceVersion) {
    public static PoemVo of(Poem poem, boolean collected, String description, boolean full) {
        return new PoemVo(
                poem.getId(),
                poem.getTitle(),
                poem.getAuthorName(),
                poem.getAuthorId(),
                poem.getDynasty(),
                poem.getKind(),
                poem.getDataset(),
                poem.getRhythmic(),
                full
                        ? poem.getContent().lines().toList()
                        : poem.getContent().lines().limit(2).toList(),
                poem.getExcerpt(),
                description,
                collected,
                poem.getSourceRef(),
                poem.getSourceVersion());
    }
}
