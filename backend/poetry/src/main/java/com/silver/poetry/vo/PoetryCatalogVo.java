package com.silver.poetry.vo;

import java.util.List;

public record PoetryCatalogVo(
        List<Collection> collections, List<String> dynasties, List<String> kinds) {
    public record Collection(String code, String name, long total) {}
}
