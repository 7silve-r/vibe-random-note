package com.silver.poetry.controller;

import com.silver.common.*;
import com.silver.poetry.service.PoetryService;
import com.silver.poetry.vo.PoemVo;
import com.silver.poetry.vo.PoetryCatalogVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PoetryController {
    @Autowired private PoetryService poetry;

    @GetMapping("/public/poetry")
    public Result<PageResult<PoemVo>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String dataset,
            @RequestParam(required = false) Integer authorId) {
        return Result.success(
                poetry.list(pageNum, pageSize, keyword, dynasty, kind, dataset, authorId, false));
    }

    @GetMapping("/public/poetry/catalog")
    public Result<PoetryCatalogVo> catalog() {
        return Result.success(poetry.catalog());
    }

    @GetMapping("/public/poetry/{id}")
    public Result<PoemVo> detail(@PathVariable Integer id) {
        return Result.success(poetry.detail(id));
    }

    @GetMapping("/public/poetry/random")
    public Result<PoemVo> random(
            @RequestParam(required = false) Integer exclude,
            @RequestParam(required = false) String dataset) {
        return Result.success(poetry.random(exclude, dataset));
    }

    @GetMapping("/public/poetry/daily")
    public Result<PoemVo> daily() {
        return Result.success(poetry.daily());
    }

    @GetMapping("/my/poetry/favorites")
    public Result<PageResult<PoemVo>> favorites(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String dataset,
            @RequestParam(required = false) Integer authorId) {
        return Result.success(
                poetry.list(pageNum, pageSize, keyword, dynasty, kind, dataset, authorId, true));
    }

    @PutMapping("/my/poetry/{id}/favorite")
    public Result<Void> favorite(@PathVariable Integer id) {
        poetry.favorite(id, false);
        return Result.success();
    }

    @DeleteMapping("/my/poetry/{id}/favorite")
    public Result<Void> cancelFavorite(@PathVariable Integer id) {
        poetry.favorite(id, true);
        return Result.success();
    }
}
