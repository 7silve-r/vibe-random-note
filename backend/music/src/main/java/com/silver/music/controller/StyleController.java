package com.silver.music.controller;

import com.silver.common.Result;
import com.silver.common.exception.BusinessException;
import com.silver.music.entity.Style;
import com.silver.music.service.StyleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class StyleController {
    @Autowired private StyleService styleService;

    @GetMapping("/music/public/styles")
    public Result<List<Style>> list() {
        return Result.success(styleService.list());
    }

    @PostMapping("/music/admin/styles")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> add(@RequestParam String name) {
        if (name.isBlank() || name.length() > 50) throw new BusinessException("风格名称须为1到50字");
        Style style = new Style();
        style.setName(name);
        if (!styleService.save(style)) throw new BusinessException("风格保存失败");
        return Result.success();
    }
}
