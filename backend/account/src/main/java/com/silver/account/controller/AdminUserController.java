package com.silver.account.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.silver.account.dto.*;
import com.silver.account.entity.User;
import com.silver.account.service.UserDeletionGuard;
import com.silver.account.service.UserService;
import com.silver.common.*;
import com.silver.common.exception.BusinessException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    @Autowired private UserService userService;
    private final List<UserDeletionGuard> deletionGuards;
    @Autowired private UserController userController;

    public AdminUserController(List<UserDeletionGuard> deletionGuards) {
        this.deletionGuards = deletionGuards;
    }

    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username) {
        var query =
                new LambdaQueryWrapper<User>()
                        .like(username != null && !username.isBlank(), User::getUsername, username)
                        .orderByDesc(User::getId);
        var page =
                userService.page(
                        new Page<>(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize))),
                        query);
        page.getRecords().forEach(user -> user.setPassword(null));
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    public Result<Void> add(@RequestBody RegisterDto dto) {
        return userController.register(dto);
    }

    private User user(Integer id) {
        User user = userService.getById(id);
        if (user == null) throw new BusinessException(404, "账号不存在");
        if ("ADMIN".equals(user.getRole())) throw new BusinessException(403, "不能管理固定管理员账号");
        return user;
    }

    @PatchMapping("/{id}/profile")
    public Result<Void> update(@PathVariable Integer id, @RequestBody ProfileDto dto) {
        User user = user(id);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setEmailVerified(false);
        if (!userService.updateById(user)) throw new BusinessException("账号更新失败");
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> status(@PathVariable Integer id, @RequestParam Integer status) {
        user(id);
        if (status != 0 && status != 1) throw new BusinessException("状态不合法");
        if (!userService
                .lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getStatus, status)
                .setSql("token_version = token_version + 1")
                .update()) throw new BusinessException("状态更新失败");
        return Result.success();
    }

    @Transactional
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        user(id);
        userService.lambdaQuery().eq(User::getId, id).last("FOR UPDATE").one();
        deletionGuards.forEach(guard -> guard.check(id));
        if (!userService.removeById(id)) throw new BusinessException("账号删除失败");
        return Result.success();
    }
}
